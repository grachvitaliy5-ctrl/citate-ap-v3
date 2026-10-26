"""
Генератор звука уведомлений о целях («рассвет»).
Только numpy + стандартная библиотека. Запуск:  python generate_goal_sound.py
Создаёт tools/variants/goal_sound_a.wav, _b.wav, _c.wav.
В приложение кладётся вариант A (app/src/main/res/raw/goal_sound.ogg).
"""
import os
import wave
import numpy as np

SR = 44100
HERE = os.path.dirname(os.path.abspath(__file__))

VARIANTS = {
    # тёплый: середина, мягкие верхние обертоны
    "a": dict(f0=392.0, dur=2.3, amps=[1.0, 0.42, 0.20, 0.10, 0.05], decays=[1.7, 1.2, 0.8, 0.5, 0.35],
              shimmer=0.10, reverb=0.22, rev_len=1.1, seed=1),
    # стеклянный: выше, ярче, длиннее звучат верхние частичные
    "b": dict(f0=659.25, dur=2.2, amps=[1.0, 0.35, 0.28, 0.22, 0.16], decays=[1.5, 1.2, 1.0, 0.8, 0.6],
              shimmer=0.18, reverb=0.26, rev_len=1.2, seed=2),
    # глубокий: низкий основной тон с октавой, длинный хвост
    "c": dict(f0=261.63, dur=2.5, amps=[1.0, 0.55, 0.22, 0.10, 0.05], decays=[2.0, 1.5, 0.9, 0.6, 0.4],
              shimmer=0.08, reverb=0.30, rev_len=1.4, seed=3),
}
RATIOS = [1.0, 2.0, 3.0, 4.2, 5.4]   # колокольчатые частичные


def synth(f0, dur, amps, decays, shimmer, reverb, rev_len, seed):
    rng = np.random.default_rng(seed)
    n = int(SR * dur)
    t = np.arange(n) / SR

    # Плавный подъём высоты на чистую квинту (x1.5) за ~1.1 с, без ступенек
    glide = 1.1
    x = np.clip(t / glide, 0, 1)
    ease = 1 - (1 - x) ** 3
    f = f0 * (1.0 + 0.5 * ease)

    # Мягкая атака ~120 мс
    attack = 0.12
    env_a = np.where(t < attack, 0.5 - 0.5 * np.cos(np.pi * t / attack), 1.0)

    out = np.zeros((n, 2))
    for k, (ratio, amp, dec) in enumerate(zip(RATIOS, amps, decays)):
        env = env_a * np.exp(-t / dec) * amp
        if k >= 2:  # мерцание верхних гармоник
            lfo = 1 + shimmer * np.sin(2 * np.pi * (5.5 + k) * t + rng.uniform(0, 6.28))
            env = env * lfo
        for ch, cents in enumerate((-3.0, 3.0)):  # лёгкий хорус между каналами
            detune = 2 ** (cents * (1 if k >= 2 else 0.3) / 1200)
            phase = 2 * np.pi * np.cumsum(f * ratio * detune) / SR
            out[:, ch] += env * np.sin(phase + rng.uniform(0, 6.28))

    # Короткий реверберационный хвост: свёртка с затухающим шумом (отдельно L/R)
    m = int(SR * rev_len)
    ir_t = np.arange(m) / SR
    total = n + m
    wet = np.zeros((total, 2))
    for ch in range(2):
        ir = rng.standard_normal(m) * np.exp(-ir_t / (rev_len / 4))
        ir[: int(0.012 * SR)] = 0  # предзадержка 12 мс
        ir /= np.sqrt(np.sum(ir ** 2))
        size = 1 << int(np.ceil(np.log2(total)))
        wet[:, ch] = np.fft.irfft(np.fft.rfft(out[:, ch], size) * np.fft.rfft(ir, size), size)[:total]
    dry = np.zeros((total, 2))
    dry[:n] = out
    mix = dry * (1 - reverb) + wet * reverb * 1.6

    # Обрезаем до длительности + 0.4 с хвоста, фейды без щелчков
    end = min(total, n + int(0.4 * SR))
    mix = mix[:end]
    fi = int(0.005 * SR)
    mix[:fi] *= np.linspace(0, 1, fi)[:, None]
    fo = int(0.35 * SR)
    mix[-fo:] *= (np.cos(np.linspace(0, np.pi, fo)) * 0.5 + 0.5)[:, None]

    # Пик −3 dBFS
    peak = np.max(np.abs(mix))
    mix *= (10 ** (-3 / 20)) / peak
    return mix


def write_wav(path, data):
    pcm = (np.clip(data, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


if __name__ == "__main__":
    out_dir = os.path.join(HERE, "variants")
    os.makedirs(out_dir, exist_ok=True)
    for name, params in VARIANTS.items():
        path = os.path.join(out_dir, f"goal_sound_{name}.wav")
        write_wav(path, synth(**params))
        print("saved", path)
