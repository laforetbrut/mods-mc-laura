"""Laura's default animations (assets/lauramod/animations/laura_humanoid.json).

Bedrock animation format, but values follow the vanilla ModelPart convention used by the default
model: degrees added to part rotations, pixels added to positions, Y pointing down.
Bones: head, body, right_arm, left_arm, right_leg, left_leg, root (whole model offset and tilt).
Arms: negative X raises the arm forward, positive Z moves the right arm outward (negative Z for the
left arm), negative Y turns the right arm toward the chest. Head: positive X looks down. Body:
positive X leans forward.

Author: vyrriox
"""

T = "query.anim_time"
L = "query.life_time"


def sin(freq, amp, phase=0, offset=0, time=T):
    """offset + sin(time * freq + phase) * amp, freq in degrees per second."""
    ph = f"+{phase}" if phase else ""
    off = f"{offset}+" if offset else ""
    return f"{off}math.sin({time}*{freq}{ph})*{amp}"


def kf(*frames):
    """Linear keyframes: (time, [x, y, z]) pairs."""
    out = {}
    for t, v in frames:
        out[f"{float(t):.2f}".rstrip("0").rstrip(".") if float(t) % 1 else f"{float(t):.1f}"] = v
    return out


def anim(length, loop, bones):
    a = {"animation_length": length, "bones": bones}
    if loop:
        a["loop"] = True
    return a


def pose(length, bones, ease_in=0.3, ease_out=0.35):
    """A held pose that eases in and out: bones = {bone: [x, y, z]} rotations."""
    out = {}
    for bone, rot in bones.items():
        channel = "position" if bone == "root" else "rotation"
        out.setdefault(bone, {})[channel] = kf((0, [0, 0, 0]), (ease_in, rot), (length - ease_out, rot), (length, [0, 0, 0]))
    return out


A = {}

# ------------------------------------------------------------------ looping states
A["idle"] = anim(4.0, True, {
    "body": {"rotation": [sin(90, 1.2, time=L), 0, 0]},
    "head": {"rotation": [sin(90, 2, 45, time=L), sin(40, 4, time=L), sin(55, 2, time=L)]},
    "right_arm": {"rotation": [sin(90, 2, time=L), 0, sin(90, 1.5, offset=2, time=L)]},
    "left_arm": {"rotation": [sin(90, 2, 180, time=L), 0, sin(90, -1.5, offset=-2, time=L)]},
})
A["walk"] = anim(1.0, True, {
    "root": {"position": [0, f"math.abs(math.sin({L}*360))*-0.6", 0]},
    "head": {"rotation": [0, 0, sin(360, 2, time=L)]},
    "body": {"rotation": [2, sin(360, 3, time=L), 0]},
})
A["happy"] = anim(2.0, True, {
    "root": {"position": [0, f"math.abs(math.sin({L}*180))*-0.8", 0]},
    "head": {"rotation": [-4, sin(90, 8, time=L), sin(180, 6, time=L)]},
    "body": {"rotation": [0, 0, sin(180, 2, time=L)]},
    "right_arm": {"rotation": [sin(180, 6, time=L), 0, sin(180, 4, offset=8, time=L)]},
    "left_arm": {"rotation": [sin(180, -6, time=L), 0, sin(180, 4, offset=-8, time=L)]},
})
A["sad"] = anim(4.0, True, {
    "head": {"rotation": [22, sin(45, 5, time=L), -4]},
    "body": {"rotation": [8, 0, 0]},
    "right_arm": {"rotation": [-8, 0, -4]},
    "left_arm": {"rotation": [-8, 0, 4]},
    "root": {"position": [0, 0.4, 0]},
})
A["angry"] = anim(3.0, True, {
    "right_arm": {"rotation": [-62, -38, 6]},
    "left_arm": {"rotation": [-70, 40, -6]},
    "head": {"rotation": [-6, sin(120, 10, time=L), sin(240, 3, time=L)]},
    "body": {"rotation": [-2, 0, 0]},
})
A["tired"] = anim(5.0, True, {
    "head": {"rotation": [f"14+math.sin({L}*72)*6", 0, sin(36, 5, time=L)]},
    "body": {"rotation": [6, 0, sin(36, 2, time=L)]},
    "right_arm": {"rotation": [4, 0, -2]},
    "left_arm": {"rotation": [4, 0, 2]},
    "root": {"position": [0, 0.5, 0]},
})
A["hungry"] = anim(3.0, True, {
    "right_arm": {"rotation": [-38, -30, 0]},
    "left_arm": {"rotation": [-38, 30, 0]},
    "head": {"rotation": [10, sin(60, 6, time=L), 0]},
    "body": {"rotation": [4, 0, 0]},
})
A["sit"] = anim(4.0, True, {
    "right_arm": {"rotation": [-28, -8, 0]},
    "left_arm": {"rotation": [-28, 8, 0]},
    "head": {"rotation": [sin(45, 3, time=L), sin(30, 10, time=L), 0]},
    "right_leg": {"rotation": [0, sin(60, 4, time=L), 0]},
    "left_leg": {"rotation": [0, sin(60, -4, time=L), 0]},
})
A["sleep"] = anim(4.0, True, {
    "body": {"rotation": [sin(90, 1.5, time=L), 0, 0]},
    "head": {"rotation": [0, 0, sin(90, 2, time=L)]},
})
A["swim"] = anim(1.2, True, {
    "right_arm": {"rotation": [f"-150+math.sin({L}*300)*30", 0, 12]},
    "left_arm": {"rotation": [f"-150+math.sin({L}*300+180)*30", 0, -12]},
    "right_leg": {"rotation": [sin(600, 25, time=L), 0, 0]},
    "left_leg": {"rotation": [sin(600, 25, 180, time=L), 0, 0]},
    "head": {"rotation": [-15, 0, 0]},
})
A["carry"] = anim(2.0, True, {
    "right_arm": {"rotation": [-48, -14, 0]},
    "left_arm": {"rotation": [-48, 14, 0]},
    "body": {"rotation": [-3, 0, 0]},
})
A["carry_walk"] = anim(1.0, True, {
    "right_arm": {"rotation": [f"-48+math.sin({L}*360)*4", -14, 0]},
    "left_arm": {"rotation": [f"-48+math.sin({L}*360)*4", 14, 0]},
    "root": {"position": [0, f"math.abs(math.sin({L}*360))*-0.5", 0]},
})
A["gagged"] = anim(3.0, True, {
    # Hay stuck on her mouth: she is not allowed to talk, so she sulks with her arms crossed and taps her foot.
    "right_arm": {"rotation": [-62, -38, 6]},
    "left_arm": {"rotation": [-70, 40, -6]},
    "head": {"rotation": [-8, f"18+math.sin({L}*60)*6", -6]},
    "right_leg": {"rotation": [f"math.max(0, math.sin({L}*240))*-14", 0, 0]},
})

# ------------------------------------------------------------------ emotes (one shot)
A["wave"] = anim(2.5, False, {
    "right_arm": {"rotation": [
        f"{T} < 0.25 ? -660*{T} : ({T} > 2.2 ? -165+({T}-2.2)*550 : -165)",
        0,
        f"{T} < 0.25 ? 80*{T} : ({T} > 2.2 ? 20-({T}-2.2)*66 : 20+math.sin(({T}-0.25)*720)*22)"]},
    "head": {"rotation": [0, -10, sin(360, 6)]},
})
A["hug"] = anim(3.0, False, {
    **pose(3.0, {"right_arm": [-80, -35, 0], "left_arm": [-80, 35, 0]}),
    "head": {"rotation": [8, 0, sin(120, 6)]},
    "body": {"rotation": [6, 0, sin(120, 3)]},
})
A["kiss"] = anim(2.5, False, pose(2.5, {
    "body": [12, 0, 0], "head": [-8, 0, 10], "right_arm": [20, 0, 30], "left_arm": [20, 0, -30], "right_leg": [35, 0, 0]}, 0.4, 0.6))
A["dance"] = anim(10.0, False, {
    "root": {"position": [f"math.sin({T}*180)*1.2", f"math.abs(math.sin({T}*360))*-1.2", 0]},
    "body": {"rotation": [0, sin(180, 18), sin(360, 5)]},
    "head": {"rotation": [sin(360, 6), sin(180, -12), sin(360, 8, 90)]},
    "right_arm": {"rotation": [f"-150+math.sin({T}*360)*25", 0, f"20+math.sin({T}*180)*25"]},
    "left_arm": {"rotation": [f"-150+math.sin({T}*360+180)*25", 0, f"-20+math.sin({T}*180)*25"]},
    "right_leg": {"rotation": [sin(360, 20), 0, sin(180, 6)]},
    "left_leg": {"rotation": [sin(360, 20, 180), 0, sin(180, 6)]},
})
# Hands meet at 0.3, 0.65, 1.0, 1.35 and 1.7 s, in time with the clap sounds (ticks 6, 13, 20, 27, 34).
clap_y = f"{T} < 1.9 ? -25-15*math.cos(({T}-0.3)*1028.57) : -25+({T}-1.9)*125"
clap_x = f"{T} < 1.9 ? -78 : -78+({T}-1.9)*390"
A["clap"] = anim(2.1, False, {
    "right_arm": {"rotation": [clap_x, clap_y, 0]},
    "left_arm": {"rotation": [clap_x, f"-({clap_y})", 0]},
    "root": {"position": [0, f"math.abs(math.sin({T}*514.29))*-0.6", 0]},
    "head": {"rotation": [-6, 0, sin(257, 4)]},
})
A["laugh"] = anim(2.5, False, {
    "body": {"rotation": [f"-6+math.sin({T}*1440)*3", 0, 0]},
    "head": {"rotation": [f"-14+math.sin({T}*1440)*4", 0, 0]},
    **{k: v for k, v in pose(2.5, {"right_arm": [-40, -35, 0], "left_arm": [-40, 35, 0]}, 0.2, 0.3).items()},
    "root": {"position": [0, f"math.abs(math.sin({T}*720))*-0.4", 0]},
})
A["cry"] = anim(4.0, False, {
    "head": {"rotation": [f"20+math.sin({T}*900)*3", 0, 0]},
    "body": {"rotation": [10, 0, 0]},
    "right_arm": {"rotation": [f"-135+math.sin({T}*900)*4", -28, 0]},
    "left_arm": {"rotation": [f"-135+math.sin({T}*900+60)*4", 28, 0]},
})
A["blush"] = anim(2.5, False, {
    **pose(2.5, {"head": [12, 20, 8], "right_arm": [-120, -30, 0], "left_arm": [-10, 0, -6]}, 0.25, 0.3),
    "body": {"rotation": [0, sin(360, 6), 0]},
})
A["facepalm"] = anim(2.0, False, pose(2.0, {"right_arm": [-140, -30, 0], "head": [22, 0, 0]}, 0.25, 0.3))
A["jump"] = anim(1.5, False, {
    "root": {"position": [0, f"{T} < 0.2 ? {T}*6 : ({T} < 0.9 ? -math.sin(({T}-0.2)*257)*9 : 0)", 0]},
    "right_arm": {"rotation": [f"{T} < 0.9 ? -170 : -170+({T}-0.9)*283", 0, 25]},
    "left_arm": {"rotation": [f"{T} < 0.9 ? -170 : -170+({T}-0.9)*283", 0, -25]},
    "right_leg": {"rotation": [f"{T} > 0.2 && {T} < 0.9 ? 30 : 0", 0, 0]},
    "left_leg": {"rotation": [f"{T} > 0.2 && {T} < 0.9 ? -20 : 0", 0, 0]},
})
A["bow"] = anim(2.0, False, pose(2.0, {
    "body": [28, 0, 0], "head": [20, 0, 0], "right_arm": [-10, 0, 25], "left_arm": [-10, 0, -25], "right_leg": [25, 0, 0],
    "root": [0, 1.5, 0]}, 0.5, 0.7))
A["think"] = anim(3.0, False, {
    **pose(3.0, {"right_arm": [-118, -34, 0], "left_arm": [-40, 38, 0]}),
    "head": {"rotation": [-12, sin(60, 10), 10]},
})
A["shrug"] = anim(1.5, False, pose(1.5, {
    "right_arm": [-30, 0, 45], "left_arm": [-30, 0, -45], "head": [0, 0, 14], "root": [0, -0.6, 0]}, 0.3, 0.4))
A["stomp"] = anim(2.0, False, {
    "right_leg": {"rotation": [f"math.max(0, math.sin({T}*540))*-40", 0, 0]},
    "left_leg": {"rotation": [f"math.max(0, math.sin({T}*540+180))*-40", 0, 0]},
    **pose(2.0, {"right_arm": [-10, 0, 22], "left_arm": [-10, 0, -22]}, 0.2, 0.3),
    "head": {"rotation": [-8, sin(540, 8), 0]},
})
A["yawn"] = anim(2.5, False, pose(2.5, {
    "right_arm": [-175, 0, 18], "left_arm": [-175, 0, -18], "head": [-24, 0, 0], "body": [-6, 0, 0]}, 0.6, 0.6))
A["eat"] = anim(1.6, False, {
    "right_arm": {"rotation": [f"-95+math.sin({T}*900)*10", -30, 0]},
    "head": {"rotation": [f"6+math.sin({T}*900)*5", 0, 0]},
})
A["poke"] = anim(1.0, False, {
    "right_arm": {"rotation": kf((0, [0, 0, 0]), (0.2, [-90, -10, 0]), (0.35, [-80, -10, 0]), (0.5, [-90, -10, 0]), (1.0, [0, 0, 0]))},
    "head": {"rotation": [0, 0, 8]},
})
A["slap"] = anim(0.75, False, {
    "right_arm": {"rotation": kf((0, [0, 0, 0]), (0.15, [-110, 40, 30]), (0.35, [-90, -50, 0]), (0.75, [0, 0, 0]))},
    "body": {"rotation": kf((0, [0, 0, 0]), (0.15, [0, 12, 0]), (0.35, [0, -15, 0]), (0.75, [0, 0, 0]))},
})
A["celebrate"] = anim(4.0, False, {
    "root": {"position": [0, f"math.abs(math.sin({T}*360))*-2.4", 0]},
    "right_arm": {"rotation": [f"-165+math.sin({T}*720)*12", 0, f"25+math.sin({T}*720)*10"]},
    "left_arm": {"rotation": [f"-165+math.sin({T}*720+180)*12", 0, f"-25-math.sin({T}*720)*10"]},
    "head": {"rotation": [-12, sin(360, 12), 0]},
    "right_leg": {"rotation": [sin(720, 12), 0, 4]},
    "left_leg": {"rotation": [sin(720, -12), 0, -4]},
})
# The snap sound plays at tick 11 (0.55 s).
A["snap"] = anim(1.7, False, {
    "right_arm": {"rotation": kf((0, [0, 0, 0]), (0.3, [-100, -20, 20]), (0.55, [-110, -10, 30]), (0.6, [-95, -25, 15]),
                                 (1.3, [-95, -25, 15]), (1.7, [0, 0, 0]))},
    "head": {"rotation": kf((0, [0, 0, 0]), (0.55, [-6, -15, 6]), (1.3, [-6, -15, 6]), (1.7, [0, 0, 0]))},
    "body": {"rotation": kf((0, [0, 0, 0]), (0.55, [0, -8, 0]), (1.3, [0, -8, 0]), (1.7, [0, 0, 0]))},
})
A["twirl"] = anim(1.7, False, {
    "right_arm": {"rotation": [-20, 0, f"math.sin({T}*106)*85"]},
    "left_arm": {"rotation": [-20, 0, f"-math.sin({T}*106)*85"]},
    "root": {"position": [0, f"math.sin({T}*106)*-1", 0]},
    "head": {"rotation": [-10, 0, 0]},
})
A["hum"] = anim(4.5, False, {
    "head": {"rotation": [-6, sin(80, 12), sin(160, 8)]},
    "body": {"rotation": [0, 0, sin(160, 4)]},
    "right_arm": {"rotation": [sin(160, 8, offset=-6), 0, sin(160, 6, offset=10)]},
    "left_arm": {"rotation": [sin(160, -8, offset=-6), 0, sin(160, 6, offset=-10)]},
    "root": {"position": [0, f"math.abs(math.sin({T}*160))*-0.5", 0]},
})
A["stretch"] = anim(2.8, False, {
    "right_arm": {"rotation": kf((0, [0, 0, 0]), (0.6, [-178, 0, 8]), (1.2, [-178, 0, 8]), (1.8, [-178, 0, 30]), (2.3, [-100, 0, 60]), (2.8, [0, 0, 0]))},
    "left_arm": {"rotation": kf((0, [0, 0, 0]), (0.6, [-178, 0, -8]), (1.2, [-178, 0, -8]), (1.8, [-178, 0, -30]), (2.3, [-100, 0, -60]), (2.8, [0, 0, 0]))},
    "body": {"rotation": kf((0, [0, 0, 0]), (0.6, [-8, 0, 0]), (1.2, [-4, 0, 10]), (1.8, [-4, 0, -10]), (2.8, [0, 0, 0]))},
    "root": {"position": kf((0, [0, 0, 0]), (0.6, [0, -1, 0]), (2.3, [0, -1, 0]), (2.8, [0, 0, 0]))},
})
A["tap_foot"] = anim(2.6, False, {
    "right_leg": {"rotation": [f"math.max(0, math.sin(({T}-0.1)*720))*-18", 0, 0]},
    **pose(2.6, {"right_arm": [-40, -40, 0], "left_arm": [-40, 40, 0]}, 0.25, 0.3),
    "head": {"rotation": [0, sin(90, 15), 6]},
})
A["blow_kiss"] = anim(1.7, False, {
    "right_arm": {"rotation": kf((0, [0, 0, 0]), (0.4, [-130, -40, 0]), (0.8, [-130, -40, 0]), (1.1, [-110, 20, 40]), (1.7, [0, 0, 0]))},
    "head": {"rotation": kf((0, [0, 0, 0]), (0.4, [8, 0, -8]), (1.1, [-8, 0, 6]), (1.7, [0, 0, 0]))},
})
A["sneeze"] = anim(1.6, False, {
    "head": {"rotation": kf((0, [0, 0, 0]), (0.6, [-22, 0, 0]), (0.8, [32, 0, 0]), (1.1, [10, 0, 0]), (1.6, [0, 0, 0]))},
    "body": {"rotation": kf((0, [0, 0, 0]), (0.6, [-6, 0, 0]), (0.8, [18, 0, 0]), (1.1, [4, 0, 0]), (1.6, [0, 0, 0]))},
    "right_arm": {"rotation": kf((0, [0, 0, 0]), (0.5, [-120, -35, 0]), (1.1, [-120, -35, 0]), (1.6, [0, 0, 0]))},
})
A["hair_flip"] = anim(1.4, False, {
    "head": {"rotation": kf((0, [0, 0, 0]), (0.3, [10, 20, 0]), (0.6, [-18, -25, -14]), (1.0, [-6, -10, -4]), (1.4, [0, 0, 0]))},
    "right_arm": {"rotation": kf((0, [0, 0, 0]), (0.3, [-150, -10, 10]), (0.6, [-160, 10, 40]), (1.4, [0, 0, 0]))},
    "body": {"rotation": kf((0, [0, 0, 0]), (0.6, [0, -10, 0]), (1.4, [0, 0, 0]))},
})
A["check_nails"] = anim(3.2, False, {
    **pose(3.2, {"right_arm": [-95, -25, 0], "head": [18, -12, 0]}),
    "left_arm": {"rotation": [f"-60+math.sin({T}*120)*6", 30, 0]},
})
A["air_guitar"] = anim(3.8, False, {
    **pose(3.8, {"left_arm": [-60, 35, -20]}, 0.25, 0.3),
    "right_arm": {"rotation": [f"-40+math.sin({T}*1440)*18", -40, 0]},
    "head": {"rotation": [f"8+math.sin({T}*720)*14", 0, sin(360, 8)]},
    "body": {"rotation": [sin(720, 4), 0, 0]},
    "root": {"position": [0, f"math.abs(math.sin({T}*720))*-0.8", 0]},
})
A["hiccup"] = anim(1.1, False, {
    "root": {"position": kf((0, [0, 0, 0]), (0.15, [0, -1.6, 0]), (0.35, [0, 0, 0]), (1.1, [0, 0, 0]))},
    "head": {"rotation": kf((0, [0, 0, 0]), (0.15, [-12, 0, 0]), (0.5, [4, 0, 0]), (1.1, [0, 0, 0]))},
    "right_arm": {"rotation": kf((0, [0, 0, 0]), (0.2, [-110, -30, 0]), (0.9, [-110, -30, 0]), (1.1, [0, 0, 0]))},
})
A["pout"] = anim(3.0, False, pose(3.0, {
    "right_arm": [-62, -38, 6], "left_arm": [-70, 40, -6], "head": [-10, 30, -8], "body": [0, 12, 0]}))
A["shiver"] = anim(2.6, False, {
    **pose(2.6, {"right_arm": [-60, -45, 0], "left_arm": [-60, 45, 0]}, 0.2, 0.3),
    "body": {"rotation": [6, 0, f"math.sin({T}*3600)*1.5"]},
    "head": {"rotation": [8, f"math.sin({T}*3600)*2", 0]},
    "root": {"position": [f"math.sin({T}*4000)*0.15", 0.3, 0]},
})
A["fan"] = anim(2.7, False, {
    "right_arm": {"rotation": [-120, f"-20+math.sin({T}*1080)*25", 0]},
    "head": {"rotation": [-10, 0, sin(180, 6)]},
    "left_arm": {"rotation": [-20, 0, -10]},
})


def build():
    return {"format_version": "1.8.0", "animations": {f"animation.laura.{k}": v for k, v in A.items()}}
