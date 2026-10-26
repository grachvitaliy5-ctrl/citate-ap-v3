import wave, numpy as np
SR = 44100
A5, E6 = 880.0, 1320.0          # две ноты: ля и ми (квинта вверх)
PARTIALS = [(0.5, 0.35, 2.20), (1.0, 1.00, 2.00), (1.2, 0.25, 1.40), (1.5, 0.30, 1.20), (2.0, 0.20, 0.90), (2.76, 0.10, 0.50)]

def voice(f0, t0, vel, dec_scale, total, rng):
    n = int(SR * total); t = np.arange(n) / SR - t0
    out = np.zeros((n, 2)); on = t >= 0
    for k, (r, a, dec) in enumerate(PARTIALS):
        env = np.where(on, a * vel * np.exp(-np.clip(t, 0, None) / (dec * dec_scale)) * np.minimum(1.0, np.clip(t, 0, None) / 0.008), 0.0)
        for ch, d in enumerate((-0.6, 0.6)):
            out[:, ch] += env * np.sin(2 * np.pi * (f0 * r + d * (1 if k else 0.3)) * np.clip(t, 0, None))
    return out

def build(steps, total, name, peak=-9.0, lp=3500, reverb=0.28):
    rng = np.random.default_rng(3)
    mix = sum(voice(f, t0, v, ds, total, rng) for f, t0, v, ds in steps)
    # реверберация (длинный мягкий хвост)
    n = len(mix); m = int(SR * 1.4); irt = np.arange(m) / SR
    size = 1 << int(np.ceil(np.log2(n + m))); wet = np.zeros((n + m, 2))
    for ch in range(2):
        ir = rng.standard_normal(m) * np.exp(-irt / 0.35); ir[:int(0.012 * SR)] = 0; ir /= np.sqrt((ir ** 2).sum())
        wet[:, ch] = np.fft.irfft(np.fft.rfft(mix[:, ch], size) * np.fft.rfft(ir, size), size)[:n + m]
    dry = np.zeros((n + m, 2)); dry[:n] = mix
    mix = dry * (1 - reverb) + wet * reverb * 1.5
    # срез верха
    f = np.fft.rfftfreq(len(mix), 1 / SR)
    for ch in range(2):
        mix[:, ch] = np.fft.irfft(np.fft.rfft(mix[:, ch]) / (1 + (f / lp) ** 4), len(mix))
    # мягкое окончание: длинное затухание до тишины (косинус 2 с), без обрыва
    fo = int(2.0 * SR); mix[-fo:] *= (0.5 + 0.5 * np.cos(np.linspace(0, np.pi, fo)))[:, None]
    fi = int(0.002 * SR); mix[:fi] *= np.linspace(0, 1, fi)[:, None]
    mix *= 10 ** (peak / 20) / np.abs(mix).max()
    with wave.open(f"./{name}.wav", "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(mix, -1, 1) * 32767).astype("<i2").tobytes())
    m1 = np.abs(mix.mean(1))
    def db(a, b): return 20 * np.log10(m1[int(a * SR):int(b * SR)].max() + 1e-9)
    L = len(m1) / SR
    print(f"{name}: {L:.1f}с, пик {db(0, L):.1f}дБ, последние 1с: {db(L-1, L):.1f}дБ, последние 0.1с: {db(L-0.1, L):.1f}дБ")


A5,Cs6,E6,A6,Fs6 = 880.0, 1108.7, 1318.5, 1760.0, 1480.0
# 1: ля - до# - ми (восходящее арпеджио ля-мажора), последняя нота звенит дольше
build([(A5, 0.00, 0.70, 1.0), (Cs6, 0.30, 0.85, 1.0), (E6, 0.62, 1.00, 1.8)], 3.0, "melodiya_1_voskhod")
# 2: до# - ми - ля (подъём к вершине), финал на октаву выше
build([(Cs6, 0.00, 0.70, 1.0), (E6, 0.30, 0.85, 1.0), (A6, 0.62, 0.95, 1.6), (A5, 0.62, 0.55, 1.8)], 3.0, "melodiya_2_vershina")
