"""Checks that every translation key and dialogue key used by the code exists.

Usage:  python tools/check_lang.py [--mc 1.21.1] [--strict]

For each assets/lauramod/lang/<locale>.json and data/lauramod/dialogues/<locale>.json it lists the
missing keys (English is the reference and must be complete), the unused keys, and placeholders
that differ from English. Exit code 1 when English is incomplete (or any file with --strict).

Author: vyrriox
"""
import argparse
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)


def enum_constants(java_file, enum_name=None):
    src = open(java_file, encoding="utf-8").read()
    if enum_name:
        start = src.index("enum " + enum_name)
    else:
        start = src.index("enum ")
    body_start = src.index("{", start) + 1
    body = src[body_start:]
    # Constants end at the first ';' at depth 0 (or the closing brace for simple enums).
    depth = 0
    out = []
    token = ""
    for ch in body:
        if ch in "({":
            depth += 1
        elif ch in ")}":
            if depth == 0:
                break
            depth -= 1
        if depth == 0 and ch == ";":
            break
        token += ch
    token = re.sub(r"/\*.*?\*/", "", token, flags=re.S)
    token = re.sub(r"//[^\n]*", "", token)
    token = re.sub(r"\([^()]*\)", "", token)
    for part in token.split(","):
        name = part.strip()
        if re.fullmatch(r"[A-Z][A-Z0-9_]*", name):
            out.append(name.lower())
    return out


def code_keys(java_root):
    literal = set()
    for root, _, files in os.walk(java_root):
        for f in files:
            if f.endswith(".java"):
                src = open(os.path.join(root, f), encoding="utf-8").read()
                for m in re.finditer(r'"((?:lauramod|key|item|block|entity|itemGroup|subtitles|advancements)\.[a-zA-Z0-9_.]*[a-zA-Z0-9_])"', src):
                    literal.add(m.group(1))
    # JVM system properties, not translation keys.
    literal -= {"lauramod.selftest", "lauramod.selftest.only", "lauramod.clienttest"}
    return literal


def advancement_dir(pkg):
    """The folder of her advancements. Minecraft renamed the data folders to the singular in 1.21:
    1.20.1 has data/lauramod/advancements, 1.21.1 and later have data/lauramod/advancement."""
    data = os.path.normpath(os.path.join(pkg, "..", "..", "..", "..", "resources", "data", "lauramod"))
    candidates = [os.path.join(data, name, "laura") for name in ("advancement", "advancements")]
    for candidate in candidates:
        if os.path.isdir(candidate):
            return candidate
    sys.exit("No advancement folder found, looked for: " + ", ".join(candidates))


def families(pkg):
    e = lambda rel, name=None: enum_constants(os.path.join(pkg, rel), name)
    keys = set()
    moods = e("entity/Mood.java")
    for m in moods:
        keys.add("lauramod.mood." + m)
    for m in e("entity/LauraMode.java"):
        keys.add("lauramod.mode." + m)
    for c in e("entity/CombatMode.java"):
        keys |= {"lauramod.combat." + c, "lauramod.combat." + c + ".help"}
    for n in e("entity/brain/Needs.java", "Need"):
        keys.add("lauramod.need." + n)
    for j in e("entity/LauraJob.java"):
        keys.add("lauramod.job." + j)
        if j != "none":
            keys.add("lauramod.job." + j + ".help")
    for c in e("entity/work/ChestPurpose.java"):
        keys |= {"lauramod.chest." + c, "lauramod.chest." + c + ".help"}
    for t in e("entity/work/LauraTask.java", "Type"):
        keys.add("lauramod.task." + t)
    for em in e("entity/Emote.java"):
        keys.add("lauramod.emote." + em)
    for a in e("desire/DesireType.java", "Activity"):
        keys.add("lauramod.activity." + a)
    for r in ("hate", "cold", "friend", "close", "love", "soulmate"):
        keys.add("lauramod.relation." + r)
    for a in e("config/LauraConfig.java", "Annoyance"):
        keys.add("lauramod.annoyance." + a)
    for t in ("home", "orders", "emotes", "work", "fetch", "style", "settings"):
        keys.add("lauramod.menu.tab." + t)
    for p in ("jobs", "errands", "chests"):
        keys.add("lauramod.menu.work." + p)
    for p in ("skins", "models"):
        keys.add("lauramod.menu.style." + p)
    for c in ("speechBubbles", "thoughtBubbles", "animations", "particles", "customModels", "remoteSkins", "menuOnRightClick", "showNeedsHud"):
        keys |= {"lauramod.client." + c, "lauramod.client." + c + ".help"}
    for h in ("summon", "orders", "home", "fetch", "tasks", "jobs", "chests", "emotes", "look", "info", "chat"):
        keys.add("lauramod.help." + h)
    for s in ("laura", "laura_summer", "laura_winter", "laura_night", "laura_sporty", "laura_gothic"):
        keys.add("lauramod.skin.builtin." + s)
    places = re.findall(r'\{ "id": "([a-z_]+)", "biomes"', open(os.path.join(pkg, "desire/DesireTable.java"), encoding="utf-8").read())
    for p in places:
        keys.add("lauramod.place." + p)
    sounds = enum_constants(os.path.join(pkg, "registry/LauraRegistries.java"), "Sound")
    for s in sounds:
        keys.add("subtitles.lauramod.laura_" + s)
    for adv in sorted(os.listdir(advancement_dir(pkg))):
        if adv.endswith(".json"):
            key = adv[:-5]
            keys |= {f"advancements.lauramod.{key}.title", f"advancements.lauramod.{key}.description"}
    keys |= {"entity.lauramod.laura", "item.lauramod.laura_spawn_egg", "block.lauramod.laura_grave", "itemGroup.lauramod"}
    return keys, moods


def dialogue_keys(pkg, moods):
    """Line keys the code asks for (the specific variants are optional)."""
    required = set()
    optional = set()
    for root, _, files in os.walk(pkg):
        for f in files:
            if not f.endswith(".java"):
                continue
            src = open(os.path.join(root, f), encoding="utf-8").read()
            for m in re.finditer(r'(?:say|sayFirst|sayToOwner|sayFirstToOwner)\((.*?)\);', src, flags=re.S):
                for k in re.findall(r'"([a-z_]+(?:\.[a-z_]+)*)"', m.group(1)):
                    if k in ("item", "count", "task", "place", "activity", "hours", "attacker", "victim", "radius", "purpose", "name",
                             "other", "days", "minutes", "laura", "player") or k.startswith("lauramod"):
                        continue
                    required.add(k)
            for k in re.findall(r'"((?:work|fetch)\.[a-z_]+(?:\.[a-z_]+)*)"', src):
                required.add(k)
    for need in ("hunger", "energy", "fun", "attention", "hygiene"):
        required |= {f"need.{need}.low", f"need.{need}.critical"}
    for tier in ("gross", "meh", "nice", "great", "amazing"):
        required.add("gift." + tier)
    for kind in ("item", "place", "activity"):
        required.add("desire.new." + kind)
        optional.add("desire.fulfilled." + kind)
    required |= {"desire.remind", "desire.remind.urgent"}
    for c in ("passive", "defensive", "aggressive"):
        required.add("combat." + c)
    for j in ("lumberjack", "farmer", "cook"):
        required.add("job.start." + j)
    for t in ("chop_tree", "harvest", "cook"):
        required.add("task.start." + t)
    for d in ("nether", "end", "overworld"):
        required.add("dimension." + d)
    for k in ("monster", "animal", "player", "generic"):
        required.add("owner.killed." + k)
    for k in ("diamond", "ore", "wood", "flower", "generic"):
        optional.add("owner.mined." + k)
    required.add("owner.mined.generic")
    for m in moods:
        optional |= {"status." + m, "ambient." + m, "order.refused." + m}
    for r in ("hate", "cold", "friend", "close", "love", "soulmate"):
        optional.add("ambient." + r)
    optional |= {"ambient.day", "ambient.night", "ambient.rain"}
    # Keys built by concatenation that the regex caught as prefixes.
    required = {k for k in required if not k.endswith(".")}
    for prefix in ("status", "ambient", "order.refused", "desire.fulfilled", "owner.found", "gift"):
        pass
    required |= {"ambient", "status", "desire.fulfilled", "owner.found.generic"}
    required -= {"need", "combat", "job.start", "task.start", "gift", "order.refused", "status", "ambient"} - {"status", "ambient"}
    return required, optional


INTENTS_REQUIRED = [
    "summon", "yes", "no", "where", "follow", "stay", "come", "home", "wander", "set_home", "stop", "fetch", "chop_tree", "harvest", "cook",
    "job_lumberjack", "job_farmer", "job_cook", "job_stop", "back_to_work", "assign_chest", "hug", "kiss", "apology", "eat", "sleep",
    "wake_up", "beautiful", "love_you", "miss_you", "thanks", "insult", "what_do_you_want", "how_are_you", "player_hungry",
]


def placeholders(text):
    return sorted(set(re.findall(r"%s|%\d+\$s", text)))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mc", default="1.21.1")
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()
    base = os.path.join(ROOT, "common", args.mc, "src", "main")
    if not os.path.isdir(base):
        versions = sorted(os.listdir(os.path.join(ROOT, "common"))) if os.path.isdir(os.path.join(ROOT, "common")) else []
        sys.exit(f"No common/{args.mc} folder. Known versions: {', '.join(versions) or 'none'}")
    pkg = os.path.join(base, "java", "com", "vyrriox", "lauramod")
    lang_dir = os.path.join(base, "resources", "assets", "lauramod", "lang")
    dia_dir = os.path.join(base, "resources", "data", "lauramod", "dialogues")

    fam, moods = families(pkg)
    keys = code_keys(os.path.join(base, "java")) | fam
    keys = {k for k in keys if not k.endswith(".")}
    ok = True

    reference = json.load(open(os.path.join(lang_dir, "en_us.json"), encoding="utf-8"))
    for name in sorted(os.listdir(lang_dir)):
        if not name.endswith(".json"):
            continue
        data = json.load(open(os.path.join(lang_dir, name), encoding="utf-8"))
        missing = sorted(keys - set(data))
        # Tag names are read by the game and by recipe viewers, never by the mod's code.
        unused = sorted(k for k in set(data) - keys if not k.startswith("tag."))
        bad =[k for k in data if k in reference and placeholders(data[k]) != placeholders(reference[k])]
        status = "OK" if not missing and not bad else "INCOMPLETE"
        print(f"[lang] {name}: {len(data)} keys, {len(missing)} missing, {len(unused)} unused, {len(bad)} placeholder mismatches -> {status}")
        for k in missing[:200]:
            print("    missing:", k)
        for k in bad:
            print("    placeholders differ:", k)
        if name == "en_us.json" and (missing or bad):
            ok = False
        if args.strict and (missing or bad):
            ok = False
        if name == "en_us.json":
            for k in unused:
                print("    unused:", k)

    required, optional = dialogue_keys(pkg, moods)
    for name in sorted(os.listdir(dia_dir)):
        if not name.endswith(".json") or name.startswith("_"):
            continue
        data = json.load(open(os.path.join(dia_dir, name), encoding="utf-8"))
        lines = data.get("lines", {})
        intents = data.get("intents", {})
        missing = sorted(required - set(lines))
        missing_intents = [i for i in INTENTS_REQUIRED if i not in intents or not intents[i].get("triggers")]
        empty = [k for k, v in lines.items() if not v]
        status = "OK" if not missing and not missing_intents else "INCOMPLETE"
        print(f"[dialogue] {name}: {len(lines)} line keys, {len(intents)} intents, {len(missing)} missing lines, {len(missing_intents)} missing intents -> {status}")
        for k in missing:
            print("    missing line:", k)
        for k in missing_intents:
            print("    missing intent:", k)
        for k in empty:
            print("    empty:", k)
        if name == "en_us.json" and (missing or missing_intents):
            ok = False
        if args.strict and (missing or missing_intents):
            ok = False

    # The mod only loads the dialogue languages listed in _index.json.
    files = sorted(n[:-5] for n in os.listdir(dia_dir) if n.endswith(".json") and not n.startswith("_"))
    index = json.load(open(os.path.join(dia_dir, "_index.json"), encoding="utf-8")).get("languages", [])
    not_indexed = [f for f in files if f not in index]
    no_file = [i for i in index if i not in files]
    print(f"[index] {len(index)} languages listed, {len(not_indexed)} files not listed, {len(no_file)} listed without a file")
    for f in not_indexed:
        print("    not listed in _index.json:", f)
    for f in no_file:
        print("    listed but missing:", f)
    if not_indexed or no_file:
        ok = False
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
