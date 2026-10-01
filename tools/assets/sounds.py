"""Synthesized sound effects for Laura: claps, snaps, taps, a tiny babble voice, sighs, sparkles...

Everything is generated from scratch (noise, oscillators and formant filters), so the files carry
no third party rights. Output is mono (positional in game) Ogg Vorbis through ffmpeg.

Author: vyrriox
"""
import math
import os
import subprocess
import tempfile
import wave

import numpy as np

RATE = 44100


def envelope(n, attack, decay_tau, rate=RATE):
    t = np.arange(n) / rate
    env = np.exp(-t / decay_tau)
    a = max(1, int(attack * rate))
    env[:a] *= np.linspace(0, 1, a)
    return env


def one_pole_lowpass(x, cutoff, rate=RATE):
    """Cutoff can be a number or an array (time-varying)."""
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    y = np.zeros_like(x)
    prev = 0.0
    for i in range(len(x)):
        a = 1.0 - math.exp(-2 * math.pi * cutoff[i] / rate)
        prev += a * (x[i] - prev)
        y[i] = prev
    return y


def highpass(x, cutoff, rate=RATE):
    return x - one_pole_lowpass(x, cutoff, rate)


def resonator(x, freq, bandwidth, rate=RATE):
    """Two-pole resonator (formant filter). freq may vary over time."""
    freq = np.broadcast_to(np.asarray(freq, dtype=float), x.shape)
    y = np.zeros_like(x)
    y1 = y2 = 0.0
    r = math.exp(-math.pi * bandwidth / rate)
    gain = 1 - r
    for i in range(len(x)):
        c = 2 * r * math.cos(2 * math.pi * freq[i] / rate)
        yi = gain * x[i] + c * y1 - r * r * y2
        y2, y1 = y1, yi
        y[i] = yi
    return y


def glottal(f0, rate=RATE, jitter=0.004, seed=0):
    """A soft glottal pulse train following the pitch curve f0 (array)."""
    rng = np.random.default_rng(seed)
    f = f0 * (1 + rng.normal(0, jitter, len(f0)))
    phase = np.cumsum(f / rate) % 1.0
    # Rosenberg-like pulse: rising sine, quick fall, closed phase.
    open_q = 0.6
    pulse = np.where(phase < open_q * 0.66, 0.5 * (1 - np.cos(np.pi * phase / (open_q * 0.66))),
                     np.where(phase < open_q, np.cos(0.5 * np.pi * (phase - open_q * 0.66) / (open_q * 0.34)), 0.0))
    return np.diff(pulse, prepend=0.0) * 40


VOWELS = {
    "a": (800, 1250, 2800),
    "e": (500, 1950, 2700),
    "i": (320, 2500, 3100),
    "o": (500, 900, 2600),
    "u": (350, 800, 2500),
    "m": (260, 1100, 2400),
}


def voice(f0, vowel_track, rate=RATE, breath=0.08, seed=0):
    """Formant voice. vowel_track: list of (start_fraction, vowel) key points."""
    n = len(f0)
    src = glottal(f0, rate, seed=seed)
    rng = np.random.default_rng(seed + 1)
    src = src + rng.normal(0, breath, n)
    formants = [np.zeros(n) for _ in range(3)]
    keys = sorted(vowel_track)
    for k in range(3):
        xs = [int(p * (n - 1)) for p, _ in keys]
        ys = [VOWELS[v][k] for _, v in keys]
        formants[k] = np.interp(np.arange(n), xs, ys)
    out = (resonator(src, formants[0], 90, rate) * 1.0 + resonator(src, formants[1], 120, rate) * 0.55
           + resonator(src, formants[2], 180, rate) * 0.3)
    return out


def normalize(x, peak=0.85):
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak


def fade(x, ms=6, rate=RATE):
    n = min(len(x) // 2, int(rate * ms / 1000))
    if n > 0:
        x[:n] *= np.linspace(0, 1, n)
        x[-n:] *= np.linspace(1, 0, n)
    return x


# ------------------------------------------------------------------ effects

def clap(seed=0):
    rng = np.random.default_rng(seed)
    n = int(0.28 * RATE)
    out = np.zeros(n)
    for k, delay in enumerate((0.0, 0.009, 0.017)):
        start = int(delay * RATE)
        burst = rng.normal(0, 1, n - start) * envelope(n - start, 0.0005, 0.011 if k < 2 else 0.05)
        out[start:] += burst * (0.8 if k < 2 else 1.0)
    out = highpass(out, 700)
    out = resonator(out, 1400 + 200 * rng.random(), 900) * 0.7 + out * 0.3
    return fade(normalize(out))


def snap(seed=0):
    rng = np.random.default_rng(seed)
    n = int(0.12 * RATE)
    click = rng.normal(0, 1, n) * envelope(n, 0.0002, 0.006)
    click = highpass(click, 2000)
    tone = np.sin(2 * np.pi * 2600 * np.arange(n) / RATE) * envelope(n, 0.0002, 0.01) * 0.5
    return fade(normalize(click + tone))


def tap(seed=0):
    rng = np.random.default_rng(seed)
    n = int(0.16 * RATE)
    t = np.arange(n) / RATE
    thud = np.sin(2 * np.pi * (110 + 60 * np.exp(-t / 0.01)) * t) * envelope(n, 0.001, 0.035)
    click = one_pole_lowpass(rng.normal(0, 1, n) * envelope(n, 0.0003, 0.004), 1800) * 0.8
    return fade(normalize(thud + click))


def whoosh(seed=0):
    rng = np.random.default_rng(seed)
    n = int(0.45 * RATE)
    t = np.linspace(0, 1, n)
    env = np.sin(np.pi * t) ** 2
    cutoff = 400 + 3200 * np.sin(np.pi * t)
    noise = one_pole_lowpass(rng.normal(0, 1, n), cutoff)
    noise = one_pole_lowpass(noise, cutoff)
    return fade(normalize(noise * env, 0.7))


def kiss(seed=0):
    rng = np.random.default_rng(seed)
    n = int(0.22 * RATE)
    t = np.arange(n) / RATE
    sweep = np.sin(2 * np.pi * np.cumsum(1800 * np.exp(-t / 0.02) + 300) / RATE) * envelope(n, 0.001, 0.02)
    pop = highpass(rng.normal(0, 1, n) * envelope(n, 0.0003, 0.008), 900) * 0.6
    tail = voice(np.full(n, 420.0), [(0, "u"), (1, "a")], seed=seed) * envelope(n, 0.02, 0.05) * 0.25
    return fade(normalize(sweep + pop + tail))


def hiccup(seed=0):
    rng = np.random.default_rng(seed)
    n = int(0.16 * RATE)
    f0 = np.linspace(520, 440, n)
    v = voice(f0, [(0, "i"), (1, "i")], breath=0.2, seed=seed)
    env = envelope(n, 0.003, 0.05)
    v *= env
    catch = highpass(rng.normal(0, 1, n) * envelope(n, 0.0005, 0.01), 1500) * 0.3
    return fade(normalize(v + catch))


def chatter(seed=0):
    """A cute babble of little syllables (like a character talking in a game)."""
    rng = np.random.default_rng(seed)
    parts = []
    for k in range(int(rng.integers(5, 8))):
        dur = rng.uniform(0.06, 0.11)
        n = int(dur * RATE)
        base = rng.uniform(300, 380)
        f0 = np.linspace(base * rng.uniform(0.95, 1.08), base * rng.uniform(0.88, 1.12), n)
        v1, v2 = rng.choice(list("aeiou")), rng.choice(list("aeiou"))
        syl = voice(f0, [(0, v1), (1, v2)], breath=0.05, seed=seed + k) * envelope(n, 0.008, dur * 0.6)
        parts.append(fade(syl, 4))
        parts.append(np.zeros(int(rng.uniform(0.015, 0.04) * RATE)))
    return fade(normalize(np.concatenate(parts), 0.75))


def hmph(seed=0):
    n = int(0.42 * RATE)
    t = np.linspace(0, 1, n)
    f0 = 250 - 70 * t
    v = voice(f0, [(0, "m"), (1, "m")], breath=0.05, seed=seed)
    env = np.minimum(1, t * 12) * np.exp(-t * 3.2)
    rng = np.random.default_rng(seed)
    puff = one_pole_lowpass(rng.normal(0, 1, n), 1200) * np.exp(-((t - 0.62) ** 2) / 0.004) * 0.8
    return fade(normalize(v * env + puff))


def sigh(seed=0):
    rng = np.random.default_rng(seed)
    n = int(1.1 * RATE)
    t = np.linspace(0, 1, n)
    breath = rng.normal(0, 1, n)
    shaped = resonator(breath, 800, 300) + resonator(breath, 1250, 400) * 0.6 + resonator(breath, 2600, 600) * 0.2
    env = np.minimum(1, t * 5) * np.exp(-np.maximum(0, t - 0.2) * 3)
    voiced = voice(260 - 60 * t, [(0, "a"), (1, "o")], breath=0.2, seed=seed) * np.exp(-t * 6) * 0.35
    return fade(normalize(shaped * env + voiced, 0.7))


def sparkle(seed=0):
    n = int(0.9 * RATE)
    t = np.arange(n) / RATE
    out = np.zeros(n)
    for k, midi in enumerate((84, 88, 91, 96)):
        start = int(k * 0.07 * RATE)
        f = 440 * 2 ** ((midi - 69) / 12)
        tt = t[: n - start]
        tone = (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(2 * np.pi * f * 2.01 * tt)) * np.exp(-tt / 0.25)
        out[start:] += tone
    return fade(normalize(out, 0.6))


def yawn(seed=0):
    n = int(1.3 * RATE)
    t = np.linspace(0, 1, n)
    f0 = 330 * (1 - 0.35 * t) + 25 * np.sin(np.pi * t)
    v = voice(f0, [(0, "o"), (0.35, "a"), (1, "a")], breath=0.25, seed=seed)
    env = np.minimum(1, t * 4) * np.exp(-np.maximum(0, t - 0.5) * 4)
    return fade(normalize(v * env, 0.75))


def laugh(seed=0):
    rng = np.random.default_rng(seed)
    parts = []
    base = 360
    for k in range(5):
        dur = 0.09
        n = int(dur * RATE)
        f0 = np.linspace(base, base * 0.93, n)
        h = rng.normal(0, 1, int(0.03 * RATE)) * 0.25
        syl = voice(f0, [(0, "e"), (1, "e")], breath=0.15, seed=seed + k) * envelope(n, 0.004, 0.05)
        parts.append(fade(highpass(h, 1500) * np.linspace(0, 1, len(h)), 3))
        parts.append(fade(syl, 4))
        parts.append(np.zeros(int(0.03 * RATE)))
        base *= 0.96
    return fade(normalize(np.concatenate(parts), 0.8))


EFFECTS = {
    "laura_clap": [clap],
    "laura_snap": [snap],
    "laura_tap": [tap],
    "laura_whoosh": [whoosh],
    "laura_kiss": [kiss],
    "laura_hiccup": [hiccup],
    "laura_chatter": [chatter],
    "laura_hmph": [hmph],
    "laura_sigh": [sigh],
    "laura_sparkle": [sparkle],
    "laura_yawn": [yawn],
    "laura_laugh": [laugh],
}
VARIANTS = {"laura_clap": 3, "laura_chatter": 4, "laura_kiss": 2, "laura_snap": 2, "laura_tap": 2, "laura_hmph": 2, "laura_laugh": 2}


def write_ogg(samples, path, rate=RATE):
    pcm = (np.clip(samples, -1, 1) * 32767).astype("<i2")
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "s.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(rate)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-map_metadata", "-1", "-c:a", "libvorbis", "-q:a", "5", "-ac", "1", path], check=True)


def muffle(source, target):
    """Her own voice, heard through the hay: low-passed and a bit louder."""
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", source, "-map_metadata", "-1", "-af", "lowpass=f=420,lowpass=f=420,volume=2.2",
                    "-c:a", "libvorbis", "-q:a", "4", "-ac", "1", target], check=True)


def generate(out_dir):
    """Writes every effect; returns {event: [file stems]}."""
    os.makedirs(out_dir, exist_ok=True)
    made = {}
    for event, (fn,) in EFFECTS.items():
        stems = []
        for v in range(VARIANTS.get(event, 1)):
            stem = f"{event}_{v + 1}"
            write_ogg(fn(seed=v * 17 + 3), os.path.join(out_dir, stem + ".ogg"))
            stems.append(stem)
        made[event] = stems
    return made
