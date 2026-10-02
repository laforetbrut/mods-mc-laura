"""Builds the media of the project page: the animated banners (media/*.gif), the section titles
and the feature icons.

Usage:  python tools/media/make_media.py [names...] [--src <captured frames>] [--out <folder>]

The footage is captured in game with the shader pack on (see tools/media/README.md); this script
frames it, adds the animated titles, hearts and labels in the style of the mod's menu, and writes
GIF files under 2 MB each. The section titles and the icons are drawn from scratch and need no
footage. Requires Pillow, numpy and ffmpeg.

Author: vyrriox
"""
import argparse
import math
import os
import sys

import numpy as np
from PIL import Image, ImageChops, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(os.path.dirname(HERE), "assets"))

import kit  # noqa: E402
import pixelfont as font  # noqa: E402
import skins  # noqa: E402
from kit import CREAM, LAVENDER_DEEP, LEMON, PINK, PINK_DEEP, PLUM, PLUM_SOFT, ROSE, WHITE  # noqa: E402

SRC = os.path.join(ROOT, "neoforge-1.21.1", "run-capture", "screenshots")
OUT = os.path.join(ROOT, "media")
SKIN = os.path.join(ROOT, "common", "1.21.1", "src", "main", "resources", "assets", "lauramod", "textures", "entity", "laura", "laura.png")

W = 800
OUTLINE = (150, 40, 90, 255)


def src(name):
    return os.path.join(SRC, name)


def clip(name, first, last, step=1):
    paths = [src(f"{name}_{i:04d}.png") for i in range(first, last + 1, step)]
    return [p for p in paths if os.path.exists(p)]


def footage(paths, crop, size):
    return [kit.load(p, size=size, crop=crop) for p in paths]


def emblem(scale):
    """The heart of the logo with Laura's face on it; the heart is 16 * scale pixels wide."""
    out = kit.heart(scale, PINK_DEEP, OUTLINE).copy()
    skin = Image.open(SKIN).convert("RGBA")
    fx, fy = skins.faces(skins.HEAD)["front"][:2]
    hx, hy = skins.faces(skins.HAT)["front"][:2]
    face = Image.alpha_composite(skin.crop((fx, fy, fx + 8, fy + 8)), skin.crop((hx, hy, hx + 8, hy + 8)))
    face = face.resize((8 * scale, 8 * scale), Image.NEAREST)
    b = max(2, int(round(scale * 0.36)))
    frame = Image.new("RGBA", (face.width + 2 * b, face.height + 2 * b), PLUM)
    frame.alpha_composite(face, (b, b))
    out.alpha_composite(frame, (8 * scale - frame.width // 2, int(round(7.27 * scale)) - frame.height // 2))
    return out


def letters_mask(img, color):
    """Mask of the pixels of a text image that have the letter color (not its shadow)."""
    mask = Image.new("L", img.size, 0)
    px = img.load()
    mp = mask.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a and (r, g, b) == tuple(color[:3]):
                mp[x, y] = 255
    return mask


def loop_tail(frames, base, count):
    """Eases the last frames into the first one, so the footage loops without a jump."""
    for j in range(count):
        t = (j + 1) / (count + 1)
        frames[-count + j] = Image.blend(frames[-count + j], base, t)
    return frames


def composite_region(frames, plate, boxes, feather=12):
    """Keeps the footage inside the boxes (one box or several) and the still plate everywhere else.
    A box may reach past the frame: the footage then stays whole up to that edge."""
    if isinstance(boxes[0], (int, float)):
        boxes = [boxes]
    mask = None
    for box in boxes:
        m = kit.feather_mask(plate.size, box, feather)
        mask = m if mask is None else ImageChops.lighter(mask, m)
    return [Image.composite(f, plate, mask) for f in frames]


def finish(frames, name, fps=kit.FPS, **kwargs):
    os.makedirs(OUT, exist_ok=True)
    preview = os.environ.get("MEDIA_PREVIEW")
    if preview:
        picks = [frames[k] for k in sorted({0, len(frames) // 4, len(frames) // 2, len(frames) * 3 // 4})]
        sheet = Image.new("RGB", (picks[0].width, sum(p.height for p in picks) + 4 * (len(picks) - 1)), (20, 20, 24))
        y = 0
        for p in picks:
            sheet.paste(p, (0, y))
            y += p.height + 4
        sheet.save(os.path.join(preview, os.path.splitext(name)[0] + "_preview.png"))
    path = os.path.join(OUT, name)
    size = kit.write_gif(frames, path, fps=fps, **kwargs)
    print(f"{name}: {len(frames)} frames, {frames[0].width}x{frames[0].height}, {size / 1e6:.2f} MB")
    return size


# ------------------------------------------------------------------ hero

def hero():
    h = 300
    crop = (0, 150, 1600, 750)
    frames = footage(clip("hero_wave", 5, 72), crop, (W, h))
    n = len(frames)
    plate = frames[0]
    seq = composite_region(frames, plate, (545, 28, 790, h + 30), feather=14)
    seq = loop_tail(seq, plate, 5)
    seq = kit.stabilize(seq, 7)

    veil = kit.gradient(470, h, (255, 240, 247, 205), (255, 240, 247, 0))
    em = emblem(7)
    title_small = kit.text_image("MY GIRLFRIEND", 3, PLUM, shadow=WHITE)
    title_big = kit.text_image("LAURA", 9, ROSE, shadow=WHITE)
    big_letters = letters_mask(title_big, ROSE)
    hearts = [kit.heart(1, PINK_DEEP, OUTLINE), kit.heart(2, PINK_DEEP, OUTLINE), kit.heart(1, WHITE, (235, 150, 190, 255)),
              kit.heart(2, (255, 205, 225, 255), (224, 85, 143, 255))]
    floaters = kit.Floaters(hearts, 9, (585, 70, 765, 250), n, seed=5, rise=70, sway=7)
    left = kit.Floaters([kit.heart(1, WHITE, (235, 150, 190, 255)), kit.heart(1, (255, 205, 225, 255), ROSE)], 6, (20, 60, 420, 280), n,
                        seed=11, rise=38, sway=5)
    sparkle = kit.icon("SPARKLE", 1)
    row_full = kit.icon("HEART_FULL", 2)
    row_empty = kit.icon("HEART_EMPTY", 2)

    out = []
    for i in range(n):
        t = i / n
        canvas = seq[i].convert("RGBA")
        canvas.alpha_composite(veil, (0, 0))
        left.draw(canvas, i, alpha=0.9)
        # The emblem beats twice per loop.
        beat = max(math.exp(-((t - 0.06) / 0.035) ** 2), 0.7 * math.exp(-((t - 0.20) / 0.035) ** 2))
        kit.paste(canvas, em, 88, 150, scale=1 + 0.10 * beat, anchor="c")
        canvas.alpha_composite(title_small, (158, 92 - font.ACCENT_ROWS * 3))
        # A glint runs over the big title once per loop.
        big = title_big.copy()
        gx = int(kit.lerp(-60, big.width + 60, kit.span(t, 0.30, 0.62)))
        glint = Image.new("L", big.size, 0)
        ImageDraw.Draw(glint).polygon([(gx, 0), (gx + 26, 0), (gx + 6, big.height), (gx - 20, big.height)], fill=150)
        white = Image.new("RGBA", big.size, (255, 255, 255, 255))
        white.putalpha(ImageChops.multiply(glint, big_letters))
        big.alpha_composite(white)
        canvas.alpha_composite(big, (158, 124 - font.ACCENT_ROWS * 9))
        # Affection hearts fill up one after the other, hold, then start again.
        for k in range(5):
            start = 0.10 + k * 0.11
            p = kit.span(t, start, start + 0.08)
            hold = 1 - kit.span(t, 0.94, 1.0)
            x = 162 + k * 38
            canvas.alpha_composite(row_empty, (x, 206))
            if p > 0:
                kit.paste(canvas, row_full, x + 16, 206 + 16, alpha=hold, scale=kit.ease_out_back(p, 2.6), anchor="c")
        floaters.draw(canvas, i)
        for k, (sx, sy, ph) in enumerate(((150, 78, 0.0), (432, 118, 0.33), (396, 212, 0.66), (60, 222, 0.5), (120, 250, 0.15))):
            kit.twinkle(canvas, sparkle, sx, sy, i, n / 2, ph)
        kit.frame_border(canvas)
        out.append(canvas.convert("RGB"))
    return finish(out, "hero.gif")


# ------------------------------------------------------------------ chat scenes

MEADOW = (155, 95, 1445, 675)       # source crop of the meadow view: her bubble, herself, the ground
MEADOW_SIZE = (W, 360)


def chat_pill(text, typed, caret, width):
    """The line the player types, as a chip of the menu: speech icon, text, caret."""
    h = 46
    out = kit.panel(width, h, radius=h // 2, fill=CREAM, border=PINK_DEEP, border_width=3, shadow_offset=4)
    out.alpha_composite(kit.icon("TAB_ORDERS", 2), (12, (h - 32) // 2))
    x = font.draw(out, text[:typed], 52, (h - 21) // 2, 3, PLUM)
    if caret:
        ImageDraw.Draw(out).rectangle([x + 4, 10, x + 6, h - 12], fill=ROSE)
    return out


def first_change(frames, box, threshold=40, share=0.012):
    """Index of the first frame that differs from the first one inside box (she appears, or starts to move)."""
    base = np.asarray(frames[0].crop(box), dtype=np.int16)
    for i, f in enumerate(frames):
        d = np.abs(np.asarray(f.crop(box), dtype=np.int16) - base).max(axis=2) > threshold
        if d.mean() > share:
            return i
    return 0


WATCH = (330, 40, 470, 340)         # where she stands in the meadow view


def chat_scene(name, prompt, clip_name, clip_last, body, bubble, lead=4, step=1, plate=None, hold=10):
    """The player types a line, she answers. body and bubble are the two areas kept from the footage."""
    w, h = MEADOW_SIZE
    frames = footage(clip(clip_name, 1, clip_last), MEADOW, MEADOW_SIZE)
    base = kit.load(src(plate), size=MEADOW_SIZE, crop=MEADOW) if plate else frames[0]
    start = max(0, first_change(frames, WATCH) - lead)
    action = composite_region(frames[start::step], base, [body, bubble], feather=16)
    action = loop_tail(action, base, 5)
    action = kit.stabilize([base] + action, 7)[1:]

    pill_w = 52 + font.width(prompt, 3) + 30
    px, py = 22, h - 46 - 22
    out = []
    # The pill slides in, the line is typed, then it is sent.
    slide, per_char, sent = 8, 2, 7
    typing = len(prompt) * per_char
    for i in range(slide + typing + hold + sent):
        canvas = base.convert("RGBA")
        if i < slide:
            k = kit.ease_out_back(i / slide, 1.4)
            kit.paste(canvas, chat_pill(prompt, 0, True, pill_w), px, py + (1 - k) * 60, alpha=min(1.0, i / 3))
        elif i < slide + typing + hold:
            typed = min(len(prompt), (i - slide) // per_char + 1)
            caret = (i // 5) % 2 == 0 or typed < len(prompt)
            kit.paste(canvas, chat_pill(prompt, typed, caret, pill_w), px, py)
        else:
            k = (i - slide - typing - hold + 1) / sent
            kit.paste(canvas, chat_pill(prompt, len(prompt), False, pill_w), px, py - kit.ease_in(k) * 26, alpha=1 - k)
        kit.frame_border(canvas)
        out.append(canvas.convert("RGB"))
    for f in action:
        canvas = f.convert("RGBA")
        kit.frame_border(canvas)
        frame = canvas.convert("RGB")
        out.extend([frame] * step)
    return finish(out, name)


def meet(lang, prompt):
    return chat_scene(f"meet-{lang}.gif", prompt, f"meet_{lang}", 96, (250, -40, 550, 400), (144, -40, 656, 124),
                      plate="c_plate_empty.png")


def talk(lang, prompt):
    return chat_scene(f"talk-{lang}.gif", prompt, f"talk_{lang}", 106, (256, -40, 544, 400), (162, -40, 638, 124))


# ------------------------------------------------------------------ outfits

CLOSE = (212, 205, 1388, 675)       # the meadow, closer: herself and the ground, 800 x 320 once scaled
CLOSE_SIZE = (W, 320)
SKINS = ["laura", "laura_summer", "laura_winter", "laura_night", "laura_sporty", "laura_gothic"]
SKIN_DIR = os.path.dirname(SKIN)


def skin_face(name, scale):
    skin = Image.open(os.path.join(SKIN_DIR, name + ".png")).convert("RGBA")
    fx, fy = skins.faces(skins.HEAD)["front"][:2]
    hx, hy = skins.faces(skins.HAT)["front"][:2]
    face = Image.alpha_composite(skin.crop((fx, fy, fx + 8, fy + 8)), skin.crop((hx, hy, hx + 8, hy + 8)))
    return face.resize((8 * scale, 8 * scale), Image.NEAREST)


def outfits():
    w, h = CLOSE_SIZE
    stills = [kit.load(src(f"skin2_{n}.png"), size=CLOSE_SIZE, crop=CLOSE) for n in SKINS]
    base = stills[0]
    box = (296, -40, 504, h + 40)
    mask = kit.feather_mask((w, h), box, 10)
    looks = [Image.composite(st, base, mask) for st in stills]
    faces = [skin_face(n, 5) for n in SKINS]
    sparkle = kit.icon("SPARKLE", 2)
    small_sparkle = kit.icon("SPARKLE", 1)
    star = kit.icon("STAR", 1)
    hold, wipe = 20, 7
    out = []
    card_x, card_y = 600, 44
    for k in range(len(SKINS)):
        cur, nxt = looks[k], looks[(k + 1) % len(SKINS)]
        for i in range(hold + wipe):
            if i < hold:
                img = cur
                line = None
            else:
                # The new outfit is revealed by a line that sweeps across her, sparkles on its edge.
                t = (i - hold + 1) / wipe
                line = int(kit.lerp(box[0], box[2], kit.ease_in_out(t)))
                img = cur.copy()
                img.paste(nxt.crop((box[0], 0, line, h)), (box[0], 0))
            canvas = img.convert("RGBA")
            if line is not None:
                glow = Image.new("RGBA", (6, 260), (255, 255, 255, 150))
                canvas.alpha_composite(glow, (line - 3, 40))
                for j, dy in enumerate((60, 120, 185, 245)):
                    kit.paste(canvas, sparkle if j % 2 == 0 else small_sparkle, line + (4 if j % 2 else -6), dy + (i * 7 + j * 13) % 17, anchor="c")
            # The card of the six outfits, as in the Style tab of her menu.
            card = kit.panel(156, 232, radius=10, fill=CREAM, border=PINK_DEEP)
            card.alpha_composite(kit.icon("TAB_STYLE", 2), (14, 10))
            for d in range(3):
                card.alpha_composite(kit.icon("HEART_FULL", 1), (58 + d * 20, 18))
            active = k if i < hold else (k + 1) % len(SKINS)
            for j, face in enumerate(faces):
                tx = 14 + (j % 2) * 66
                ty = 50 + (j // 2) * 60
                on = j == active
                tile = kit.rounded(60, 54, 8, (255, 214, 232, 255) if on else (238, 228, 255, 255), ROSE if on else LAVENDER_DEEP, 3 if on else 2)
                tile.alpha_composite(face, (10, 7))
                pop = 1.0
                if on and i >= hold:
                    pop = 1 + 0.12 * math.sin(kit.span(i, hold, hold + wipe) * math.pi)
                kit.paste(card, tile, tx + 30, ty + 27, scale=pop, anchor="c")
                if on:
                    kit.paste(card, star, tx + 52, ty + 4, anchor="c")
            canvas.alpha_composite(card, (card_x, card_y))
            kit.frame_border(canvas)
            out.append(canvas.convert("RGB"))
    return finish(out, "outfits.gif")


# ------------------------------------------------------------------ emotes

EMOTE_SEQUENCE = [("EMOTE_WAVE", "em_wave", 40), ("EMOTE_TWIRL", "em_twirl", 32), ("EMOTE_BLOW_KISS", "em_blow_kiss", 34),
                  ("EMOTE_AIR_GUITAR", "em_air_guitar", 44)]
WHEEL_EXTRA = ["EMOTE_CLAP", "EMOTE_DANCE", "EMOTE_LAUGH", "EMOTE_JUMP"]


def disc(icon_name, size, border, icon_scale=2, fill=CREAM):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([0, 0, size - 1, size - 1], fill=border)
    d.ellipse([3, 3, size - 4, size - 4], fill=fill)
    ic = kit.icon(icon_name, icon_scale)
    img.alpha_composite(ic, ((size - ic.width) // 2, (size - ic.height) // 2))
    return img


def emotes(step=1):
    w, h = CLOSE_SIZE
    names = [e[0] for e in EMOTE_SEQUENCE] + WHEEL_EXTRA
    order = [names[0], names[4], names[1], names[5], names[2], names[6], names[3], names[7]]
    cx, cy, radius = 150, 160, 96
    box = (268, -40, 532, h + 40)
    base = kit.load(src("em_wave_0001.png"), size=CLOSE_SIZE, crop=CLOSE)
    out = []
    for index, (icon_name, clip_name, duration) in enumerate(EMOTE_SEQUENCE):
        frames = footage(clip(clip_name, 6, 9 + duration + 4, step), CLOSE, CLOSE_SIZE)
        frames = composite_region(frames, base, box, feather=14)
        frames = loop_tail(frames, base, max(2, 4 // step))
        frames = kit.stabilize([base] + frames, 7)[1:]
        slot = order.index(icon_name)
        total = len(frames) * step
        for i in range(total):
            canvas = frames[i // step].convert("RGBA")
            # The emote wheel: eight discs around the one that plays.
            pop = kit.ease_out_back(kit.span(i, 0, 7), 2.2)
            for j, name in enumerate(order):
                a = -math.pi / 2 + j * math.tau / len(order)
                x = cx + math.cos(a) * radius
                y = cy + math.sin(a) * radius
                on = j == slot
                border = ROSE if on else (LAVENDER_DEEP if j % 2 else PINK_DEEP)
                d = disc(name, 46, border)
                kit.paste(canvas, d, x, y, scale=1 + 0.28 * pop if on else 1.0, anchor="c", alpha=1.0 if on else 0.92)
            centre = disc(icon_name, 92, ROSE, icon_scale=4, fill=(255, 236, 244, 255))
            kit.paste(canvas, centre, cx, cy, scale=0.86 + 0.14 * pop, anchor="c")
            kit.frame_border(canvas)
            out.append(canvas.convert("RGB"))
    return finish(out, "emotes.gif")


# ------------------------------------------------------------------ needs and wishes

NEEDS = [("NEED_HUNGER", (255, 179, 138, 255)), ("NEED_ENERGY", (179, 157, 255, 255)), ("NEED_FUN", (126, 224, 195, 255)),
         ("NEED_ATTENTION", (247, 127, 178, 255)), ("NEED_HYGIENE", (143, 211, 255, 255))]


def needs():
    w, h = CLOSE_SIZE
    base = kit.load(src("r_plate.png"), size=CLOSE_SIZE, crop=(362, 205, 1538, 675))    # she stands left of centre
    bubble = Image.open(os.path.join(os.path.dirname(SKIN_DIR), "thought_bubble.png")).convert("RGBA").resize((96, 96), Image.NEAREST)
    food = kit.icon("EAT", 3)
    hearts = [kit.heart(1, PINK_DEEP, OUTLINE), kit.heart(2, PINK_DEEP, OUTLINE), kit.heart(1, WHITE, (235, 150, 190, 255))]
    n = 140
    lx, ly = 298, 70          # her head
    out = []
    for i in range(n):
        t = i / n
        canvas = base.convert("RGBA")
        # Timeline: hunger falls, a wish appears, food arrives, everything goes up again, then time passes
        # and the values glide back to where the loop starts.
        fall = kit.ease_in_out(kit.span(t, 0.04, 0.28))
        fed = kit.ease_out(kit.span(t, 0.52, 0.64))
        settle = kit.ease_in_out(kit.span(t, 0.87, 1.0))
        hunger = kit.lerp(kit.lerp(kit.lerp(0.62, 0.12, fall), 0.96, fed), 0.62, settle)
        hungry = 0.20 < t < 0.54
        wish = kit.ease_out_back(kit.span(t, 0.22, 0.30), 2.0) * (1 - kit.span(t, 0.53, 0.57))
        if wish > 0.01:
            bob = math.sin(t * math.tau * 3) * 3
            b = bubble.copy()
            b.alpha_composite(food, (24, 14))
            kit.paste(canvas, b, lx + 64, ly - 13 + bob, scale=wish, anchor="c")
        # The food flies to her in an arc.
        fly = kit.span(t, 0.40, 0.53)
        if 0 < fly < 1:
            fx = kit.lerp(60, lx, kit.ease_in_out(fly))
            fy = kit.lerp(300, ly + 70, fly) - math.sin(fly * math.pi) * 90
            kit.paste(canvas, food, fx, fy, anchor="c", scale=1.0)
        # Hearts once she is fed.
        burst = kit.span(t, 0.53, 0.82)
        if 0 < burst < 1:
            for j in range(7):
                ang = -math.pi / 2 + (j - 3) * 0.42
                dist = 30 + 70 * kit.ease_out(burst)
                kit.paste(canvas, hearts[j % 3], lx + math.cos(ang) * dist, ly + 40 + math.sin(ang) * dist, alpha=1 - kit.ease_in(burst), anchor="c")
        # The card of her needs, as in her menu.
        card = kit.panel(330, 232, radius=10, fill=CREAM, border=PINK_DEEP)
        mood = "MOOD_HUNGRY" if hungry else ("MOOD_IN_LOVE" if 0.54 <= t < 0.87 else "MOOD_HAPPY")
        shake = math.sin(i * 1.9) * 2 if hungry and t < 0.32 else 0
        card.alpha_composite(kit.icon(mood, 2), (14 + int(shake), 12))
        font.draw(card, "Laura", 56, 14, 3, PLUM)
        for k in range(10):
            x = 56 + k * 18
            card.alpha_composite(kit.icon("HEART_FULL" if k < 6 else "HEART_EMPTY", 1), (x, 46))
            if k == 6 and t >= 0.58:
                # The heart she gains when she is fed; it fades as the loop comes round.
                kit.paste(card, kit.icon("HEART_FULL", 1), x + 8, 46 + 8, alpha=1 - kit.span(t, 0.90, 0.99),
                          scale=max(0.05, kit.ease_out_back(kit.span(t, 0.58, 0.66), 3.0)), anchor="c")
        values = [hunger, 0.91, kit.lerp(kit.lerp(0.80, 0.97, fed), 0.80, settle), kit.lerp(kit.lerp(0.74, 0.95, fed), 0.74, settle), 0.95]
        for k, (icon_name, color) in enumerate(NEEDS):
            y = 78 + k * 29
            card.alpha_composite(kit.icon(icon_name, 1), (16, y))
            blink = k == 0 and hungry and (i // 4) % 2 == 0
            card.alpha_composite(kit.bar(206, 14, values[k], (255, 120, 110, 255) if blink else color), (42, y + 1))
            font.draw(card, f"{int(round(values[k] * 100))}%", 258, y + 1, 2, PLUM_SOFT)
        canvas.alpha_composite(card, (440, 44))
        kit.frame_border(canvas)
        out.append(canvas.convert("RGB"))
    return finish(out, "needs.gif")


# ------------------------------------------------------------------ her jobs

FIELD = (200, 185, 1600, 745)       # the wheat field, 800 x 320 once scaled
FIELD_CLIP = (24, 136)              # the frames of the clip that are shown
HARVESTS = [44, 58, 72, 86, 100, 114, 128, 142]     # frames where a crop is cut (logged by the capture tool)
JOB_AREAS = [(160, 140, 328, 184), (344, 140, 512, 184), (528, 140, 696, 184)]    # the item of each job, in the Work tab

WHEAT = [
    "................",
    ".......gg.......",
    "......glgg......",
    "..gg..glgg..gg..",
    ".glgg.glgg.glgg.",
    ".glgg.gggg.glgg.",
    ".gggg.gggg.gggg.",
    ".gggg..ss..gggg.",
    "..gg...ss...gg..",
    "...ss..ss..ss...",
    "....ss.ss.ss....",
    ".....rrrrrr.....",
    ".....rrrrrr.....",
    "......ssss......",
    ".....ss..ss.....",
    "................",
]
WHEAT_COLORS = {"g": kit.GOLD, "l": LEMON, "s": (206, 156, 62, 255), "r": ROSE}


def pixel_sprite(rows, colors, scale=1, outline=PLUM):
    """A sprite drawn with one letter per pixel, outlined like the icons of the menu."""
    img = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    px = img.load()
    solid = set()
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c in colors:
                px[x, y] = colors[c]
                solid.add((x, y))
    for y in range(img.height):
        for x in range(img.width):
            if (x, y) not in solid and any((x + dx, y + dy) in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = outline
    return img.resize((img.width * scale, img.height * scale), Image.NEAREST) if scale != 1 else img


def tab_items(shot, areas, tolerance=6):
    """Cuts the items drawn on a row of tiles of a captured tab: the fill of the tile becomes transparent."""
    out = []
    for area in areas:
        tile = shot.crop(area).convert("RGBA")
        colors = tile.getcolors(tile.width * tile.height)
        fill = max(colors)[1]
        px = tile.load()
        for y in range(tile.height):
            for x in range(tile.width):
                r, g, b, _ = px[x, y]
                if max(abs(r - fill[0]), abs(g - fill[1]), abs(b - fill[2])) <= tolerance:
                    px[x, y] = (0, 0, 0, 0)
        out.append(tile.crop(tile.getbbox()))
    return out


def work():
    w, h = CLOSE_SIZE
    first, last = FIELD_CLIP
    harvests = [e for e in HARVESTS if e <= last - 6]
    frames = footage(clip("farm", first, last), FIELD, CLOSE_SIZE)
    base = kit.load(src("farm_0008.png"), size=CLOSE_SIZE, crop=FIELD)
    box = (30, -40, 600, h + 40)
    action = composite_region(frames, base, box, feather=14)
    action = kit.stabilize([base] + action, 10)[1:]
    end = action[-1]

    items = tab_items(kit.load(src("m_en_work.png"), crop=MENU_CROP), JOB_AREAS)
    wheat = pixel_sprite(WHEAT, WHEAT_COLORS, 2)
    cursor = pointer(2)
    sparkle = kit.icon("SPARKLE", 2)
    small_sparkle = kit.icon("SPARKLE", 1)
    star = kit.icon("STAR", 1)

    intro, wipe, outro = 16, 12, 8
    click = 10
    events = [intro + (e - first) for e in harvests]
    flight = 13
    total = intro + len(action) + wipe + outro
    card_x, card_y, card_w, card_h = 652, 20, 134, 280
    tile_w, tile_h = 106, 52
    tile_x = (card_w - tile_w) // 2
    tile_y = [54 + k * (tile_h + 6) for k in range(3)]
    count_at = (card_x + 40, card_y + 248)         # where the harvest lands, on the canvas
    her = (343, 196)

    out = []
    for i in range(total):
        if i < intro:
            img, line = base, None
        elif i < intro + len(action):
            img, line = action[i - intro], None
        elif i < intro + len(action) + wipe:
            # The field grows back behind a line that sweeps across it, and the loop starts again.
            t = (i - intro - len(action) + 1) / wipe
            line = int(kit.lerp(box[0], box[2], kit.ease_in_out(t)))
            img = end.copy()
            img.paste(base.crop((0, 0, line, h)), (0, 0))
        else:
            img, line = base, None
        canvas = img.convert("RGBA")
        if line is not None:
            canvas.alpha_composite(Image.new("RGBA", (6, h - 60), (255, 255, 255, 150)), (line - 3, 30))
            for j, dy in enumerate((54, 112, 176, 238)):
                kit.paste(canvas, sparkle if j % 2 == 0 else small_sparkle, line + (4 if j % 2 else -6), dy + (i * 7 + j * 13) % 17, anchor="c")

        working = click <= i < intro + len(action) + wipe
        done = sum(1 for e in events if i >= e + flight) if working else 0
        # The card of her jobs, as in the Work tab of her menu: the farmer is switched on.
        card = kit.panel(card_w, card_h, radius=10, fill=CREAM, border=PINK_DEEP)
        card.alpha_composite(kit.icon("TAB_WORK", 2), (14, 12))
        for d in range(3):
            card.alpha_composite(kit.icon("HEART_FULL", 1), (58 + d * 20, 20))
        for k, item in enumerate(items):
            on = k == 1 and working
            tile = kit.rounded(tile_w, tile_h, 8, PINK if on else (238, 228, 255, 255), ROSE if on else LAVENDER_DEEP, 3 if on else 2)
            tile.alpha_composite(item, ((tile_w - item.width) // 2, (tile_h - item.height) // 2))
            pop = 1.0
            if on:
                pop = 1 + 0.14 * math.sin(kit.span(i, click, click + 7) * math.pi)
            kit.paste(card, tile, tile_x + tile_w // 2, tile_y[k] + tile_h // 2, scale=pop, anchor="c")
            if on:
                kit.paste(card, star, tile_x + tile_w - 6, tile_y[k] + 5, anchor="c")
        # The harvest counter.
        last_hit = max([e + flight for e in events if i >= e + flight], default=None) if working else None
        bump = 1 + 0.35 * (1 - kit.span(i, last_hit, last_hit + 6)) if last_hit is not None else 1.0
        kit.paste(card, wheat, 40, 248, scale=bump, anchor="c")
        label = kit.text_image(f"x{done}", 3, PLUM)
        card.alpha_composite(label, (64, 248 - 10 - font.ACCENT_ROWS * 3))
        canvas.alpha_composite(card, (card_x, card_y))

        # Each cut crop flies from her hands to the counter.
        for n, e in enumerate(events):
            if working and e <= i < e + flight:
                t = (i - e + 1) / flight
                side = -1 if n % 2 == 0 else 1
                x0, y0 = her[0] + side * 44, her[1]
                k = kit.ease_in_out(t)
                x = kit.lerp(x0, count_at[0], k)
                y = kit.lerp(y0, count_at[1], k) - math.sin(t * math.pi) * 96
                kit.paste(canvas, wheat, x, y, scale=kit.lerp(0.6, 1.0, kit.ease_out(t * 3)), anchor="c")
                kit.paste(canvas, small_sparkle, x - 14 + (i * 5) % 9, y + 12, alpha=0.8, anchor="c")
        # The pointer picks the job in the menu, then leaves her to it.
        tx, ty = card_x + tile_x + tile_w // 2 + 8, card_y + tile_y[1] + tile_h // 2 + 4
        if i < click + 18:
            if i <= click:
                k = kit.ease_in_out(i / click)
                cx, cy = kit.lerp(tx - 120, tx, k), kit.lerp(ty + 96, ty, k)
            else:
                k = kit.ease_in(kit.span(i, click + 6, click + 18))
                cx, cy = tx + k * 40, ty + k * 210
            if click <= i < click + 6:
                r = 8 + (i - click) * 4
                ring = Image.new("RGBA", (r * 2 + 4, r * 2 + 4), (0, 0, 0, 0))
                ImageDraw.Draw(ring).ellipse([1, 1, r * 2 + 2, r * 2 + 2], outline=(224, 85, 143, int(230 * (1 - (i - click) / 6))), width=3)
                kit.paste(canvas, ring, tx - 8, ty - 4, anchor="c")
            canvas.alpha_composite(cursor, (int(cx), int(cy)))
        kit.frame_border(canvas)
        out.append(canvas.convert("RGB"))
    return finish(out, "work.gif")


# ------------------------------------------------------------------ her menu

TABS = ["home", "orders", "emotes", "work", "fetch", "style", "settings"]
MENU_CROP = (400, 210, 1200, 690)

POINTER = [
    "X..........",
    "XX.........",
    "XoX........",
    "XooX.......",
    "XoooX......",
    "XooooX.....",
    "XoooooX....",
    "XooooooX...",
    "XoooooooX..",
    "XooooXXXXX.",
    "XooXooX....",
    "XoX.XooX...",
    "XX..XooX...",
    "X....XooX..",
    ".....XooX..",
    "......XX...",
]


def pointer(scale=2):
    img = Image.new("RGBA", (len(POINTER[0]), len(POINTER)), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(POINTER):
        for x, c in enumerate(row):
            if c == "X":
                px[x, y] = PLUM
            elif c == "o":
                px[x, y] = WHITE
    return img.resize((img.width * scale, img.height * scale), Image.NEAREST)


def menu(lang):
    size = (W, 480)
    shots = [kit.load(src(f"m_{lang}_{t}.png"), crop=MENU_CROP) for t in TABS]
    base = shots[0]
    mask = kit.feather_mask(size, (74, 17, 726, 463), 4)
    shots = [base] + [Image.composite(sh, base, mask) for sh in shots[1:]]
    cursor = pointer(2)
    hold, move = 26, 9
    out = []
    for k in range(len(TABS)):
        nxt = (k + 1) % len(TABS)
        y0, y1 = 118 + 46 * k, 118 + 46 * nxt
        for i in range(hold):
            canvas = shots[k].convert("RGBA")
            if i < hold - move:
                cx, cy = 126, y0 + 6
            else:
                t = kit.ease_in_out((i - (hold - move) + 1) / move)
                cx, cy = 126 + math.sin(t * math.pi) * 14, kit.lerp(y0, y1, t) + 6
            # A ring where the tab was just clicked.
            if i < 6:
                r = 8 + i * 4
                ring = Image.new("RGBA", (r * 2 + 4, r * 2 + 4), (0, 0, 0, 0))
                ImageDraw.Draw(ring).ellipse([1, 1, r * 2 + 2, r * 2 + 2], outline=(224, 85, 143, int(230 * (1 - i / 6))), width=3)
                kit.paste(canvas, ring, 118, y0, anchor="c")
            canvas.alpha_composite(cursor, (int(cx), int(cy)))
            kit.frame_border(canvas)
            out.append(canvas.convert("RGB"))
    return finish(out, f"menu-{lang}.gif")


# ------------------------------------------------------------------ section titles

# File name, text, icon of the menu. One title per section of the page, in each language.
TITLES = [
    ("title-meet-en", "MEET HER", "AFFECTION"),
    ("title-does-en", "WHAT SHE DOES", "TAB_WORK"),
    ("title-yours-en", "MAKE HER YOURS", "TAB_STYLE"),
    ("title-multiplayer-en", "MULTIPLAYER", "HUG"),
    ("title-config-en", "CONFIGURATION AND LANGUAGES", "TAB_SETTINGS"),
    ("title-integrations-en", "OPTIONAL INTEGRATIONS", "BACKPACK"),
    ("title-versions-en", "SUPPORTED VERSIONS", "CHECK"),
    ("title-start-en", "GETTING STARTED", "STAR"),
    ("title-guides", "GUIDES", "TAB_INFO"),
    ("title-meet-fr", "LA RENCONTRER", "AFFECTION"),
    ("title-does-fr", "CE QU'ELLE FAIT", "TAB_WORK"),
    ("title-yours-fr", "LA PERSONNALISER", "TAB_STYLE"),
    ("title-multiplayer-fr", "MULTIJOUEUR", "HUG"),
    ("title-config-fr", "CONFIGURATION ET LANGUES", "TAB_SETTINGS"),
    ("title-integrations-fr", "INTÉGRATIONS FACULTATIVES", "BACKPACK"),
    ("title-versions-fr", "VERSIONS SUPPORTÉES", "CHECK"),
    ("title-start-fr", "PREMIERS PAS", "STAR"),
]
# The two halves of the page.
LANGUAGES = [("lang-en", "ENGLISH"), ("lang-fr", "FRANÇAIS")]
LILAC = (238, 228, 255, 255)
LILAC_SHADE = (140, 118, 224, 255)
TITLE_FRAMES = 24       # 80 ms each


def hop(t, at, height=3, length=0.24):
    """Vertical offset of a sprite that hops once per loop, around the moment `at` (0..1)."""
    k = (t - at) % 1.0
    return -int(round(math.sin(k / length * math.pi) * height)) if k < length else 0


def ribbon(text, icon_name, fill=CREAM, border=PINK_DEEP, shade=ROSE, ink=PLUM):
    """A section title on a see-through background: the text on a pill between two icons of the menu,
    with a line, a sparkle and a heart on each side. The icons and the hearts hop in turn."""
    w, h = W, 60
    top, body = 6, 44
    ic = kit.icon(icon_name, 2)
    tw = font.width(text, 3)
    pw = 18 + ic.width + 12 + tw + 12 + ic.width + 18
    px = (w - pw) // 2
    pill = Image.new("RGBA", (pw, body + 4), (0, 0, 0, 0))
    pill.alpha_composite(kit.rounded(pw, body, body // 2, shade), (0, 4))
    pill.alpha_composite(kit.rounded(pw, body, body // 2, fill, border, 3), (0, 0))
    font.draw(pill, text, 18 + ic.width + 12, (body - font.height(3)) // 2, 3, ink)
    heart = kit.heart(1, PINK_DEEP, OUTLINE)
    spark = kit.icon("SPARKLE", 1)
    y = top + body // 2
    lines = ((46, px - 14), (px + pw + 14, w - 46))
    out = []
    for i in range(TITLE_FRAMES):
        t = i / TITLE_FRAMES
        canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(canvas)
        for k, (x0, x1) in enumerate(lines):
            d.rectangle([x0, y - 1, x1, y + 1], fill=border)
            # A sparkle blinks on each line, when the line is long enough to hold one.
            if x1 - x0 >= 64 and (t + 0.5 * k) % 1.0 < 0.42:
                kit.paste(canvas, spark, (x0 + x1) // 2, y, anchor="c")
        canvas.alpha_composite(pill, (px, top))
        canvas.alpha_composite(ic, (px + 18, top + 6 + hop(t, 0.04)))
        canvas.alpha_composite(ic, (px + pw - 18 - ic.width, top + 6 + hop(t, 0.54)))
        kit.paste(canvas, heart, 30, y + hop(t, 0.29), anchor="c")
        kit.paste(canvas, heart, w - 30, y + hop(t, 0.79), anchor="c")
        out.append(canvas)
    return out


def divider():
    """A line with three hearts that hop one after the other, to close a part of the page."""
    w, h = W, 44
    big = kit.heart(2, PINK_DEEP, OUTLINE)
    small = kit.heart(1, PINK_DEEP, OUTLINE)
    y = h // 2
    out = []
    for i in range(TITLE_FRAMES):
        t = i / TITLE_FRAMES
        canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(canvas)
        d.rectangle([120, y - 1, w // 2 - 52, y + 1], fill=PINK_DEEP)
        d.rectangle([w // 2 + 52, y - 1, w - 120, y + 1], fill=PINK_DEEP)
        kit.paste(canvas, small, w // 2 - 32, y + 2 + hop(t, 0.05), anchor="c")
        kit.paste(canvas, big, w // 2, y + hop(t, 0.20, height=4), anchor="c")
        kit.paste(canvas, small, w // 2 + 32, y + 2 + hop(t, 0.35), anchor="c")
        out.append(canvas)
    return out


# The icons of the menu shown next to the names of the features on the page.
PAGE_ICONS = ["COMBAT_FIGHT", "COMBAT_PASSIVE", "COME", "DESIRE", "EAT", "EMOTE_WAVE", "FOLLOW", "GO_HOME", "HEALTH", "HEART_FULL", "HUG",
              "INVENTORY", "LOCK", "MOOD_HAPPY", "NEED_ATTENTION", "PENCIL", "PLUS", "QUEUE", "REFRESH", "STAR", "STAY", "TAB_EMOTES",
              "TAB_FETCH", "TAB_HOME", "TAB_ORDERS", "TAB_SETTINGS", "TAB_STYLE", "TAB_WORK", "UNGAG", "UPLOAD"]


def icons():
    folder = os.path.join(OUT, "icons")
    os.makedirs(folder, exist_ok=True)
    for name in PAGE_ICONS:
        kit.icon(name, 2).save(os.path.join(folder, name.lower().replace("_", "-") + ".png"))
    print(f"icons: {len(PAGE_ICONS)} files, 32 pixels wide")


def titles():
    os.makedirs(OUT, exist_ok=True)
    built = [(name, ribbon(text, icon_name)) for name, text, icon_name in TITLES]
    built += [(name, ribbon(text, "TAB_ORDERS", fill=LILAC, border=LAVENDER_DEEP, shade=LILAC_SHADE)) for name, text in LANGUAGES]
    built.append(("divider", divider()))
    total = 0
    for name, frames in built:
        total += kit.write_sprite_gif(frames, os.path.join(OUT, name + ".gif"))
    print(f"titles: {len(built)} files, {frames[0].width} pixels wide, {total / 1e3:.0f} KB in all")
    return total


BUILDERS = {
    "hero": hero,
    "meet-en": lambda: meet("en", "I feel lonely"),
    "meet-fr": lambda: meet("fr", "je me sens seul"),
    "talk-en": lambda: talk("en", "dance for me!"),
    "talk-fr": lambda: talk("fr", "danse pour moi !"),
    "needs": needs,
    "work": work,
    "outfits": outfits,
    "emotes": emotes,
    "menu-en": lambda: menu("en"),
    "menu-fr": lambda: menu("fr"),
    "titles": titles,
    "icons": icons,
}


def main():
    global SRC, OUT
    parser = argparse.ArgumentParser()
    parser.add_argument("names", nargs="*", default=list(BUILDERS))
    parser.add_argument("--src", default=SRC)
    parser.add_argument("--out", default=OUT)
    args = parser.parse_args()
    SRC, OUT = args.src, args.out
    for name in args.names:
        BUILDERS[name]()


if __name__ == "__main__":
    main()
