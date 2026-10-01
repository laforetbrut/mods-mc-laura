"""Laura's built-in skins, painted pixel by pixel (64x64, slim arms).

Author: vyrriox
"""
import random

from PIL import Image

# Box layouts of a 64x64 skin: (u, v, width, height, depth) of each part.
HEAD = (0, 0, 8, 8, 8)
HAT = (32, 0, 8, 8, 8)
BODY = (16, 16, 8, 12, 4)
JACKET = (16, 32, 8, 12, 4)
RIGHT_ARM = (40, 16, 3, 12, 4)
RIGHT_SLEEVE = (40, 32, 3, 12, 4)
LEFT_ARM = (32, 48, 3, 12, 4)
LEFT_SLEEVE = (48, 48, 3, 12, 4)
RIGHT_LEG = (0, 16, 4, 12, 4)
RIGHT_PANTS = (0, 32, 4, 12, 4)
LEFT_LEG = (16, 48, 4, 12, 4)
LEFT_PANTS = (0, 48, 4, 12, 4)


def faces(box):
    """Rectangles (x, y, w, h) of the six faces of a box, vanilla cube UV layout."""
    u, v, w, h, d = box
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


def hexc(value, alpha=255):
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def shade(color, factor):
    r, g, b, a = color
    return (max(0, min(255, int(r * factor))), max(0, min(255, int(g * factor))), max(0, min(255, int(b * factor))), a)


class Painter:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rng = random.Random(seed)

    def put(self, x, y, color):
        if 0 <= x < 64 and 0 <= y < 64 and color is not None:
            self.px[x, y] = color

    def rect(self, rect, color, noise=0.0, vertical_shade=0.0):
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                c = color
                if vertical_shade:
                    c = shade(c, 1.0 - vertical_shade * (y / max(1, h - 1)))
                if noise:
                    c = shade(c, 1.0 + self.rng.uniform(-noise, noise))
                self.put(x0 + x, y0 + y, c)

    def box(self, box, color, noise=0.04, vertical_shade=0.12, skip=()):
        for name, rect in faces(box).items():
            if name in skip:
                continue
            factor = {"top": 1.08, "bottom": 0.8, "right": 0.9, "left": 0.9, "back": 0.94}.get(name, 1.0)
            self.rect(rect, shade(color, factor), noise, vertical_shade if name not in ("top", "bottom") else 0.0)

    def face_rows(self, box, face, rows_from, rows_to, color, noise=0.04):
        x0, y0, w, h = faces(box)[face]
        for y in range(rows_from, rows_to):
            for x in range(w):
                c = shade(color, 1.0 + self.rng.uniform(-noise, noise))
                self.put(x0 + x, y0 + y, c)

    def all_sides_rows(self, box, rows_from, rows_to, color, noise=0.04):
        for face in ("front", "back", "right", "left"):
            self.face_rows(box, face, rows_from, rows_to, color, noise)

    def pixel(self, box, face, x, y, color):
        x0, y0, w, h = faces(box)[face]
        if 0 <= x < w and 0 <= y < h:
            self.put(x0 + x, y0 + y, color)


class Style:
    def __init__(self, **kw):
        self.skin = hexc("f6d3bd")
        self.skin_shadow = hexc("e8b9a0")
        self.hair = hexc("6b3e26")
        self.hair_light = hexc("8a5537")
        self.eyes = hexc("3f8fd6")
        self.lips = hexc("e0707e")
        self.blush = hexc("f4a3ad")
        self.lashes = hexc("2b1a14")
        self.top = hexc("ffb6d5")
        self.top_dark = hexc("f38fbb")
        self.bottom = hexc("4a6fa5")
        self.legs = None  # skin by default
        self.shoes = hexc("ffffff")
        self.shoe_sole = hexc("d9c7d1")
        self.accessory = hexc("ff6fa8")
        self.ponytail = False
        self.long_hair = True
        self.dress = False
        self.sleeves = "long"
        self.__dict__.update(kw)


def paint_head(p, s):
    # Skin everywhere, then hair.
    p.box(HEAD, s.skin, noise=0.02, vertical_shade=0.05)
    front = faces(HEAD)["front"]
    fx, fy = front[0], front[1]
    hair = s.hair
    # Top and back of the head: hair.
    p.rect(faces(HEAD)["top"], hair, noise=0.07)
    p.rect(faces(HEAD)["back"], shade(hair, 0.95), noise=0.07)
    # Sides: hair with a small ear area.
    for side in ("right", "left"):
        x0, y0, w, h = faces(HEAD)[side]
        for y in range(8):
            for x in range(8):
                ear = y in (4, 5) and ((side == "right" and x == 5) or (side == "left" and x == 2))
                if not ear:
                    p.put(x0 + x, y0 + y, shade(hair, 0.92 + p.rng.uniform(-0.06, 0.06)))
    # Front: bangs and face.
    for x in range(8):
        p.put(fx + x, fy + 0, shade(hair, 1.0 + p.rng.uniform(-0.05, 0.05)))
        p.put(fx + x, fy + 1, shade(hair, 1.02 + p.rng.uniform(-0.05, 0.05)))
    for x in (0, 1, 2, 5, 7):
        p.put(fx + x, fy + 2, shade(hair, 1.0 + p.rng.uniform(-0.05, 0.05)))
    p.put(fx + 3, fy + 1, s.hair_light)
    p.put(fx + 6, fy + 1, s.hair_light)
    for y in range(3, 8):
        p.put(fx + 0, fy + y, shade(hair, 0.95))
        p.put(fx + 7, fy + y, shade(hair, 0.95))
    # Eyes with lashes: outer white, inner iris.
    p.put(fx + 1, fy + 3, s.lashes)
    p.put(fx + 2, fy + 3, s.lashes)
    p.put(fx + 5, fy + 3, s.lashes)
    p.put(fx + 6, fy + 3, s.lashes)
    p.put(fx + 1, fy + 4, hexc("ffffff"))
    p.put(fx + 2, fy + 4, s.eyes)
    p.put(fx + 5, fy + 4, s.eyes)
    p.put(fx + 6, fy + 4, hexc("ffffff"))
    # Blush, nose shadow and mouth.
    p.put(fx + 1, fy + 5, s.blush)
    p.put(fx + 6, fy + 5, s.blush)
    p.put(fx + 3, fy + 5, s.skin_shadow)
    p.put(fx + 3, fy + 6, s.lips)
    p.put(fx + 4, fy + 6, s.lips)
    # Hat layer: hair volume on the sides and the back.
    hx, hy = faces(HAT)["back"][0], faces(HAT)["back"][1]
    for y in range(8):
        for x in range(8):
            if p.rng.random() < 0.55:
                p.put(hx + x, hy + y, shade(hair, 0.9 + p.rng.uniform(-0.06, 0.08)))
    for side in ("right", "left"):
        x0, y0, w, h = faces(HAT)[side]
        for y in range(2, 8):
            for x in range(8):
                edge = (side == "right" and x >= 5) or (side == "left" and x <= 2)
                if edge and p.rng.random() < 0.7:
                    p.put(x0 + x, y0 + y, shade(hair, 0.93 + p.rng.uniform(-0.05, 0.05)))
    tx, ty = faces(HAT)["top"][0], faces(HAT)["top"][1]
    for x in range(8):
        if p.rng.random() < 0.4:
            p.put(tx + x, ty + p.rng.randint(0, 7), shade(s.hair_light, 1.0))


def paint_long_hair(p, s):
    """Hair falling on the back (body back and jacket back)."""
    bx, by, bw, bh = faces(BODY)["back"]
    length = 9 if s.long_hair else 4
    for y in range(length):
        for x in range(bw):
            if y < length - 1 or x in (1, 2, 5, 6):
                p.put(bx + x, by + y, shade(s.hair, 0.93 + p.rng.uniform(-0.07, 0.07)))
    jx, jy, jw, jh = faces(JACKET)["back"]
    for y in range(length + 1):
        for x in range(jw):
            if (y < length or x % 2 == 0) and p.rng.random() < 0.85:
                p.put(jx + x, jy + y, shade(s.hair, 0.9 + p.rng.uniform(-0.07, 0.07)))
    # Strands over the shoulders, on the sides of the body.
    for side in ("right", "left"):
        x0, y0, w, h = faces(JACKET)[side]
        for y in range(min(length, 7)):
            p.put(x0 + (3 if side == "right" else 0), y0 + y, shade(s.hair, 0.9))
    if s.ponytail:
        # A ponytail tied high: shorter hair, a bump on the hat back.
        hx, hy = faces(HAT)["back"][0], faces(HAT)["back"][1]
        for y in range(1, 5):
            for x in range(3, 5):
                p.put(hx + x, hy + y, shade(s.hair, 1.05))
        p.put(hx + 3, hy + 1, s.accessory)
        p.put(hx + 4, hy + 1, s.accessory)


def paint_body(p, s):
    p.box(BODY, s.top, noise=0.04)
    # Neck: a skin V on the front.
    fx, fy = faces(BODY)["front"][0], faces(BODY)["front"][1]
    p.put(fx + 3, fy, s.skin)
    p.put(fx + 4, fy, s.skin)
    # Waist line or dress.
    if not s.dress:
        p.all_sides_rows(BODY, 10, 12, s.bottom, noise=0.05)
        p.face_rows(BODY, "bottom", 0, 4, s.bottom)
        p.pixel(BODY, "front", 3, 10, shade(s.bottom, 1.25))


def paint_arms(p, s):
    for arm, sleeve in ((RIGHT_ARM, RIGHT_SLEEVE), (LEFT_ARM, LEFT_SLEEVE)):
        p.box(arm, s.skin, noise=0.02, vertical_shade=0.06)
        rows = {"long": 10, "short": 4, "none": 0}[s.sleeves]
        if rows:
            p.all_sides_rows(arm, 0, rows, s.top, noise=0.04)
            p.face_rows(arm, "top", 0, 4, s.top)
            if s.sleeves == "long":
                p.all_sides_rows(arm, rows - 1, rows, s.top_dark, noise=0.02)
        else:
            p.face_rows(arm, "top", 0, 4, s.skin)


def paint_legs(p, s):
    for leg in (RIGHT_LEG, LEFT_LEG):
        p.box(leg, s.legs or s.skin, noise=0.03, vertical_shade=0.05)
        if s.dress:
            p.all_sides_rows(leg, 0, 4, s.bottom, noise=0.04)
        else:
            p.all_sides_rows(leg, 0, 3, s.bottom, noise=0.05)
        p.face_rows(leg, "top", 0, 4, s.bottom)
        p.all_sides_rows(leg, 10, 12, s.shoes, noise=0.03)
        p.face_rows(leg, "bottom", 0, 4, s.shoe_sole)
        p.pixel(leg, "front", 1, 10, shade(s.shoes, 0.92))


def heart_on(p, box, face, x, y, color):
    shape = (".X.X.",
             "XXXXX",
             ".XXX.",
             "..X..")
    for dy, row in enumerate(shape):
        for dx, ch in enumerate(row):
            if ch == "X":
                p.pixel(box, face, x + dx, y + dy, color)


def laura(seed=1):
    s = Style()
    p = Painter(seed)
    paint_head(p, s)
    paint_body(p, s)
    paint_arms(p, s)
    paint_legs(p, s)
    paint_long_hair(p, s)
    heart_on(p, BODY, "front", 1, 3, hexc("ffffff"))
    # A pink bow in her hair.
    bx, by = faces(HAT)["left"][0], faces(HAT)["left"][1]
    for x, y in ((2, 1), (3, 1), (4, 1), (3, 2), (2, 2), (4, 2)):
        p.put(bx + x, by + y, s.accessory)
    return p.img


def laura_summer(seed=2):
    s = Style(top=hexc("fff2a8"), top_dark=hexc("f6d86b"), bottom=hexc("fff2a8"), dress=True, sleeves="none",
              shoes=hexc("c98b5a"), shoe_sole=hexc("8c5a34"), accessory=hexc("ff8fb1"), hair_light=hexc("a8683f"))
    p = Painter(seed)
    paint_head(p, s)
    paint_body(p, s)
    paint_arms(p, s)
    paint_legs(p, s)
    paint_long_hair(p, s)
    # Little white flowers on the dress.
    for x, y in ((1, 3), (5, 6), (2, 9), (6, 2)):
        p.pixel(BODY, "front", x, y, hexc("ffffff"))
    for x, y in ((1, 2), (1, 1)):
        p.pixel(LEFT_LEG, "front", x, y, hexc("ffffff"))
    # A flower above the ear.
    fx, fy = faces(HAT)["right"][0], faces(HAT)["right"][1]
    for x, y in ((4, 2), (5, 1), (6, 2), (5, 3)):
        p.put(fx + x, fy + y, hexc("ff8fb1"))
    p.put(fx + 5, fy + 2, hexc("ffe066"))
    return p.img


def laura_winter(seed=3):
    s = Style(top=hexc("d9404f"), top_dark=hexc("b8303f"), bottom=hexc("3b3f5c"), legs=hexc("3b3f5c"),
              shoes=hexc("7a4a2e"), shoe_sole=hexc("4f2f1d"), accessory=hexc("ffffff"))
    p = Painter(seed)
    paint_head(p, s)
    paint_body(p, s)
    paint_arms(p, s)
    paint_legs(p, s)
    paint_long_hair(p, s)
    # Fur trims and buttons.
    p.all_sides_rows(BODY, 9, 10, hexc("f4f1ee"), noise=0.03)
    for arm in (RIGHT_ARM, LEFT_ARM):
        p.all_sides_rows(arm, 9, 10, hexc("f4f1ee"), noise=0.03)
    for y in (3, 6):
        p.pixel(BODY, "front", 4, y, hexc("f2c14e"))
    # Striped scarf on the jacket layer.
    for face in ("front", "back", "right", "left"):
        x0, y0, w, h = faces(JACKET)[face]
        for x in range(w):
            p.put(x0 + x, y0, hexc("ffffff") if x % 2 == 0 else hexc("e8505f"))
            p.put(x0 + x, y0 + 1, hexc("e8505f") if x % 2 == 0 else hexc("ffffff"))
    for y in range(2, 6):
        p.pixel(JACKET, "front", 5, y, hexc("e8505f") if y % 2 == 0 else hexc("ffffff"))
    # Beanie with a pompom on the hat layer.
    for face in ("front", "back", "right", "left"):
        x0, y0, w, h = faces(HAT)[face]
        for y in range(0, 3):
            for x in range(w):
                p.put(x0 + x, y0 + y, hexc("d9404f") if y < 2 else hexc("f4f1ee"))
    p.rect(faces(HAT)["top"], hexc("d9404f"), noise=0.04)
    tx, ty = faces(HAT)["top"][0], faces(HAT)["top"][1]
    for x, y in ((3, 3), (4, 3), (3, 4), (4, 4)):
        p.put(tx + x, ty + y, hexc("ffffff"))
    return p.img


def laura_night(seed=4):
    s = Style(top=hexc("cdb8ff"), top_dark=hexc("a98ef0"), bottom=hexc("cdb8ff"), legs=hexc("cdb8ff"),
              shoes=hexc("ffc2dc"), shoe_sole=hexc("f09ac0"), accessory=hexc("9a7bff"))
    p = Painter(seed)
    paint_head(p, s)
    paint_body(p, s)
    paint_arms(p, s)
    paint_legs(p, s)
    paint_long_hair(p, s)
    # Stars on the pyjamas.
    for box, face, x, y in ((BODY, "front", 1, 2), (BODY, "front", 6, 5), (BODY, "front", 3, 8), (BODY, "back", 5, 10),
                            (RIGHT_LEG, "front", 1, 6), (LEFT_LEG, "front", 2, 3), (RIGHT_ARM, "front", 1, 4)):
        p.pixel(box, face, x, y, hexc("fff4a8"))
    # A sleep mask pushed up on the forehead (hat layer).
    x0, y0, w, h = faces(HAT)["front"]
    for x in range(8):
        p.put(x0 + x, y0 + 1, hexc("ff9ccb"))
        p.put(x0 + x, y0 + 2, hexc("ff9ccb") if x not in (3, 4) else hexc("ffe4f1"))
    return p.img


def laura_sporty(seed=5):
    s = Style(top=hexc("5fd3c4"), top_dark=hexc("3fb3a4"), bottom=hexc("2b2b3a"), legs=None, sleeves="none",
              shoes=hexc("ffffff"), shoe_sole=hexc("ff6fa8"), accessory=hexc("ff6fa8"), ponytail=True, long_hair=False)
    p = Painter(seed)
    paint_head(p, s)
    paint_body(p, s)
    paint_arms(p, s)
    paint_legs(p, s)
    paint_long_hair(p, s)
    # Headband and a stripe.
    for face in ("front", "back", "right", "left"):
        x0, y0, w, h = faces(HAT)[face]
        for x in range(w):
            p.put(x0 + x, y0 + 1, hexc("ff6fa8"))
    for y in range(12):
        p.pixel(BODY, "right", 1, y, hexc("ffffff") if y < 10 else s.bottom)
        p.pixel(BODY, "left", 2, y, hexc("ffffff") if y < 10 else s.bottom)
    for leg in (RIGHT_LEG, LEFT_LEG):
        p.all_sides_rows(leg, 3, 5, hexc("2b2b3a"), noise=0.03)
    return p.img


def laura_gothic(seed=6):
    s = Style(skin=hexc("f3e2dc"), skin_shadow=hexc("e0c8c0"), hair=hexc("1d1a24"), hair_light=hexc("7a4fb8"),
              eyes=hexc("9a4fd6"), lips=hexc("5a2146"), blush=hexc("e8c6d2"), top=hexc("2a2231"), top_dark=hexc("1a1520"),
              bottom=hexc("2a2231"), legs=hexc("1f1a26"), shoes=hexc("121016"), shoe_sole=hexc("7a4fb8"),
              accessory=hexc("7a4fb8"), dress=True)
    p = Painter(seed)
    paint_head(p, s)
    paint_body(p, s)
    paint_arms(p, s)
    paint_legs(p, s)
    paint_long_hair(p, s)
    # Purple lace, a choker and a streak of purple hair.
    p.all_sides_rows(BODY, 11, 12, hexc("7a4fb8"), noise=0.02)
    for leg in (RIGHT_LEG, LEFT_LEG):
        p.all_sides_rows(leg, 3, 4, hexc("7a4fb8"), noise=0.02)
    for face in ("front", "back", "right", "left"):
        x0, y0, w, h = faces(JACKET)[face]
        for x in range(w):
            p.put(x0 + x, y0, hexc("121016"))
    p.pixel(JACKET, "front", 3, 0, hexc("b98cff"))
    p.pixel(JACKET, "front", 4, 0, hexc("b98cff"))
    fx, fy = faces(HEAD)["front"][0], faces(HEAD)["front"][1]
    p.put(fx + 2, fy + 0, hexc("7a4fb8"))
    p.put(fx + 2, fy + 1, hexc("7a4fb8"))
    p.put(fx + 1, fy + 2, hexc("7a4fb8"))
    return p.img


SKINS = {
    "laura": laura,
    "laura_summer": laura_summer,
    "laura_winter": laura_winter,
    "laura_night": laura_night,
    "laura_sporty": laura_sporty,
    "laura_gothic": laura_gothic,
}


def preview(img, scale=8):
    """A front view (head, body, arms, legs) for checking a skin by eye."""
    out = Image.new("RGBA", (16 * scale, 32 * scale), (255, 240, 246, 255))

    def blit(rect, x, y, overlay=None):
        part = img.crop((rect[0], rect[1], rect[0] + rect[2], rect[1] + rect[3]))
        if overlay:
            over = img.crop((overlay[0], overlay[1], overlay[0] + overlay[2], overlay[1] + overlay[3]))
            part = Image.alpha_composite(part, over)
        part = part.resize((rect[2] * scale, rect[3] * scale), Image.NEAREST)
        out.alpha_composite(part, (x * scale, y * scale))

    blit(faces(HEAD)["front"], 4, 0, faces(HAT)["front"])
    blit(faces(BODY)["front"], 4, 8, faces(JACKET)["front"])
    blit(faces(RIGHT_ARM)["front"], 1, 8, faces(RIGHT_SLEEVE)["front"])
    blit(faces(LEFT_ARM)["front"], 12, 8, faces(LEFT_SLEEVE)["front"])
    blit(faces(RIGHT_LEG)["front"], 4, 20, faces(RIGHT_PANTS)["front"])
    blit(faces(LEFT_LEG)["front"], 8, 20, faces(LEFT_PANTS)["front"])
    return out
