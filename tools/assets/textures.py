"""Small textures: the hay gag, the thought bubble, Laura's Heart, the gravestone and the logo.

Author: vyrriox
"""
import random

from PIL import Image, ImageDraw

from icons import Icon, PINK_DEEP, ROSE, RED, WHITE, LEMON, PLUM
from skins import faces, HEAD, HAT

STRAW = [(233, 196, 92, 255), (216, 172, 70, 255), (245, 214, 120, 255), (196, 150, 58, 255)]


def straw(rng):
    return rng.choice(STRAW)


def hay_gag():
    """32x16, laid out for the tuft and the three straws of the hay model (see LauraLayers.Gag)."""
    rng = random.Random(7)
    img = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    px = img.load()
    for x0, y0, x1, y1 in ((0, 0, 12, 3), (0, 4, 6, 6), (8, 4, 14, 6), (16, 4, 20, 7)):
        for x in range(x0, x1):
            for y in range(y0, y1):
                px[x, y] = straw(rng)
    # A few darker strands on the tuft.
    for x in range(1, 12, 3):
        px[x, 1] = (170, 120, 45, 255)
    return img


def thought_bubble():
    size = 32
    big = Image.new("RGBA", (size * 4, size * 4), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    fill = (255, 255, 255, 255)
    for cx, cy, r in ((52, 46, 30), (84, 44, 28), (68, 30, 26), (40, 62, 20), (92, 64, 20), (66, 66, 24)):
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill)
    d.ellipse([22, 94, 38, 110], fill=fill)
    d.ellipse([10, 112, 20, 122], fill=fill)
    small = big.convert("RGBa").resize((size, size), Image.BOX).convert("RGBA")
    px = small.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = (255, 255, 255, 235) if px[x, y][3] >= 100 else (0, 0, 0, 0)
    solid = {(x, y) for y in range(size) for x in range(size) if px[x, y][3]}
    for y in range(size):
        for x in range(size):
            if (x, y) not in solid and any((x + dx, y + dy) in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = (179, 157, 255, 255)
    # A soft lavender shade at the bottom of the cloud.
    for (x, y) in solid:
        if (x, y + 1) not in solid and y < 26:
            px[x, y] = (236, 228, 255, 240)
    return small


def laura_heart():
    i = Icon().heart(8, 8.5, 13, PINK_DEEP)
    i.finish(outline=(150, 40, 90, 255))
    i.pixels(WHITE, (4, 5), (4, 6), (5, 5))
    i.pixels((255, 160, 200, 255), (6, 5), (5, 7), (6, 6))
    i.pixels(ROSE, (10, 10), (9, 11), (8, 12), (11, 9))
    i.pixels(LEMON, (13, 1), (13, 3), (12, 2), (14, 2))
    return i.image()


def stone(seed, engraved=False):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    px = img.load()
    base = [(186, 184, 196), (174, 172, 186), (196, 194, 205), (165, 162, 178)]
    for y in range(16):
        for x in range(16):
            c = rng.choice(base)
            if rng.random() < 0.06:
                c = (150, 146, 164)
            px[x, y] = c + (255,)
    if engraved:
        heart = ("..XX.XX..",
                 ".XXXXXXX.",
                 ".XXXXXXX.",
                 "..XXXXX..",
                 "...XXX...",
                 "....X....")
        for dy, row in enumerate(heart):
            for dx, ch in enumerate(row):
                if ch == "X":
                    px[4 + dx, 6 + dy] = (122, 104, 132, 255)
        for dx in range(9):
            if heart[0][dx] == "X":
                px[4 + dx, 5] = (210, 208, 220, 255)
        for x in range(5, 12):
            px[x, 3] = (140, 132, 152, 255)
    return img


def grave_petals():
    """Little pink petals lying on the base."""
    rng = random.Random(11)
    img = stone(12)
    px = img.load()
    for _ in range(9):
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = rng.choice([(255, 182, 213, 255), (247, 127, 178, 255), (255, 214, 230, 255)])
    return img


# A tiny 5x7 pixel font for the logo title.
FONT = {
    "M": ["X...X", "XX.XX", "X.X.X", "X.X.X", "X...X", "X...X", "X...X"],
    "Y": ["X...X", "X...X", ".X.X.", "..X..", "..X..", "..X..", "..X.."],
    "G": [".XXX.", "X...X", "X....", "X.XXX", "X...X", "X...X", ".XXX."],
    "I": ["XXX", ".X.", ".X.", ".X.", ".X.", ".X.", "XXX"],
    "R": ["XXXX.", "X...X", "X...X", "XXXX.", "X.X..", "X..X.", "X...X"],
    "L": ["X....", "X....", "X....", "X....", "X....", "X....", "XXXXX"],
    "F": ["XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "X...."],
    "E": ["XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "XXXXX"],
    "N": ["X...X", "XX..X", "X.X.X", "X..XX", "X...X", "X...X", "X...X"],
    "D": ["XXXX.", "X...X", "X...X", "X...X", "X...X", "X...X", "XXXX."],
    "A": [".XXX.", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X"],
    "U": ["X...X", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX."],
    " ": ["..", "..", "..", "..", "..", "..", ".."],
}


def pixel_text(draw, text, x, y, scale, color, shadow=None):
    cx = x
    for ch in text:
        glyph = FONT[ch]
        for gy, row in enumerate(glyph):
            for gx, c in enumerate(row):
                if c == "X":
                    if shadow:
                        draw.rectangle([cx + gx * scale + scale // 2, y + gy * scale + scale // 2,
                                        cx + (gx + 1) * scale - 1 + scale // 2, y + (gy + 1) * scale - 1 + scale // 2], fill=shadow)
                    draw.rectangle([cx + gx * scale, y + gy * scale, cx + (gx + 1) * scale - 1, y + (gy + 1) * scale - 1], fill=color)
        cx += (len(glyph[0]) + 1) * scale
    return cx


def text_width(text, scale):
    return sum((len(FONT[ch][0]) + 1) * scale for ch in text) - scale


def logo(skin):
    w, h = 512, 256
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Rounded pastel card with a vertical gradient.
    for y in range(h):
        t = y / (h - 1)
        c = (int(255 - 8 * t), int(228 - 40 * t), int(241 - 20 * t), 255)
        d.line([(0, y), (w, y)], fill=c)
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w - 1, h - 1], radius=36, fill=255)
    img.putalpha(mask)
    d = ImageDraw.Draw(img)
    rng = random.Random(3)
    for _ in range(26):
        x, y, s = rng.randrange(w), rng.randrange(h), rng.choice((2, 3, 4))
        heart = Icon().heart(8, 8.5, 13, (255, 255, 255, 255)).finish(outline=None).image()
        heart = heart.resize((16 * s // 2, 16 * s // 2), Image.NEAREST)
        heart.putalpha(heart.getchannel("A").point(lambda a: int(a * 0.5)))
        img.alpha_composite(heart, (x, y))
    # Laura's face from her skin, big and pixelated, on a heart.
    big_heart = Icon().heart(8, 8.5, 13, PINK_DEEP).finish(outline=(150, 40, 90, 255)).image().resize((176, 176), Image.NEAREST)
    img.alpha_composite(big_heart, (26, 40))
    face_rect = faces(HEAD)["front"]
    hat_rect = faces(HAT)["front"]
    face = skin.crop((face_rect[0], face_rect[1], face_rect[0] + 8, face_rect[1] + 8))
    hat = skin.crop((hat_rect[0], hat_rect[1], hat_rect[0] + 8, hat_rect[1] + 8))
    face = Image.alpha_composite(face, hat).resize((88, 88), Image.NEAREST)
    frame = Image.new("RGBA", (96, 96), PLUM)
    img.alpha_composite(frame, (66, 72))
    img.alpha_composite(face, (70, 76))
    # Title, centered in the space right of the heart.
    for line, scale, y, color in (("MY GIRLFRIEND", 3, 70, (91, 42, 77, 255)), ("LAURA", 9, 104, (224, 85, 143, 255))):
        tx = 214 + (284 - text_width(line, scale)) // 2
        pixel_text(d, line, tx, y, scale, color, shadow=(255, 255, 255, 220))
    return img
