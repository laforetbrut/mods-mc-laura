"""Builds the branded top banner of the project page (images/hero.gif) from footage recorded in game.
The other banners (meet, work, outfits) are built by images/tools/laura/make_media.py.

Builds the animated banners of the project page (images/*.gif) from footage recorded in game.

    python images/tools/make_banners.py [names...]

One function per banner. This file is a starting point: keep the banners the mod needs, change the
clips, the crops and the boxes to match the footage, and write the overlays that tell its story.
Look at every banner with gifsheet.py before it ships, and at heat.py when one is too heavy.

Sizes: a clip is recorded at 1600 x 900; a banner is 800 pixels wide. A crop is given in pixels of
the recording, a box (the area that keeps moving) in pixels of the banner.

Author: vyrriox
"""
import glob
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, HERE)

import fx  # noqa: E402
import kit  # noqa: E402
import pixelfont as font  # noqa: E402
from theme import Theme  # noqa: E402

# Where the capture tool wrote the frames: <project folder>/run-capture/screenshots. In a repository with
# one folder per target, that is for example os.path.join(ROOT, "neoforge-1.21.1", "run-capture", "screenshots").
SRC = os.path.join(ROOT, "neoforge-1.21.1", "run-capture", "screenshots")
OUT = os.path.dirname(HERE)             # images/, the folder this toolkit sits in
THEME = Theme.load(os.path.join(HERE, "theme.json"))
# The logo of the brand kit for a dark plate (Branding/, made by the brand-logo skill), or None.
BRAND_LOGO = next(iter(sorted(glob.glob(os.path.join(ROOT, "Branding", "logo", "png", "*-horizontal-reverse-1024.png")))), None)


def src(name):
    return os.path.join(SRC, name)


def out(name):
    return os.path.join(OUT, name)


# ------------------------------------------------------------------ finishing with the ArtCraft suite

# Every banner below is finished by EffectCraft before it is encoded: a grade, a vignette and a few
# particles in the theme's colors ("cube" suits a Minecraft mod, "faded" is softer). Set it to None
# to encode the frames as drawn; without EffectCraft it does nothing. Particles weigh: watch the MB.
kit.LOOK = fx.look(THEME, vibrance=18, vignette=25, particles=8, particle_type="cube")


# ------------------------------------------------------------------ the banner on top of the page

def hero_branded():
    # The logo of Branding/ on the empty cherry grove of the hero shot, darkened so the logo carries the banner.
    # EffectCraft's own darkening is too light on this bright grove: the plate is dimmed and blurred first.
    from PIL import Image, ImageEnhance, ImageFilter
    plate = Image.open(src("hero_plate_empty.png")).convert("RGB").filter(ImageFilter.GaussianBlur(3))
    plate = ImageEnhance.Brightness(plate).enhance(0.42)
    plate_path = out("preview/hero-plate.png")
    os.makedirs(os.path.dirname(plate_path), exist_ok=True)
    plate.save(plate_path)
    frames = fx.branded_hero(THEME, plate_path, BRAND_LOGO, tagline="Your companion who shares your world",
                             particle_type="cube", darken=62)
    if not frames:
        raise SystemExit("EffectCraft is not available: the branded hero cannot be built")
    kit.finish([kit.frame_border(f, THEME) for f in frames], out("hero.gif"), look=False)


BUILDERS = {f.__name__: f for f in (hero_branded,)}

if __name__ == "__main__":
    for name in sys.argv[1:] or BUILDERS:
        BUILDERS[name]()
