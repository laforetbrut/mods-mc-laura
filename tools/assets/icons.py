"""The 16x16 icons of Laura's menu (textures/gui/kawaii_icons.png).

Shapes are drawn at 4x, reduced to 16x16 with a hard alpha edge, then outlined like stickers.
Small details (eyes, mouths, glints) are placed pixel by pixel afterwards.

Author: vyrriox
"""
import math

from PIL import Image, ImageDraw

S = 4  # drawing scale
N = 16 * S

PLUM = (91, 42, 77, 255)
PLUM_SOFT = (156, 107, 138, 255)
PINK = (255, 182, 213, 255)
PINK_DEEP = (247, 127, 178, 255)
ROSE = (224, 85, 143, 255)
RED = (235, 72, 96, 255)
CREAM = (255, 247, 251, 255)
WHITE = (255, 255, 255, 255)
LAVENDER = (217, 200, 255, 255)
LAVENDER_DEEP = (179, 157, 255, 255)
MINT = (196, 242, 226, 255)
MINT_DEEP = (126, 224, 195, 255)
PEACH = (255, 214, 184, 255)
PEACH_DEEP = (255, 179, 138, 255)
SKY = (205, 232, 255, 255)
SKY_DEEP = (143, 211, 255, 255)
LEMON = (255, 236, 140, 255)
GOLD = (247, 196, 72, 255)
BROWN = (160, 104, 70, 255)
BROWN_DARK = (112, 70, 46, 255)
SILVER = (214, 222, 235, 255)
GREEN = (140, 214, 120, 255)
FACE = (255, 226, 170, 255)
TEAR = (120, 200, 255, 255)
BLUSH = (255, 150, 170, 255)
SHADOW = (0, 0, 0, 0)


class Icon:
    def __init__(self):
        self.big = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.big)
        self.small = None

    # ---- shapes, in 16 pixel coordinates (floats allowed)
    def ellipse(self, x0, y0, x1, y1, color):
        self.d.ellipse([x0 * S, y0 * S, x1 * S - 1, y1 * S - 1], fill=color)
        return self

    def circle(self, cx, cy, r, color):
        return self.ellipse(cx - r, cy - r, cx + r, cy + r, color)

    def rect(self, x0, y0, x1, y1, color, radius=0):
        if radius:
            self.d.rounded_rectangle([x0 * S, y0 * S, x1 * S - 1, y1 * S - 1], radius=radius * S, fill=color)
        else:
            self.d.rectangle([x0 * S, y0 * S, x1 * S - 1, y1 * S - 1], fill=color)
        return self

    def poly(self, points, color):
        self.d.polygon([(x * S, y * S) for x, y in points], fill=color)
        return self

    def line(self, points, color, width=1.5):
        self.d.line([(x * S, y * S) for x, y in points], fill=color, width=int(width * S), joint="curve")
        for x, y in (points[0], points[-1]):
            r = width * S / 2
            self.d.ellipse([x * S - r, y * S - r, x * S + r, y * S + r], fill=color)
        return self

    def arc(self, cx, cy, r, start, end, color, width=1.5):
        self.d.arc([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], start, end, fill=color, width=int(width * S))
        return self

    def heart(self, cx, cy, size, color):
        # Two circles and a triangle; size is the full width.
        r = size / 4
        self.circle(cx - r, cy - r * 0.4, r * 1.05, color)
        self.circle(cx + r, cy - r * 0.4, r * 1.05, color)
        self.poly([(cx - size / 2, cy - r * 0.1), (cx + size / 2, cy - r * 0.1), (cx, cy + size * 0.55)], color)
        return self

    def star(self, cx, cy, r, color, points=5, inner=0.45, rotation=-90):
        pts = []
        for i in range(points * 2):
            rr = r if i % 2 == 0 else r * inner
            a = math.radians(rotation + i * 180 / points)
            pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
        return self.poly(pts, color)

    # ---- finishing
    def finish(self, outline=PLUM):
        premultiplied = self.big.convert("RGBa").resize((16, 16), Image.BOX).convert("RGBA")
        px = premultiplied.load()
        for y in range(16):
            for x in range(16):
                r, g, b, a = px[x, y]
                px[x, y] = (r, g, b, 255) if a >= 100 else (0, 0, 0, 0)
        if outline:
            solid = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
            for y in range(16):
                for x in range(16):
                    if (x, y) in solid:
                        continue
                    if any((x + dx, y + dy) in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                        px[x, y] = outline
        self.small = premultiplied
        return self

    def px(self, x, y, color):
        if 0 <= x < 16 and 0 <= y < 16:
            self.small.load()[x, y] = color
        return self

    def pixels(self, color, *coords):
        for x, y in coords:
            self.px(x, y, color)
        return self

    def image(self):
        if self.small is None:
            self.finish()
        return self.small


# ------------------------------------------------------------------ faces

def face(base=FACE, tint=None):
    i = Icon().circle(8, 8.5, 6.6, base)
    if tint:
        i.circle(8, 8.5, 6.6, tint)
    return i.finish()


def eyes_dots(i, y=7, color=PLUM):
    return i.pixels(color, (5, y), (5, y - 1), (10, y), (10, y - 1))


def eyes_happy(i, y=7):
    # ^ ^
    return i.pixels(PLUM, (4, y), (5, y - 1), (6, y), (9, y), (10, y - 1), (11, y))


def eyes_closed(i, y=7):
    return i.pixels(PLUM, (4, y), (5, y), (6, y), (9, y), (10, y), (11, y))


def blush(i, y=9):
    return i.pixels(BLUSH, (3, y), (4, y), (11, y), (12, y))


def smile(i, y=10):
    return i.pixels(PLUM, (6, y), (7, y + 1), (8, y + 1), (9, y))


def frown(i, y=11):
    return i.pixels(PLUM, (6, y), (7, y - 1), (8, y - 1), (9, y))


def flat(i, y=11):
    return i.pixels(PLUM, (6, y), (7, y), (8, y), (9, y))


def open_mouth(i, y=10, color=ROSE):
    return i.pixels(PLUM, (7, y), (8, y), (6, y + 1), (9, y + 1), (7, y + 2), (8, y + 2)).pixels(color, (7, y + 1), (8, y + 1))


def mood_happy():
    return smile(blush(eyes_happy(face())))


def mood_in_love():
    i = face()
    i.pixels(RED, (4, 6), (6, 6), (4, 7), (5, 7), (6, 7), (5, 8), (9, 6), (11, 6), (9, 7), (10, 7), (11, 7), (10, 8))
    return smile(blush(i, 10), 10)


def mood_neutral():
    return flat(eyes_dots(face()))


def mood_bored():
    i = face()
    i.pixels(PLUM, (4, 7), (5, 7), (6, 7), (9, 7), (10, 7), (11, 7)).pixels(PLUM_SOFT, (4, 6), (5, 6), (6, 6), (9, 6), (10, 6), (11, 6))
    return flat(i, 11)


def mood_hungry():
    i = open_mouth(eyes_dots(face()), 9)
    return i.pixels(TEAR, (9, 12), (9, 13))


def mood_tired():
    i = flat(eyes_closed(face()), 11)
    return i.pixels(LAVENDER_DEEP, (12, 1), (13, 1), (14, 1), (13, 2), (12, 3), (13, 3), (14, 3))


def mood_sad():
    i = frown(eyes_dots(face()), 11)
    return i.pixels(TEAR, (5, 8), (5, 9), (4, 10))


def mood_angry():
    i = face(base=(255, 170, 150, 255))
    i.pixels(PLUM, (4, 5), (5, 6), (11, 5), (10, 6))
    eyes_dots(i, 8)
    return frown(i, 11).pixels(RED, (13, 3), (14, 2), (12, 2), (13, 1))


def mood_jealous():
    i = face(base=(200, 235, 170, 255))
    i.pixels(PLUM, (6, 7), (6, 6), (11, 7), (11, 6)).pixels(WHITE, (5, 7), (10, 7))
    return flat(i, 11).pixels(PLUM, (9, 10))


def mood_sulking():
    i = face()
    i.pixels(PLUM, (3, 7), (4, 7), (5, 7), (10, 7), (11, 7), (12, 7))
    i.pixels(PINK_DEEP, (3, 9), (4, 9), (11, 9), (12, 9), (4, 10), (11, 10))
    return i.pixels(PLUM, (7, 11), (8, 11), (7, 10))


# ------------------------------------------------------------------ tabs and stats

def tab_home():
    i = Icon().poly([(1.5, 8), (8, 1.5), (14.5, 8)], PINK_DEEP).rect(3.5, 7, 12.5, 14.5, CREAM).rect(6.5, 10, 9.5, 14.5, BROWN)
    return i.finish().pixels(RED, (10, 8), (11, 8), (10, 9))


def tab_orders():
    i = Icon().rect(1.5, 2, 14.5, 11.5, LAVENDER, 3).poly([(4, 11), (8, 11), (4, 14.5)], LAVENDER)
    return i.finish().pixels(PLUM, (8, 4), (8, 5), (8, 6), (8, 7), (8, 9))


def tab_emotes():
    return smile(blush(eyes_happy(face(base=LEMON))))


def tab_work():
    i = Icon().line([(3, 14), (12, 5)], BROWN, 2).poly([(9, 1.5), (14.5, 7), (12.5, 9), (7, 3.5)], SILVER)
    return i.finish()


def tab_fetch():
    i = Icon().arc(8, 8, 5, 180, 360, BROWN_DARK, 1.4).rect(2, 8, 14, 14.5, BROWN, 1).circle(8, 7.5, 2.8, RED)
    return i.finish().pixels(GREEN, (8, 4), (9, 4)).pixels(PEACH, (4, 10), (6, 12), (10, 10), (12, 12))


def tab_style():
    i = Icon().poly([(5, 1.5), (11, 1.5), (10, 6), (14.5, 14.5), (1.5, 14.5), (6, 6)], PINK)
    return i.finish().pixels(PINK_DEEP, (6, 6), (7, 6), (8, 6), (9, 6)).pixels(WHITE, (7, 9), (9, 11), (6, 12))


def tab_settings():
    i = Icon().star(8, 8, 7, LAVENDER_DEEP, points=8, inner=0.72, rotation=0).circle(8, 8, 4.6, LAVENDER_DEEP)
    return i.finish().pixels(CREAM, (7, 7), (8, 7), (7, 8), (8, 8), (7, 6), (8, 6), (6, 7), (6, 8), (9, 7), (9, 8), (7, 9), (8, 9))


def tab_info():
    i = Icon().circle(8, 8, 6.6, SKY_DEEP)
    return i.finish().pixels(WHITE, (7, 4), (8, 4), (7, 6), (8, 6), (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10), (6, 11), (7, 11), (8, 11), (9, 11))


def need_hunger():
    i = Icon().ellipse(1.5, 1.5, 11, 11, (196, 120, 76, 255)).line([(9, 9), (13, 13)], WHITE, 2).circle(13.6, 12.2, 1.4, WHITE).circle(12.2, 13.6, 1.4, WHITE)
    return i.finish().pixels((226, 160, 110, 255), (4, 4), (5, 4), (4, 5))


def need_energy():
    return Icon().poly([(9.5, 1), (3, 9), (7.5, 9), (6, 15), (13, 6.5), (8.5, 6.5), (11, 1)], GOLD).finish()


def need_fun():
    i = Icon().ellipse(3, 1, 13, 12, PINK_DEEP).poly([(7, 11.5), (9, 11.5), (8, 13)], PINK_DEEP)
    return i.finish().pixels(PLUM_SOFT, (8, 14), (7, 15)).pixels(WHITE, (5, 3), (5, 4), (6, 3))


def need_attention():
    i = Icon().rect(1.5, 2, 14.5, 12, CREAM, 3).poly([(9, 11.5), (13, 11.5), (12, 15)], CREAM)
    i.finish()
    return i.pixels(PINK_DEEP, (5, 5), (6, 5), (9, 5), (10, 5), (4, 6), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6), (11, 6),
                    (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9))


def need_hygiene():
    i = Icon().circle(8, 10, 5, SKY_DEEP).poly([(3.4, 9.2), (12.6, 9.2), (8, 1)], SKY_DEEP)
    return i.finish().pixels(WHITE, (6, 8), (6, 9), (5, 10))


def health():
    return Icon().heart(8, 8.5, 13, RED).finish().pixels(WHITE, (4, 5), (4, 6), (5, 5))


def affection():
    i = Icon().heart(8, 8.5, 13, PINK_DEEP).finish().pixels(WHITE, (4, 5), (4, 6), (5, 5))
    return i.pixels(LEMON, (13, 1), (13, 2), (12, 2), (14, 2), (13, 3))


def desire():
    i = Icon().circle(6, 8, 4, WHITE).circle(10.5, 7, 4.2, WHITE).circle(8, 5, 3.6, WHITE).circle(3.5, 13.5, 1.4, WHITE)
    return i.finish(outline=LAVENDER_DEEP).pixels(PINK_DEEP, (7, 7), (9, 7), (7, 8), (8, 8), (9, 8), (8, 9))


def days():
    i = Icon().rect(2, 3, 14, 14.5, WHITE, 1).rect(2, 3, 14, 6, ROSE, 1)
    i.finish()
    return i.pixels(PLUM, (5, 2), (11, 2)).pixels(PINK_DEEP, (6, 9), (7, 8), (8, 9), (9, 8), (10, 9), (7, 10), (8, 10), (9, 10), (8, 11), (6, 8)).pixels(PINK_DEEP, (10, 8))


# ------------------------------------------------------------------ interface

def close():
    return Icon().line([(4, 4), (12, 12)], ROSE, 2.4).line([(12, 4), (4, 12)], ROSE, 2.4).finish()


def prev():
    return Icon().poly([(11, 2.5), (11, 13.5), (3.5, 8)], PINK_DEEP).finish()


def next_():
    return Icon().poly([(5, 2.5), (5, 13.5), (12.5, 8)], PINK_DEEP).finish()


def pencil():
    i = Icon().poly([(3, 13), (3.4, 10), (11, 2.4), (13.6, 5), (6, 12.6)], LEMON).poly([(11, 2.4), (13.6, 5), (14.6, 4), (12, 1.4)], PINK_DEEP)
    return i.finish().pixels(PLUM, (3, 13), (4, 12))


def check():
    return Icon().line([(3, 8.5), (6.5, 12), (13, 4)], MINT_DEEP, 2.6).finish()


def plus():
    return Icon().rect(6.5, 2.5, 9.5, 13.5, MINT_DEEP, 1).rect(2.5, 6.5, 13.5, 9.5, MINT_DEEP, 1).finish()


def minus():
    return Icon().rect(2.5, 6.5, 13.5, 9.5, PEACH_DEEP, 1).finish()


def trash():
    i = Icon().rect(3.5, 5, 12.5, 14.5, LAVENDER, 1).rect(2, 3, 14, 5, LAVENDER_DEEP).rect(6, 1.5, 10, 3, LAVENDER_DEEP)
    return i.finish().pixels(PLUM_SOFT, (6, 7), (6, 8), (6, 9), (6, 10), (6, 11), (9, 7), (9, 8), (9, 9), (9, 10), (9, 11))


def upload():
    i = Icon().poly([(8, 1.5), (13, 7), (10, 7), (10, 11), (6, 11), (6, 7), (3, 7)], MINT_DEEP).rect(2, 12.5, 14, 14.5, LAVENDER_DEEP)
    return i.finish()


def refresh():
    i = Icon().arc(8, 8, 5, 40, 320, SKY_DEEP, 2.2).poly([(11, 1.5), (14.5, 5.5), (10, 6.5)], SKY_DEEP)
    return i.finish()


def sparkle():
    return Icon().star(8, 8, 7, LEMON, points=4, inner=0.28, rotation=-90).finish().pixels(WHITE, (8, 7), (8, 8))


def heart_full():
    return Icon().heart(8, 8.5, 13, PINK_DEEP).finish().pixels(WHITE, (4, 5), (4, 6), (5, 5))


def heart_half():
    i = Icon().heart(8, 8.5, 13, (240, 225, 232, 255))
    i.d.rectangle([0, 0, 8 * S - 1, N], fill=None)
    left = Icon().heart(8, 8.5, 13, PINK_DEEP)
    left.d.rectangle([8 * S, 0, N, N], fill=(0, 0, 0, 0))
    i.big.alpha_composite(left.big)
    return i.finish()


def heart_empty():
    return Icon().heart(8, 8.5, 13, (240, 225, 232, 255)).finish(outline=PLUM_SOFT)


def star():
    return Icon().star(8, 8.6, 7.2, GOLD).finish().pixels(WHITE, (7, 5), (7, 6))


def lock():
    i = Icon().arc(8, 6.5, 3.6, 180, 360, SILVER, 1.8).line([(4.4, 6.5), (4.4, 8)], SILVER, 1.8).line([(11.6, 6.5), (11.6, 8)], SILVER, 1.8)
    i.rect(2.5, 7.5, 13.5, 14.5, GOLD, 1)
    return i.finish().pixels(BROWN_DARK, (8, 10), (8, 11))


# ------------------------------------------------------------------ orders

def follow():
    i = Icon()
    for cx, cy in ((4.8, 10.5), (11.2, 5.5)):
        i.ellipse(cx - 2.2, cy - 1.6, cx + 2.2, cy + 3.6, PINK_DEEP)
        for dx in (-1.8, 0, 1.8):
            i.circle(cx + dx, cy - 3, 0.9, PINK_DEEP)
    return i.finish()


def stay():
    i = Icon().circle(8, 6.5, 5.2, ROSE).poly([(3.6, 8), (12.4, 8), (8, 15)], ROSE)
    return i.finish().pixels(CREAM, (7, 5), (8, 5), (7, 6), (8, 6), (6, 6), (9, 6), (7, 7), (8, 7), (7, 4), (8, 4))


def wander():
    i = Icon().line([(2, 13), (5, 9), (9, 11), (11, 6), (14, 3)], MINT_DEEP, 1.8)
    return i.finish().pixels(PINK_DEEP, (14, 2), (13, 2), (14, 3))


def come():
    i = Icon().arc(8, 7, 5, 0, 180, RED, 3.2).rect(1.4, 3, 4.6, 7.4, RED).rect(11.4, 3, 14.6, 7.4, RED)
    i.rect(1.4, 1.5, 4.6, 3.6, SILVER).rect(11.4, 1.5, 14.6, 3.6, SILVER)
    return i.finish()


def go_home():
    i = Icon().poly([(1.5, 8), (8, 1.5), (14.5, 8)], LAVENDER_DEEP).rect(3.5, 7, 12.5, 14.5, CREAM)
    return i.finish().pixels(PINK_DEEP, (6, 10), (7, 10), (8, 10), (9, 10), (8, 9), (8, 11), (7, 9), (7, 11), (9, 9), (9, 11), (10, 10))


def set_home():
    i = Icon().poly([(1.5, 8), (8, 1.5), (14.5, 8)], MINT_DEEP).rect(3.5, 7, 12.5, 14.5, CREAM)
    return i.finish().pixels(MINT_DEEP, (8, 9), (8, 10), (8, 11), (8, 12), (7, 10), (6, 10), (9, 10), (10, 10))


def sleep():
    i = Icon().circle(7, 8.5, 6, LEMON)
    i.d.ellipse([(7.5 - 5) * S, (6 - 5) * S, (7.5 + 5.5) * S, (6 + 5.5) * S], fill=(0, 0, 0, 0))
    i.finish()
    return i.pixels(LAVENDER_DEEP, (11, 2), (12, 2), (13, 2), (12, 3), (11, 4), (12, 4), (13, 4))


def wake():
    i = Icon().circle(8, 8, 3.6, GOLD)
    for k in range(8):
        a = math.radians(k * 45)
        i.line([(8 + math.cos(a) * 5.2, 8 + math.sin(a) * 5.2), (8 + math.cos(a) * 6.8, 8 + math.sin(a) * 6.8)], GOLD, 1.3)
    return i.finish()


def stop():
    i = Icon()
    pts = [(8 + math.cos(math.radians(22.5 + k * 45)) * 7, 8 + math.sin(math.radians(22.5 + k * 45)) * 7) for k in range(8)]
    i.poly(pts, RED)
    return i.finish().pixels(WHITE, (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7), (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8))


def hug():
    i = Icon().heart(8, 8.5, 10, PINK_DEEP).arc(8, 8.5, 6.2, 20, 160, PEACH, 2).arc(8, 8.5, 6.2, 200, 340, PEACH, 2)
    return i.finish()


def lips(i=None, cx=8, cy=8.5, scale=1.0):
    i = i or Icon()
    w = 6 * scale
    i.ellipse(cx - w, cy - 3 * scale, cx, cy + 1.5 * scale, ROSE).ellipse(cx, cy - 3 * scale, cx + w, cy + 1.5 * scale, ROSE)
    i.ellipse(cx - w * 0.9, cy - 1 * scale, cx + w * 0.9, cy + 4 * scale, ROSE)
    return i


def kiss():
    return lips().finish().pixels(PLUM, (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8)).pixels(PINK, (5, 10), (6, 10))


def compliment():
    i = Icon().star(8, 8.6, 7.2, PINK)
    return i.finish().pixels(RED, (7, 7), (9, 7), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (7, 9), (8, 9), (9, 9), (8, 10))


def inventory():
    i = Icon().rect(2.5, 4, 13.5, 15, PINK_DEEP, 3).arc(8, 4.5, 3, 180, 360, PLUM_SOFT, 1.4).rect(4.5, 9, 11.5, 13, PINK, 1)
    return i.finish().pixels(PLUM, (7, 9), (8, 9))


def eat():
    i = Icon().ellipse(1.5, 6, 14.5, 14.5, CREAM).rect(1.5, 6, 14.5, 9, (0, 0, 0, 0))
    i.ellipse(1.5, 6.5, 14.5, 14.5, WHITE)
    i.big.paste((0, 0, 0, 0), (0, 0, N, int(9.5 * S)))
    i.ellipse(2, 5, 14, 10.5, (255, 190, 120, 255))
    return i.finish().pixels(RED, (5, 6), (10, 7)).pixels(GREEN, (8, 6), (7, 7))


def ungag():
    i = Icon().circle(4.5, 11.5, 2.6, PINK_DEEP).circle(11.5, 11.5, 2.6, PINK_DEEP)
    i.line([(5.5, 9.5), (11, 2)], SILVER, 1.6).line([(10.5, 9.5), (5, 2)], SILVER, 1.6)
    return i.finish().pixels(CREAM, (4, 11), (11, 11))


def queue():
    i = Icon()
    for y in (3, 7.5, 12):
        i.circle(3, y, 1.4, PINK_DEEP).rect(6, y - 1, 14.5, y + 1, LAVENDER_DEEP, 0.8)
    return i.finish()


# ------------------------------------------------------------------ emotes

def hand(i, cx, cy, color=FACE, scale=1.0):
    s = scale
    i.rect(cx - 3 * s, cy - 1 * s, cx + 3 * s, cy + 4.5 * s, color, 1.5 * s)
    for k, dx in enumerate((-2.6, -0.9, 0.8, 2.5)):
        top = cy - (4.2 if k in (1, 2) else 3.5) * s
        i.rect(cx + (dx - 0.8) * s, top, cx + (dx + 0.8) * s, cy + 0.5 * s, color, 0.8 * s)
    i.rect(cx - 4.8 * s, cy + 0.2 * s, cx - 2.2 * s, cy + 1.8 * s, color, 0.8 * s)
    return i


def note(i, x, y, color=LAVENDER_DEEP):
    i.circle(x, y, 1.7, color).rect(x + 0.9, y - 6, x + 2, y, color).rect(x + 0.9, y - 6, x + 4, y - 4.6, color)
    return i


def backpack():
    i = Icon().rect(3, 4, 13, 15, BROWN, 3).arc(8, 4.5, 3, 180, 360, BROWN_DARK, 1.4).rect(4.5, 9.5, 11.5, 13.5, (196, 140, 96, 255), 1)
    return i.finish().pixels(GOLD, (7, 9), (8, 9)).pixels(PINK_DEEP, (4, 6), (11, 6))


def emote_wave():
    i = hand(Icon(), 8.5, 8.5)
    i.finish()
    return i.pixels(PINK_DEEP, (1, 3), (2, 2), (1, 5), (14, 3), (15, 5))


def emote_clap():
    i = hand(Icon(), 5.5, 9.5, scale=0.8)
    hand(i, 10.5, 9.5, scale=0.8)
    i.finish()
    return i.pixels(LEMON, (8, 1), (8, 2), (7, 2), (9, 2), (8, 3), (3, 2), (13, 2))


def emote_dance():
    i = note(Icon(), 4.5, 12.5, PINK_DEEP)
    note(i, 10, 9.5, LAVENDER_DEEP)
    return i.finish()


def emote_laugh():
    i = face()
    i.pixels(PLUM, (4, 5), (5, 6), (4, 7), (11, 5), (10, 6), (11, 7))
    i.pixels(PLUM, (5, 9), (6, 9), (7, 9), (8, 9), (9, 9), (10, 9), (5, 10), (10, 10), (6, 11), (7, 11), (8, 11), (9, 11))
    return i.pixels(ROSE, (6, 10), (7, 10), (8, 10), (9, 10)).pixels(TEAR, (3, 8), (12, 8))


def emote_cry():
    i = frown(eyes_closed(face()), 11)
    return i.pixels(TEAR, (4, 8), (4, 9), (4, 10), (4, 11), (11, 8), (11, 9), (11, 10), (11, 11))


def emote_blush():
    i = smile(eyes_happy(face()), 10)
    return i.pixels(BLUSH, (2, 9), (3, 9), (4, 9), (3, 10), (11, 9), (12, 9), (13, 9), (12, 10)).pixels(RED, (3, 8), (12, 8))


def emote_facepalm():
    i = face()
    hand(i, 7, 6.5, (255, 205, 150, 255), 0.75)
    i.finish()
    return frown(i, 12)


def emote_jump():
    i = Icon().poly([(8, 1.5), (14, 8), (10.5, 8), (10.5, 12), (5.5, 12), (5.5, 8), (2, 8)], MINT_DEEP)
    i.finish()
    return i.pixels(PLUM_SOFT, (4, 14), (5, 14), (6, 14), (9, 14), (10, 14), (11, 14))


def emote_bow():
    i = Icon().poly([(8, 8), (1.5, 3.5), (1.5, 12.5)], PINK_DEEP).poly([(8, 8), (14.5, 3.5), (14.5, 12.5)], PINK_DEEP).circle(8, 8, 2.2, ROSE)
    return i.finish().pixels(WHITE, (3, 6), (13, 6))


def emote_think():
    i = eyes_dots(face(), 6)
    i.pixels(PLUM, (7, 11), (8, 11), (9, 11))
    return i.pixels(LAVENDER_DEEP, (12, 0), (13, 0), (14, 1), (13, 2), (13, 4))


def emote_shrug():
    i = face()
    hand(i, 2.8, 5, (255, 205, 150, 255), 0.55)
    hand(i, 13.2, 5, (255, 205, 150, 255), 0.55)
    i.finish()
    eyes_closed(i, 8)
    return i.pixels(PLUM, (6, 11), (7, 11), (8, 12), (9, 12))


def emote_stomp():
    i = Icon().rect(4, 3, 9, 11, PINK_DEEP, 1).rect(4, 9, 13.5, 13, PINK_DEEP, 1.5).rect(4, 12.5, 14, 14, PLUM_SOFT)
    i.finish()
    return i.pixels(PEACH_DEEP, (1, 12), (1, 10), (15, 10), (15, 8))


def emote_yawn():
    i = eyes_closed(face(), 6)
    i.pixels(PLUM, (7, 9), (8, 9), (6, 10), (9, 10), (6, 11), (9, 11), (7, 12), (8, 12)).pixels(ROSE, (7, 10), (8, 10), (7, 11), (8, 11))
    return i.pixels(LAVENDER_DEEP, (12, 1), (13, 1), (13, 2), (12, 3), (13, 3))


def emote_celebrate():
    i = Icon().poly([(2, 14.5), (5, 5), (11.5, 11.5)], GOLD)
    i.finish()
    return i.pixels(PINK_DEEP, (9, 2), (12, 4), (14, 1)).pixels(MINT_DEEP, (11, 1), (14, 6)).pixels(LAVENDER_DEEP, (7, 3), (13, 8)).pixels(RED, (10, 5))


def emote_snap():
    i = Icon().star(8, 8, 7.2, LEMON, points=6, inner=0.35)
    return i.finish().pixels(WHITE, (8, 7), (8, 8), (7, 8))


def emote_twirl():
    i = Icon().arc(8, 8, 6, 0, 300, PINK_DEEP, 1.6).arc(8, 8, 3, 90, 390, LAVENDER_DEEP, 1.6)
    return i.finish()


def emote_hum():
    return note(Icon(), 6.5, 12, PINK_DEEP).finish()


def emote_stretch():
    i = Icon().circle(8, 5.5, 2.6, FACE).rect(6, 8, 10, 14.5, PINK, 1)
    i.line([(6.5, 9), (3, 2)], FACE, 1.4).line([(9.5, 9), (13, 2)], FACE, 1.4)
    return i.finish()


def emote_tap_foot():
    i = Icon().rect(3, 7, 13.5, 12, WHITE, 2).rect(3, 11, 14, 13, PINK_DEEP, 1)
    i.finish()
    return i.pixels(PLUM_SOFT, (1, 4), (2, 5), (14, 3), (13, 4)).pixels(PINK_DEEP, (6, 8), (8, 8))


def emote_blow_kiss():
    i = lips(Icon(), 6, 10, 0.75).heart(12, 4.5, 6, PINK_DEEP)
    return i.finish()


def emote_sneeze():
    i = eyes_closed(face(), 6)
    i.pixels(PLUM, (6, 10), (7, 10), (8, 10))
    return i.pixels(TEAR, (11, 10), (13, 9), (14, 11), (12, 12), (14, 13))


def emote_hair_flip():
    i = Icon().arc(6, 9, 5, 200, 30, BROWN, 2.4).arc(10, 9, 4, 180, 20, (140, 85, 55, 255), 2.0)
    i.finish()
    return i.pixels(LEMON, (13, 2), (13, 3), (12, 3), (14, 3), (13, 4))


def emote_check_nails():
    i = hand(Icon(), 8, 9)
    i.finish()
    return i.pixels(PINK_DEEP, (5, 5), (7, 4), (9, 4), (11, 5))


def emote_air_guitar():
    i = Icon().ellipse(1.5, 7, 9, 14.5, RED).circle(8, 8.5, 2.6, RED).line([(7, 9), (14, 2)], BROWN, 1.4)
    return i.finish().pixels(PLUM, (5, 10), (5, 11), (6, 10), (6, 11))


def emote_hiccup():
    i = eyes_dots(face())
    i.pixels(PLUM, (7, 11), (8, 11))
    return i.pixels(SKY_DEEP, (12, 2), (13, 1), (14, 2), (13, 3))


def emote_pout():
    i = face()
    i.pixels(PLUM, (4, 7), (5, 6), (6, 7), (9, 7), (10, 6), (11, 7))
    return i.pixels(ROSE, (7, 10), (8, 10), (7, 11), (8, 11)).pixels(PINK_DEEP, (3, 9), (12, 9))


def emote_shiver():
    i = face(base=(205, 225, 255, 255))
    eyes_dots(i)
    return i.pixels(PLUM, (6, 11), (7, 10), (8, 11), (9, 10)).pixels(WHITE, (1, 2), (14, 3), (2, 13))


def emote_fan():
    i = Icon().poly([(8, 14), (1.5, 5), (5, 2.5), (8, 2), (11, 2.5), (14.5, 5)], PINK)
    i.finish()
    return i.pixels(PINK_DEEP, (5, 5), (6, 7), (7, 9), (8, 4), (8, 7), (10, 5), (9, 8)).pixels(BROWN, (8, 13), (8, 14))


def emote_hug():
    return hug()


def emote_kiss():
    return kiss()


def combat_passive():
    i = Icon()
    for k in range(5):
        a = math.radians(-90 + k * 72)
        i.circle(8 + math.cos(a) * 3.8, 8 + math.sin(a) * 3.8, 3, PINK)
    i.circle(8, 8, 2.4, LEMON)
    return i.finish()


def combat_fight():
    i = Icon().poly([(12.5, 1.5), (14.5, 3.5), (6, 12), (4, 10)], SILVER).line([(2.5, 9), (7, 13.5)], BROWN_DARK, 1.6)
    return i.finish().pixels(BROWN, (2, 14), (3, 13))


ICONS = {
    "TAB_HOME": tab_home, "TAB_ORDERS": tab_orders, "TAB_EMOTES": tab_emotes, "TAB_WORK": tab_work,
    "TAB_FETCH": tab_fetch, "BACKPACK": backpack, "TAB_STYLE": tab_style, "TAB_SETTINGS": tab_settings, "TAB_INFO": tab_info,
    "NEED_HUNGER": need_hunger, "NEED_ENERGY": need_energy, "NEED_FUN": need_fun, "NEED_ATTENTION": need_attention,
    "NEED_HYGIENE": need_hygiene, "HEALTH": health, "AFFECTION": affection, "DESIRE": desire, "DAYS": days,
    "MOOD_HAPPY": mood_happy, "MOOD_IN_LOVE": mood_in_love, "MOOD_NEUTRAL": mood_neutral, "MOOD_BORED": mood_bored,
    "MOOD_HUNGRY": mood_hungry, "MOOD_TIRED": mood_tired, "MOOD_SAD": mood_sad, "MOOD_ANGRY": mood_angry,
    "MOOD_JEALOUS": mood_jealous, "MOOD_SULKING": mood_sulking,
    "CLOSE": close, "PREV": prev, "NEXT": next_, "PENCIL": pencil, "CHECK": check, "PLUS": plus, "MINUS": minus,
    "TRASH": trash, "UPLOAD": upload, "REFRESH": refresh, "SPARKLE": sparkle, "HEART_FULL": heart_full,
    "HEART_HALF": heart_half, "HEART_EMPTY": heart_empty, "STAR": star, "LOCK": lock,
    "FOLLOW": follow, "STAY": stay, "WANDER": wander, "COME": come, "GO_HOME": go_home, "SET_HOME": set_home,
    "SLEEP": sleep, "WAKE": wake, "STOP": stop, "HUG": hug, "KISS": kiss, "COMPLIMENT": compliment,
    "INVENTORY": inventory, "EAT": eat, "UNGAG": ungag, "QUEUE": queue,
    "EMOTE_WAVE": emote_wave, "EMOTE_CLAP": emote_clap, "EMOTE_DANCE": emote_dance, "EMOTE_LAUGH": emote_laugh,
    "EMOTE_CRY": emote_cry, "EMOTE_BLUSH": emote_blush, "EMOTE_FACEPALM": emote_facepalm, "EMOTE_JUMP": emote_jump,
    "EMOTE_BOW": emote_bow, "EMOTE_THINK": emote_think, "EMOTE_SHRUG": emote_shrug, "EMOTE_STOMP": emote_stomp,
    "EMOTE_YAWN": emote_yawn, "EMOTE_CELEBRATE": emote_celebrate, "EMOTE_SNAP": emote_snap, "EMOTE_TWIRL": emote_twirl,
    "EMOTE_HUM": emote_hum, "EMOTE_STRETCH": emote_stretch, "EMOTE_TAP_FOOT": emote_tap_foot,
    "EMOTE_BLOW_KISS": emote_blow_kiss, "EMOTE_SNEEZE": emote_sneeze, "EMOTE_HAIR_FLIP": emote_hair_flip,
    "EMOTE_CHECK_NAILS": emote_check_nails, "EMOTE_AIR_GUITAR": emote_air_guitar, "EMOTE_HICCUP": emote_hiccup,
    "EMOTE_POUT": emote_pout, "EMOTE_SHIVER": emote_shiver, "EMOTE_FAN": emote_fan, "EMOTE_HUG": emote_hug,
    "EMOTE_KISS": emote_kiss, "COMBAT_PASSIVE": combat_passive, "COMBAT_FIGHT": combat_fight,
}


def atlas(order):
    """Builds the 256x256 atlas; {@code order} is the list of enum constant names."""
    out = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    missing = []
    for index, name in enumerate(order):
        fn = ICONS.get(name)
        if fn is None:
            if not name.startswith("SPARE"):
                missing.append(name)
            continue
        img = fn()
        if isinstance(img, Icon):
            img = img.image()
        out.alpha_composite(img, ((index % 16) * 16, (index // 16) * 16))
    return out, missing
