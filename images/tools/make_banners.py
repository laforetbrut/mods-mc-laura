"""Builds the animated banners of the project page (images/*.gif) from footage recorded in game.

    python images/tools/make_banners.py [names...]

One function per banner. This file is a starting point: keep the banners the mod needs, change the
clips, the crops and the boxes to match the footage, and write the overlays that tell its story.
Look at every banner with gifsheet.py before it ships, and at heat.py when one is too heavy.

Sizes: a clip is recorded at 1600 x 900; a banner is 800 pixels wide. A crop is given in pixels of
the recording, a box (the area that keeps moving) in pixels of the banner.

Author: vyrriox
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, HERE)

import kit  # noqa: E402
import pixelfont as font  # noqa: E402
from theme import Theme  # noqa: E402

# Where the capture tool wrote the frames: <project folder>/run-capture/screenshots. In a repository with
# one folder per target, that is for example os.path.join(ROOT, "neoforge-1.21.1", "run-capture", "screenshots").
SRC = os.path.join(ROOT, "run-capture", "screenshots")
OUT = os.path.dirname(HERE)             # images/, the folder this toolkit sits in
THEME = Theme.load(os.path.join(HERE, "theme.json"))


def src(name):
    return os.path.join(SRC, name)


def out(name):
    return os.path.join(OUT, name)


# ------------------------------------------------------------------ the banner on top of the page

def hero():
    # A still of the best view of the mod; the subject keeps moving inside `region`.
    kit.hero_banner(out("hero.gif"), THEME, src("hero_0005.png"), "MOD NAME", kicker=None, tagline="What it is, in five words",
                    logo=None, frames=kit.clip(SRC, "hero", 5, 72), region=(545, 28, 790, 330), crop=(0, 150, 1600, 750))


# ------------------------------------------------------------------ a feature shown by footage alone

def feature():
    # The plate is a still of the same view with nothing happening; only the boxes come from the clip.
    kit.clip_scene(out("feature.gif"), THEME, kit.clip(SRC, "feature", 1, 120), crop=(200, 185, 1600, 745), plate=src("feature_plate.png"),
                   boxes=(120, -40, 680, 360))


# ------------------------------------------------------------------ footage with an overlay that explains it

EVENTS = [44, 58, 72, 86, 100]          # frames of the clip where something happens (logged by `watch` in the capture script)
FIRST = 24                               # first frame of the clip that is shown


def machine():
    icon = kit.icon("cube", THEME, 2)

    def overlay(canvas, i, n):
        # A card that counts what the footage shows: it makes the point of the scene readable at a glance.
        done = sum(1 for e in EVENTS if i >= e - FIRST)
        last = max([e - FIRST for e in EVENTS if i >= e - FIRST], default=None)
        card = kit.panel(150, 60, THEME)
        bump = 1 + 0.3 * (1 - kit.span(i, last, last + 6)) if last is not None else 1.0
        kit.paste(card, icon, 34, 30, scale=bump, anchor="c")
        font.draw(card, f"x{done}", 62, 20, 3, THEME.ink)
        canvas.alpha_composite(card, (630, 20))

    kit.clip_scene(out("machine.gif"), THEME, kit.clip(SRC, "machine", FIRST, 136), crop=(200, 185, 1600, 745), plate=src("machine_0008.png"),
                   boxes=(30, -40, 600, 360), threshold=10, overlay=overlay)


# ------------------------------------------------------------------ a chat line or a command, then what it does

def command():
    for lang, prompt in (("en", "/mymod start"), ("fr", "/mymod start")):
        kit.chat_scene(out(f"command-{lang}.gif"), THEME, prompt, kit.clip(SRC, f"command_{lang}", 1, 96), crop=(155, 95, 1445, 675),
                       plate=src("command_plate.png"), boxes=[(250, -40, 550, 400)], watch=(330, 40, 470, 340))


# ------------------------------------------------------------------ a screen of the mod, visited by a pointer

def screen():
    tabs = ["main", "options", "about"]
    for lang in ("en", "fr"):
        kit.tour_scene(out(f"screen-{lang}.gif"), THEME, [src(f"screen_{lang}_{t}.png") for t in tabs],
                       targets=[(118, 118), (118, 164), (118, 210)], crop=(400, 210, 1200, 690), keep=(74, 17, 726, 463))


# ------------------------------------------------------------------ variants of one thing

def variants():
    names = ["red", "green", "blue"]

    def overlay(canvas, i, n, index, progress):
        kit.paste(canvas, kit.label(f"{index + 1}/{len(names)}", THEME, scale=3), 660, 30)

    kit.swap_scene(out("variants.gif"), THEME, [src(f"variant_{v}.png") for v in names], crop=(212, 205, 1388, 675), box=(296, 0, 504, 320),
                   overlay=overlay)


# ------------------------------------------------------------------ no footage: a still brought to life

def still():
    def overlay(canvas, i, n):
        chip = kit.label("A caption", THEME, scale=2, icon_name="star")
        kit.paste(canvas, chip, 400, 286 + kit.hop(i / n, 0.1), anchor="c")

    kit.still_scene(out("still.gif"), THEME, src("still.png"), crop=kit.crop_for((800, 320)), overlay=overlay)


# ------------------------------------------------------------------ no footage at all: the mod's own textures

TEXTURES = os.path.join(ROOT, "src", "main", "resources", "assets", "modid", "textures")


def tex(name):
    return os.path.join(TEXTURES, name + ".png")


def hero_plain():
    # The name across a background of the theme. A logo that is a square picture becomes the emblem.
    # The top banner is shared by every language of the page: no words on it but the name.
    kit.hero_banner(out("hero.gif"), THEME, kit.backdrop(THEME, (800, 300), "glow"), "MOD NAME", logo=None, side="center")


def showcase():
    # Items as they are, blocks drawn as blocks: (top,), (top, side) or (top, side, front).
    kit.showcase_scene(out("showcase.gif"), THEME, [(tex("block/machine_top"), tex("block/machine_side")), tex("item/gem"), tex("item/wrench")],
                       captions=["MACHINE", "GEM", "WRENCH"])


def build():
    # A small build and a JSON model, drawn from the assets the way the inventory shows a block.
    from iso import Assets, Scene

    scene = Scene(Assets(os.path.join(ROOT, "src", "main", "resources", "assets"), "modid"))
    scene.fill(0, 0, 0, 4, 0, 4, "block/floor")
    scene.model("block/machine", 2, 1, 2)
    picture = scene.render(scale=48)
    counts = [0, 12, 31, 64]                    # what the overlay tells, one value per quarter of the loop

    def overlay(canvas, i, n):
        kit.paste(canvas, picture.image, 250, 165, anchor="c")
        card = kit.panel(190, 64, THEME)
        kit.paste(card, kit.icon("cube", THEME, 2), 36, 32, anchor="c")
        font.draw(card, f"x{counts[i * len(counts) // n]}", 64, 22, 3, THEME.ink)
        canvas.alpha_composite(card, (500, 128))

    # The first frame must be a complete picture: it is what shows while the file loads.
    kit.still_scene(out("build.gif"), THEME, kit.backdrop(THEME, (800, 320), "glow", focus=(0.3, 0.55)), overlay=overlay)


BUILDERS = {f.__name__: f for f in (hero, feature, machine, command, screen, variants, still, hero_plain, showcase, build)}


def main():
    names = sys.argv[1:] or list(BUILDERS)
    for name in names:
        BUILDERS[name]()


if __name__ == "__main__":
    main()
