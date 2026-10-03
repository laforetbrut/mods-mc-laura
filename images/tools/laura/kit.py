"""Drawing and animation helpers for the page media (GIF banners in the style of the mod).

Author: vyrriox
"""
import math
import os
import shutil
import subprocess
import sys
import tempfile

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(HERE), "assets"))

import icons as icon_lib  # noqa: E402
from icons import (CREAM, GOLD, LAVENDER, LAVENDER_DEEP, LEMON, MINT, MINT_DEEP, PEACH, PEACH_DEEP, PINK, PINK_DEEP, PLUM,  # noqa: E402,F401
                   PLUM_SOFT, RED, ROSE, SKY, SKY_DEEP, WHITE)

import pixelfont as font  # noqa: E402

FPS = 20


# ------------------------------------------------------------------ easing

def clamp(t, lo=0.0, hi=1.0):
    return lo if t < lo else hi if t > hi else t


def lerp(a, b, t):
    return a + (b - a) * t


def ease_out(t):
    t = clamp(t)
    return 1 - (1 - t) ** 3


def ease_in(t):
    t = clamp(t)
    return t ** 3


def ease_in_out(t):
    t = clamp(t)
    return 4 * t ** 3 if t < 0.5 else 1 - (-2 * t + 2) ** 3 / 2


def ease_out_back(t, overshoot=1.70158):
    t = clamp(t)
    c3 = overshoot + 1
    return 1 + c3 * (t - 1) ** 3 + overshoot * (t - 1) ** 2


def span(t, start, end):
    """Progress 0..1 of t between start and end (in the same unit as t)."""
    if end <= start:
        return 1.0 if t >= end else 0.0
    return clamp((t - start) / (end - start))


# ------------------------------------------------------------------ sprites

_icon_cache = {}


def icon(name, scale=1):
    """An icon of the mod's menu atlas (tools/assets/icons.py), scaled without smoothing."""
    key = (name, scale)
    if key not in _icon_cache:
        img = icon_lib.ICONS[name]()
        if isinstance(img, icon_lib.Icon):
            img = img.image()
        img = img.copy()
        if scale != 1:
            img = img.resize((16 * scale, 16 * scale), Image.NEAREST)
        _icon_cache[key] = img
    return _icon_cache[key]


_heart_cache = {}


def heart(scale=1, color=PINK_DEEP, outline=(150, 40, 90, 255)):
    """The pixel heart of the logo and of Laura's Heart, 16 pixels wide at scale 1."""
    key = (scale, color, outline)
    if key not in _heart_cache:
        img = icon_lib.Icon().heart(8, 8.5, 13, color).finish(outline=outline).image().copy()
        if outline:
            px = img.load()
            for x, y in ((4, 5), (4, 6), (5, 5)):
                px[x, y] = WHITE
        if scale != 1:
            img = img.resize((16 * scale, 16 * scale), Image.NEAREST)
        _heart_cache[key] = img
    return _heart_cache[key]


def with_alpha(img, alpha):
    """A copy of the image with its alpha multiplied (0..1)."""
    if alpha >= 0.999:
        return img
    out = img.copy()
    out.putalpha(out.getchannel("A").point(lambda a: int(a * clamp(alpha))))
    return out


def paste(dst, src, x, y, alpha=1.0, scale=1.0, anchor="tl", smooth=False):
    """Alpha composites src on dst. anchor: tl (top left) or c (centre); scale resizes around the anchor."""
    if alpha <= 0.003:
        return
    if scale != 1.0:
        w = max(1, int(round(src.width * scale)))
        h = max(1, int(round(src.height * scale)))
        src = src.resize((w, h), Image.LANCZOS if smooth else Image.NEAREST)
    if anchor == "c":
        x -= src.width / 2
        y -= src.height / 2
    src = with_alpha(src, alpha)
    x, y = int(round(x)), int(round(y))
    # alpha_composite needs the source inside the destination.
    sx0, sy0 = max(0, -x), max(0, -y)
    sx1, sy1 = min(src.width, dst.width - x), min(src.height, dst.height - y)
    if sx1 <= sx0 or sy1 <= sy0:
        return
    dst.alpha_composite(src.crop((sx0, sy0, sx1, sy1)), (x + sx0, y + sy0))


def cover(img, w, h, fx=0.5, fy=0.5):
    """Scales and crops the image to fill w x h, keeping the point (fx, fy) of the source in view."""
    s = max(w / img.width, h / img.height)
    nw, nh = int(math.ceil(img.width * s)), int(math.ceil(img.height * s))
    img = img.resize((nw, nh), Image.LANCZOS)
    x = int(round((nw - w) * fx))
    y = int(round((nh - h) * fy))
    return img.crop((x, y, x + w, y + h))


def rounded(w, h, radius, fill, border=None, border_width=2):
    """A pixel sharp rounded rectangle (no smoothing, like the mod's menu)."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if border:
        d.rounded_rectangle([0, 0, w - 1, h - 1], radius=radius, fill=border)
        d.rounded_rectangle([border_width, border_width, w - 1 - border_width, h - 1 - border_width],
                            radius=max(0, radius - border_width), fill=fill)
    else:
        d.rounded_rectangle([0, 0, w - 1, h - 1], radius=radius, fill=fill)
    return img


def panel(w, h, radius=8, fill=CREAM, border=PINK_DEEP, border_width=2, shadow=(91, 42, 77, 70), shadow_offset=3):
    """A card of the kawaii menu: cream body, pink border, soft plum shadow under it."""
    out = Image.new("RGBA", (w + shadow_offset, h + shadow_offset), (0, 0, 0, 0))
    if shadow:
        out.alpha_composite(rounded(w, h, radius, shadow), (shadow_offset, shadow_offset))
    out.alpha_composite(rounded(w, h, radius, fill, border, border_width), (0, 0))
    return out


def gradient(w, h, left, right, vertical=False):
    """A linear gradient between two RGBA colors."""
    n = h if vertical else w
    ramp = np.linspace(0, 1, n, dtype=np.float32)
    a = np.array(left, dtype=np.float32)
    b = np.array(right, dtype=np.float32)
    line = (a[None, :] * (1 - ramp[:, None]) + b[None, :] * ramp[:, None]).astype(np.uint8)
    arr = np.repeat(line[:, None, :], w, axis=1) if vertical else np.repeat(line[None, :, :], h, axis=0)
    return Image.fromarray(arr, "RGBA")


def text_image(text, scale, color, shadow=None, spacing=1):
    """The text on its own transparent image (accent and descender rows included)."""
    w = font.width(text, scale, spacing) + scale
    h = font.LINE_ROWS * scale + scale
    img = Image.new("RGBA", (max(1, w), h), (0, 0, 0, 0))
    font.draw(img, text, 0, font.ACCENT_ROWS * scale, scale, color, shadow, spacing)
    return img


def pill(text, scale=2, icon_name=None, fill=CREAM, border=PINK_DEEP, color=PLUM, pad=8):
    """A label chip: optional icon, then the text."""
    tw = font.width(text, scale)
    th = font.height(scale)
    ic = icon(icon_name, 1 if scale < 3 else 2) if icon_name else None
    w = pad * 2 + tw + (ic.width + 6 if ic else 0)
    h = max(th, ic.height if ic else 0) + pad * 2 - 2
    out = panel(w, h, radius=h // 2, fill=fill, border=border)
    x = pad
    if ic:
        out.alpha_composite(ic, (x, (h - ic.height) // 2))
        x += ic.width + 6
    font.draw(out, text, x, (h - th) // 2, scale, color)
    return out


def bar(w, h, ratio, color, track=(240, 222, 232, 255), border=(214, 190, 205, 255)):
    """A need bar of the overlay: rounded track, coloured fill."""
    img = rounded(w, h, h // 2, track, border, 1)
    fw = int(round((w - 4) * clamp(ratio)))
    if fw > 0:
        img.alpha_composite(rounded(max(fw, h - 4), h - 4, (h - 4) // 2, color), (2, 2))
    return img


# ------------------------------------------------------------------ looping particles

class Floaters:
    """Sprites that drift and fade on a loop of {@code period} frames, so the animation repeats without a jump."""

    def __init__(self, sprites, count, box, period, seed=1, rise=60, sway=8, min_scale=1.0, max_scale=1.0, drift=0):
        import random
        rng = random.Random(seed)
        self.items = []
        self.period = period
        for i in range(count):
            self.items.append({
                "sprite": rng.choice(sprites),
                "x": rng.uniform(box[0], box[2]),
                "y": rng.uniform(box[1], box[3]),
                "phase": rng.random(),
                "rise": rise * rng.uniform(0.7, 1.3),
                "sway": sway * rng.uniform(0.5, 1.2),
                "swayPhase": rng.uniform(0, math.tau),
                "scale": rng.uniform(min_scale, max_scale),
                "drift": drift * rng.uniform(0.6, 1.4),
                "loops": rng.choice((1, 1, 2)),
            })

    def draw(self, dst, frame, alpha=1.0):
        for it in self.items:
            t = (frame / self.period * it["loops"] + it["phase"]) % 1.0
            a = min(1.0, t / 0.18, (1 - t) / 0.3) * alpha
            x = it["x"] + math.sin(t * math.tau + it["swayPhase"]) * it["sway"] + it["drift"] * t
            y = it["y"] - it["rise"] * t
            paste(dst, it["sprite"], x, y, alpha=a, scale=it["scale"], anchor="c")


def twinkle(dst, sprite, x, y, frame, period, phase=0.0, alpha=1.0):
    """A sparkle that grows and fades on a loop."""
    t = (frame / period + phase) % 1.0
    k = math.sin(t * math.pi) ** 2
    paste(dst, sprite, x, y, alpha=k * alpha, scale=0.5 + 0.5 * k, anchor="c")


# ------------------------------------------------------------------ footage

def load(path, size=None, crop=None):
    """Loads a captured frame. crop is (x0, y0, x1, y1) in source pixels, applied before the resize."""
    img = Image.open(path).convert("RGB")
    if crop:
        img = img.crop(crop)
    if size:
        img = img.resize(size, Image.LANCZOS)
    return img


def feather_mask(size, box, feather):
    """White inside box, fading to black over {@code feather} pixels inside its edges."""
    mask = Image.new("L", size, 0)
    inner = (box[0] + feather, box[1] + feather, box[2] - feather, box[3] - feather)
    ImageDraw.Draw(mask).rectangle(inner, fill=255)
    return mask.filter(ImageFilter.GaussianBlur(feather / 2.0)) if feather > 0 else mask


def stabilize(frames, threshold=7):
    """Keeps a pixel as it was in the previous frame when it barely changed: static areas of game footage
    (renderer noise) become identical from frame to frame, which is what makes a GIF small."""
    out = []
    prev = None
    for f in frames:
        arr = np.asarray(f.convert("RGB"), dtype=np.int16)
        if prev is None:
            cur = arr
        else:
            diff = np.abs(arr - prev).max(axis=2)
            keep = diff < threshold
            cur = np.where(keep[:, :, None], prev, arr)
        prev = cur
        out.append(Image.fromarray(cur.astype(np.uint8), "RGB"))
    return out


def crossfade_loop(frames, overlap):
    """Blends the last {@code overlap} frames into the first ones so the clip loops without a jump."""
    if overlap <= 0 or len(frames) <= overlap * 2:
        return frames
    n = len(frames) - overlap
    out = []
    for i in range(n):
        if i < overlap:
            t = (i + 1) / (overlap + 1)
            out.append(Image.blend(frames[n + i], frames[i], t))
        else:
            out.append(frames[i])
    return out


# ------------------------------------------------------------------ output

def write_gif(frames, path, fps=FPS, colors=256, dither="bayer:bayer_scale=5", stats="full"):
    """Encodes the frames with one palette for the whole clip (ffmpeg palettegen and paletteuse)."""
    tmp = tempfile.mkdtemp(prefix="lauramedia_")
    try:
        for i, f in enumerate(frames):
            f.convert("RGB").save(os.path.join(tmp, f"f_{i:04d}.png"), compress_level=1)
        vf = (f"[0:v]split[a][b];[a]palettegen=max_colors={colors}:stats_mode={stats}[p];"
              f"[b][p]paletteuse=dither={dither}:diff_mode=rectangle")
        cmd = ["ffmpeg", "-y", "-v", "error", "-framerate", str(fps), "-i", os.path.join(tmp, "f_%04d.png"),
               "-filter_complex", vf, "-loop", "0", path]
        subprocess.run(cmd, check=True)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    return os.path.getsize(path)


def write_sprite_gif(frames, path, duration=80, threshold=128):
    """Encodes pixel art drawn on RGBA frames as a GIF with a see-through background: one palette for
    all the frames, hard edges (a pixel is either shown or not), every frame drawn on a clean slate."""
    arrays = [np.asarray(f.convert("RGBA")) for f in frames]
    index = {}
    packed = []
    for a in arrays:
        rgb = (a[:, :, 0].astype(np.uint32) << 16) | (a[:, :, 1].astype(np.uint32) << 8) | a[:, :, 2].astype(np.uint32)
        packed.append(rgb)
        for value in np.unique(rgb[a[:, :, 3] >= threshold]):
            index.setdefault(int(value), len(index) + 1)
    if len(index) > 255:
        raise ValueError(f"{len(index)} colors, a GIF holds 255 and the transparent one")
    palette = [0, 0, 0]
    for value in index:
        palette += [value >> 16, (value >> 8) & 255, value & 255]
    images = []
    for a, rgb in zip(arrays, packed):
        values, inverse = np.unique(rgb, return_inverse=True)
        lookup = np.array([index.get(int(v), 0) for v in values], dtype=np.uint8)
        pixels = lookup[inverse].reshape(rgb.shape)
        pixels[a[:, :, 3] < threshold] = 0
        img = Image.fromarray(pixels, "P")
        img.putpalette(palette)
        images.append(img)
    images[0].save(path, save_all=True, append_images=images[1:], duration=duration, loop=0, transparency=0, disposal=2,
                   optimize=False)
    return os.path.getsize(path)


def frame_border(img, color=ROSE, inner=WHITE, width=3):
    """The frame around every banner: a rose line, then a thin white one."""
    d = ImageDraw.Draw(img)
    w, h = img.size
    for i in range(width):
        d.rectangle([i, i, w - 1 - i, h - 1 - i], outline=color)
    d.rectangle([width, width, w - 1 - width, h - 1 - width], outline=inner)
    return img
