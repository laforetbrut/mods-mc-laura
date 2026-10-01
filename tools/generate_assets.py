"""Generates the art and sound assets of My Girlfriend Laura.

Usage:  python tools/generate_assets.py [resources_dir] [--mc 1.21.1]

Writes skins, GUI icons, item and block textures, block models, the logo and the sound files
into the resources folder of a Minecraft version (default: common/1.21.1/src/main/resources).
Requires Pillow, numpy and ffmpeg (with libvorbis).

Author: vyrriox
"""
import argparse
import json
import os
import re
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, os.path.join(HERE, "assets"))

import icons  # noqa: E402
import skins  # noqa: E402
import sounds  # noqa: E402
import textures  # noqa: E402

OLD_SOUNDS = os.path.join(HERE, "assets", "voice")


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)


def icon_order(mc):
    src_path = os.path.join(ROOT, "common", mc, "src", "main", "java", "com", "vyrriox", "lauramod", "client", "gui", "kawaii", "Icon.java")
    with open(src_path, encoding="utf-8") as f:
        src = f.read()
    start = src.index("public enum Icon {") + len("public enum Icon {")
    body = re.sub(r"//[^\n]*", "", src[start:src.index(";", start)])
    return [t.strip() for t in body.split(",") if t.strip()]


def grave_models(assets):
    stone = "lauramod:block/laura_grave"
    front = "lauramod:block/laura_grave_front"
    base = "lauramod:block/laura_grave_base"

    def box(frm, to, north_texture="#stone", top_texture="#stone"):
        faces = {}
        for face in ("north", "south", "east", "west", "up", "down"):
            tex = north_texture if face == "north" else top_texture if face == "up" else "#stone"
            faces[face] = {"texture": tex}
            if face in ("down",):
                faces[face]["cullface"] = "down"
        return {"from": frm, "to": to, "faces": faces}

    model = {
        "parent": "minecraft:block/block",
        "textures": {"particle": stone, "stone": stone, "front": front, "base": base},
        "elements": [
            box([1, 0, 4], [15, 2, 12], top_texture="#base"),
            box([3, 2, 6], [13, 13, 10], north_texture="#front"),
            box([4, 13, 6], [12, 15, 10]),
            box([6, 15, 6], [10, 16, 10]),
        ],
    }
    # Front face UV: the whole engraved texture.
    model["elements"][1]["faces"]["north"]["uv"] = [3, 2, 13, 13]
    write_json(os.path.join(assets, "models", "block", "laura_grave.json"), model)
    write_json(os.path.join(assets, "models", "item", "laura_grave.json"), {"parent": "lauramod:block/laura_grave"})
    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
    write_json(os.path.join(assets, "blockstates", "laura_grave.json"), {
        "variants": {f"facing={k}": ({"model": "lauramod:block/laura_grave", "y": v} if v else {"model": "lauramod:block/laura_grave"})
                     for k, v in rotations.items()}})


def item_models(assets):
    write_json(os.path.join(assets, "models", "item", "laura_heart.json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": "lauramod:item/laura_heart"}})
    write_json(os.path.join(assets, "models", "item", "laura_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})


SUBTITLED = {
    "laura_ambient": 6, "laura_fart": 1, "laura_happy": 1, "laura_sad": 1, "laura_angry": 2,
}


def sound_files(assets):
    out = os.path.join(assets, "sounds")
    os.makedirs(out, exist_ok=True)
    for name in sorted(os.listdir(OLD_SOUNDS)):
        if name.endswith(".ogg"):
            shutil.copyfile(os.path.join(OLD_SOUNDS, name), os.path.join(out, name))
    made = sounds.generate(out)
    muffled = []
    for k in range(1, 4):
        stem = f"laura_muffled_{k}"
        sounds.muffle(os.path.join(OLD_SOUNDS, f"laura_ambient_{k}.ogg"), os.path.join(out, stem + ".ogg"))
        muffled.append(stem)
    events = {
        "laura_ambient": [f"laura_ambient_{k}" for k in range(1, 7)],
        "laura_fart": ["laura_fart_1"],
        "laura_happy": ["laura_happy_1", "laura_ambient_5"],
        "laura_sad": ["laura_sad_1"],
        "laura_angry": ["laura_angry_1", "laura_angry_2"],
        "laura_muffled": muffled,
    }
    events.update(made)
    data = {}
    for event, stems in events.items():
        data[event] = {
            "category": "neutral",
            "subtitle": "subtitles.lauramod." + event,
            "sounds": [{"name": "lauramod:" + s} for s in stems],
        }
    write_json(os.path.join(assets, "sounds.json"), data)
    return sorted(events)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("resources", nargs="?", default=None)
    parser.add_argument("--mc", default="1.21.1")
    parser.add_argument("--no-sounds", action="store_true")
    parser.add_argument("--preview", default=None, help="folder for preview images")
    args = parser.parse_args()
    resources = args.resources or os.path.join(ROOT, "common", args.mc, "src", "main", "resources")
    assets = os.path.join(resources, "assets", "lauramod")

    generated = {}
    for name, fn in skins.SKINS.items():
        img = fn()
        generated[name] = img
        save(img, os.path.join(assets, "textures", "entity", "laura", name + ".png"))
    save(textures.hay_gag(), os.path.join(assets, "textures", "entity", "hay_gag.png"))
    save(textures.thought_bubble(), os.path.join(assets, "textures", "entity", "thought_bubble.png"))
    atlas, missing = icons.atlas(icon_order(args.mc))
    save(atlas, os.path.join(assets, "textures", "gui", "kawaii_icons.png"))
    save(textures.laura_heart(), os.path.join(assets, "textures", "item", "laura_heart.png"))
    save(textures.stone(5), os.path.join(assets, "textures", "block", "laura_grave.png"))
    save(textures.stone(5, engraved=True), os.path.join(assets, "textures", "block", "laura_grave_front.png"))
    save(textures.grave_petals(), os.path.join(assets, "textures", "block", "laura_grave_base.png"))
    save(textures.logo(generated["laura"]), os.path.join(resources, "lauramod_logo.png"))
    grave_models(assets)
    item_models(assets)
    events = [] if args.no_sounds else sound_files(assets)

    if args.preview:
        os.makedirs(args.preview, exist_ok=True)
        from PIL import Image
        sheet = Image.new("RGBA", (6 * 136, 264), (255, 255, 255, 255))
        for i, img in enumerate(generated.values()):
            sheet.alpha_composite(skins.preview(img, 8), (i * 136 + 4, 4))
        sheet.save(os.path.join(args.preview, "skins.png"))
        atlas.resize((1024, 1024), Image.NEAREST).save(os.path.join(args.preview, "icons.png"))
    print("Assets written to", resources)
    print("Atlas icons drawn as items instead:", ", ".join(missing))
    if events:
        print("Sound events:", ", ".join(events))


if __name__ == "__main__":
    main()
