"""
Мелодия приветствия самурая (она же звук уведомлений по умолчанию).
Запуск:  python tools/generate_welcome.py
Создаёт app/src/main/res/raw/samurai_welcome.wav (если есть ffmpeg — также .ogg).
Алгоритм флейты тот же, что в Melody.kt (приложение рисует по нему мелодии островов).
"""
import os, subprocess, wave
import numpy as np

SR = 22050
SC = [293.66, 349.23, 392.0, 440.0, 523.25, 587.33, 698.46, 783.99, 880.0, 1046.5, 1174.66]
N0 = [(3, 1), (5, 1), (4, .5), (3, .5), (1, 1), (2, 1), (0, 3)]
BEAT = 0.46
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "app", "src", "main", "res", "raw")


def flute(f, dur, vel=1.0, seed=1):
    n = int((dur + .45) * SR)
    t = np.arange(n) / SR
    fr = np.where(t < .07, f * (.97 + .03 * t / .07), f)
    fr = fr + f * .007 * np.minimum(1, t / .3) * np.sin(2 * np.pi * 5.6 * t)
    ph = 2 * np.pi * np.cumsum(fr) / SR
    tri = 2 / np.pi * np.arcsin(np.sin(ph))
    sq = np.sin(ph) + np.sin(3 * ph) / 3
    osc = tri + .05 * sq
    env = np.interp(t, [0, .05, max(.06, dur - .08), dur + .14, dur + .5], [0, .2 * vel, .16 * vel, 0, 0])
    rng = np.random.default_rng(seed)
    ns = rng.standard_normal(n)
    k = max(1, int(SR / (f * 2)))
    ns = np.convolve(ns, np.ones(k) / k, mode="same")
    nenv = np.interp(t, [0, .02, .14, max(.15, dur)], [0, .5, .08, 0])
    return (osc + .08 * ns * nenv * 4) * env


def echo(x, delay=.33, fb=.34, mix=.45):
    d = int(delay * SR)
    y = np.zeros(len(x) + d * 7)
    y[:len(x)] += x
    wet = np.zeros_like(y)
    lp = 0.0
    for i in range(d, len(y)):
        src = y[i - d] * 0 + wet[i - d] * fb + (x[i - d] if i - d < len(x) else 0)
        lp += (src - lp) * .6
        wet[i] = lp
    return y + mix * wet


def render():
    total = sum(d for _, d in N0) * BEAT
    out = np.zeros(int((total + 3) * SR))
    acc = 0.0
    for k, (i, d) in enumerate(N0):
        s = flute(SC[i], d * BEAT * 1.05, .95, k + 1)
        a = int(acc * SR)
        out[a:a + len(s)] += s[:len(out) - a]
        acc += d * BEAT
    t = np.arange(len(out)) / SR
    drone = 0.1 * 2 / np.pi * np.arcsin(np.sin(2 * np.pi * 73.42 * t)) * np.minimum(1, t / .4) * np.exp(-t / 3.2)
    out += drone
    out = echo(out)
    out = out[:int((total + 1.6) * SR)]
    fade = int(.5 * SR)
    out[-fade:] *= np.linspace(1, 0, fade)
    out *= 0.8 / np.max(np.abs(out))
    return out


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    pcm = (render() * 32767).astype(np.int16)
    wav = os.path.join(OUT, "samurai_welcome.wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    try:
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4",
                        os.path.join(OUT, "samurai_welcome.ogg")], check=True)
        os.remove(wav)
        print("ogg ok")
    except Exception as e:
        print("ffmpeg недоступен, оставлен wav:", e)
