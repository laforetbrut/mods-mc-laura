"""Checks Laura's default animations against the body of the default model.

Usage:  python tools/check_animations.py [--mc 1.21.1] [--file <laura_humanoid.json>] [--verbose]

The default model is the vanilla player model: separate head, body, arms and legs, each turned
around its own pivot (rotation order Z, Y, X like ModelPart). This script plays every animation of
assets/lauramod/animations/laura_humanoid.json on that model, frame by frame, with the head turned a
little to each side as when she looks at you, and reports:

  - an arm that goes into her head (the classic raised arm whose Z rotation was meant to open it
    outward but closes it over the head once the arm is up); an arm that only brushes her chin
    (lowest 2.5 pixels of the front of the head), as when her hand comes to her mouth, is accepted,
  - an arm that goes deep into her torso,
  - a raised arm (hand above the shoulder) whose hand ends up on the inner side of the shoulder,
    which reads as an arm bent the wrong way.

Exit code 1 when anything is found. Plain Python, no dependency.

Author: vyrriox
"""
import argparse
import json
import math
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

# Vanilla player model, in pixels, Y down, front towards -Z (PlayerModel). Laura's skins use the
# slim arms by default; player skins may use the wide ones: both are checked.
HEAD_BOX = ((-4, 4), (-8, 0), (-4, 4))
BODY_BOX = ((-4, 4), (0, 12), (-2, 2))
MODELS = {
    "slim": {
        "right_arm": {"pivot": (-5, 2.5, 0), "box": ((-2, 1), (-2, 10), (-2, 2)), "side": -1},
        "left_arm": {"pivot": (5, 2.5, 0), "box": ((-1, 2), (-2, 10), (-2, 2)), "side": 1},
    },
    "wide": {
        "right_arm": {"pivot": (-5, 2, 0), "box": ((-3, 1), (-2, 10), (-2, 2)), "side": -1},
        "left_arm": {"pivot": (5, 2, 0), "box": ((-1, 3), (-2, 10), (-2, 2)), "side": 1},
    },
}
# Vanilla adds a small outward Z swing to idle arms (AnimationUtils.bobArms), about 3 degrees.
BOB = 2.9
HEAD_TOLERANCE = 0.5    # pixels of overlap with the head hidden by the hair layer (0.5 px thick)
JAW_TOLERANCE = 1.5     # a straight arm that brings the hand to the mouth brushes the chin
BODY_TOLERANCE = 1.0    # crossed arms brush the chest: only a deep overlap is reported
SHOULDER = 3.0          # the part of the arm closer than this to its pivot is the shoulder joint
LOOK_YAWS = (-15, 0, 15)   # head turned towards you; standing, her body soon turns to face you too
STEP = 0.04             # seconds between two checked frames


# ------------------------------------------------------------------ Molang subset
TOKEN = re.compile(r"\s*(\d+\.\d*|\.\d+|\d+|[A-Za-z_][A-Za-z_0-9.]*|&&|\|\||[<>=!]=|[-+*/(),?:<>!])")


def tokenize(text):
    pos, out = 0, []
    text = text.strip()
    while pos < len(text):
        m = TOKEN.match(text, pos)
        if not m:
            raise ValueError(f"cannot read Molang at {text[pos:]!r}")
        out.append(m.group(1))
        pos = m.end()
    return out


FUNCS = {
    "math.sin": lambda a: math.sin(math.radians(a)),
    "math.cos": lambda a: math.cos(math.radians(a)),
    "math.abs": abs,
    "math.max": max,
    "math.min": min,
    "math.sqrt": math.sqrt,
    "math.clamp": lambda v, lo, hi: max(lo, min(hi, v)),
    "math.lerp": lambda a, b, t: a + (b - a) * t,
}


class Expr:
    """Parses once, evaluates many times with query.anim_time and query.life_time."""

    def __init__(self, text):
        self.tokens = tokenize(text)
        self.i = 0
        self.tree = self.ternary()
        if self.i != len(self.tokens):
            raise ValueError(f"unexpected {self.tokens[self.i]!r} in {text!r}")

    def peek(self):
        return self.tokens[self.i] if self.i < len(self.tokens) else None

    def take(self, expected=None):
        tok = self.peek()
        if expected is not None and tok != expected:
            raise ValueError(f"expected {expected!r}, found {tok!r}")
        self.i += 1
        return tok

    def ternary(self):
        cond = self.binary(0)
        if self.peek() == "?":
            self.take()
            a = self.ternary()
            self.take(":")
            b = self.ternary()
            return ("?", cond, a, b)
        return cond

    LEVELS = [("||",), ("&&",), ("==", "!="), ("<", ">", "<=", ">="), ("+", "-"), ("*", "/")]

    def binary(self, level):
        if level == len(self.LEVELS):
            return self.unary()
        left = self.binary(level + 1)
        while self.peek() in self.LEVELS[level]:
            op = self.take()
            left = (op, left, self.binary(level + 1))
        return left

    def unary(self):
        if self.peek() in ("-", "!", "+"):
            op = self.take()
            return ("u" + op, self.unary())
        return self.primary()

    def primary(self):
        tok = self.take()
        if tok == "(":
            inner = self.ternary()
            self.take(")")
            return inner
        if tok[0].isdigit() or tok[0] == ".":
            return ("n", float(tok))
        name = tok.lower()
        if self.peek() == "(":
            self.take()
            args = []
            if self.peek() != ")":
                args.append(self.ternary())
                while self.peek() == ",":
                    self.take()
                    args.append(self.ternary())
            self.take(")")
            if name not in FUNCS:
                raise ValueError(f"unknown function {name}")
            return ("f", name, args)
        return ("v", name)

    def eval(self, env, node=None):
        node = self.tree if node is None else node
        kind = node[0]
        if kind == "n":
            return node[1]
        if kind == "v":
            return env.get(node[1].replace("q.", "query."), 0.0)
        if kind == "f":
            return float(FUNCS[node[1]](*[self.eval(env, a) for a in node[2]]))
        if kind == "?":
            return self.eval(env, node[2]) if self.eval(env, node[1]) != 0 else self.eval(env, node[3])
        if kind == "u-":
            return -self.eval(env, node[1])
        if kind == "u+":
            return self.eval(env, node[1])
        if kind == "u!":
            return 0.0 if self.eval(env, node[1]) != 0 else 1.0
        a, b = self.eval(env, node[1]), self.eval(env, node[2])
        return {
            "+": lambda: a + b, "-": lambda: a - b, "*": lambda: a * b, "/": lambda: a / b if b else 0.0,
            "<": lambda: float(a < b), ">": lambda: float(a > b), "<=": lambda: float(a <= b),
            ">=": lambda: float(a >= b), "==": lambda: float(a == b), "!=": lambda: float(a != b),
            "&&": lambda: float(a != 0 and b != 0), "||": lambda: float(a != 0 or b != 0),
        }[kind]()


def value(v):
    if isinstance(v, (int, float)):
        return lambda env, c=float(v): c
    expr = Expr(str(v))
    return expr.eval


def channel(data):
    """A channel as a function of time: a constant/Molang vector or linear keyframes."""
    if isinstance(data, list):
        comps = [value(c) for c in data]
        return lambda t, env: [c(env) for c in comps]
    keys = sorted((float(k), [value(c) for c in (v if isinstance(v, list) else v.get("post", v.get("pre")))])
                  for k, v in data.items())

    def sample(t, env):
        if t <= keys[0][0]:
            return [c(env) for c in keys[0][1]]
        if t >= keys[-1][0]:
            return [c(env) for c in keys[-1][1]]
        for (t0, a), (t1, b) in zip(keys, keys[1:]):
            if t0 <= t <= t1:
                f = (t - t0) / max(1e-4, t1 - t0)
                return [ca(env) + (cb(env) - ca(env)) * f for ca, cb in zip(a, b)]
        return [0.0, 0.0, 0.0]
    return sample


# ------------------------------------------------------------------ geometry
def rot_x(v, a):
    c, s = math.cos(a), math.sin(a)
    return (v[0], v[1] * c - v[2] * s, v[1] * s + v[2] * c)


def rot_y(v, a):
    c, s = math.cos(a), math.sin(a)
    return (v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c)


def rot_z(v, a):
    c, s = math.cos(a), math.sin(a)
    return (v[0] * c - v[1] * s, v[0] * s + v[1] * c, v[2])


def turn(v, rot):
    """ModelPart order: X first, then Y, then Z (quaternion rotationZYX)."""
    x, y, z = (math.radians(r) for r in rot)
    return rot_z(rot_y(rot_x(v, x), y), z)


def unturn(v, rot):
    x, y, z = (math.radians(r) for r in rot)
    return rot_x(rot_y(rot_z(v, -z), -y), -x)


def add(a, b):
    return tuple(p + q for p, q in zip(a, b))


def sub(a, b):
    return tuple(p - q for p, q in zip(a, b))


def depth_inside(p, box):
    """How deep a point is inside a box (pixels), 0 when outside."""
    d = min(min(p[i] - box[i][0], box[i][1] - p[i]) for i in range(3))
    return max(0.0, d)


def head_overlap(p):
    """Overlap of a point with the head that shows. Up to 1.5 pixels in the chin (lowest 2.5
    pixels of the front 3 pixels of the head) is allowed: an arm that brings the hand to her mouth
    brushes it, and from any side it reads as the arm touching the chin, not as an arm through the
    face."""
    d = depth_inside(p, HEAD_BOX)
    if d <= 0:
        return 0.0
    under = HEAD_BOX[1][1] - p[1]
    behind_face = p[2] - HEAD_BOX[2][0]
    if under <= 2.5 and behind_face <= 3.0 and d <= JAW_TOLERANCE:
        return 0.0
    return d


def arm_points(box):
    """Points on the surface and inside of an arm box, with their distance from the pivot along the arm."""
    (x0, x1), (y0, y1), (z0, z1) = box
    pts = []
    for i in range(3):
        for j in range(13):
            for k in range(3):
                pts.append((x0 + (x1 - x0) * i / 2, y0 + (y1 - y0) * j / 12, z0 + (z1 - z0) * k / 2))
    return pts


def check_pose(pose, look_yaw, model="slim"):
    """Problems of one frame: list of (kind, bone, depth)."""
    head_rot = add(pose.get("head", (0, 0, 0)), (0, look_yaw, 0))
    body_rot = pose.get("body", (0, 0, 0))
    found = []
    for name, spec in MODELS[model].items():
        rot = pose.get(name, (0, 0, 0))
        rot = (rot[0], rot[1], rot[2] - spec["side"] * BOB)
        head_depth = body_depth = 0.0
        for p in arm_points(spec["box"]):
            if p[1] < SHOULDER:
                continue
            world = add(spec["pivot"], turn(p, rot))
            head_depth = max(head_depth, head_overlap(unturn(world, head_rot)))
            body_depth = max(body_depth, depth_inside(unturn(world, body_rot), BODY_BOX))
        if head_depth > HEAD_TOLERANCE:
            found.append(("head", name, head_depth))
        if body_depth > BODY_TOLERANCE:
            found.append(("body", name, body_depth))
        hand = add(spec["pivot"], turn((spec["box"][0][0] / 2 + spec["box"][0][1] / 2, 9, 0), rot))
        if hand[1] < -6 and hand[2] > -6 and hand[0] * spec["side"] < 1.0:
            # Hand above her head and past its middle: the raised arm closes over her head.
            found.append(("inward", name, 1.0 - hand[0] * spec["side"]))
    return found


def load_animations(path):
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    out = {}
    for full, anim in data["animations"].items():
        name = full.rsplit(".", 1)[-1]
        bones = {}
        for bone, chans in anim.get("bones", {}).items():
            if "rotation" in chans:
                bones[bone] = channel(chans["rotation"])
        out[name] = (float(anim.get("animation_length", 1.0)), bones)
    return out


def check(path, verbose=False):
    problems = {}
    for name, (length, bones) in load_animations(path).items():
        worst = {}
        steps = max(20, int(length / STEP))
        for s in range(steps + 1):
            t = length * s / steps
            env = {"query.anim_time": t, "query.life_time": t}
            pose = {bone: tuple(fn(t, env)) for bone, fn in bones.items()}
            for yaw, model in ((y, m) for y in LOOK_YAWS for m in MODELS):
                for kind, bone, depth in check_pose(pose, yaw, model):
                    key = (kind, bone, model)
                    if depth > worst.get(key, (0, 0))[0]:
                        worst[key] = (depth, t)
        if worst:
            problems[name] = worst
        elif verbose:
            print(f"  ok  {name}")
    return problems


LABEL = {"head": "arm inside the head", "body": "arm deep in the torso", "inward": "raised arm closes over the head"}


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--mc", default=None, help="1.20.1, 1.21.1 or 26.1.2 (default: every version)")
    parser.add_argument("--file", default=None, help="an animation file to check instead")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()
    if args.file:
        files = [args.file]
    else:
        versions = [args.mc] if args.mc else sorted(os.listdir(os.path.join(ROOT, "common")))
        files = [os.path.join(ROOT, "common", v, "src", "main", "resources", "assets", "lauramod", "animations",
                              "laura_humanoid.json") for v in versions]
    bad = 0
    for path in files:
        problems = check(path, args.verbose)
        count = len(load_animations(path))
        print(f"{os.path.relpath(path, ROOT)}: {count} animations checked, {len(problems)} with problems")
        for name, worst in sorted(problems.items()):
            for (kind, bone, model), (depth, t) in sorted(worst.items()):
                print(f"  {name:12s} {bone:10s} {model:5s} {LABEL[kind]:32s} {depth:5.2f} px at {t:.2f} s")
        bad += len(problems)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
