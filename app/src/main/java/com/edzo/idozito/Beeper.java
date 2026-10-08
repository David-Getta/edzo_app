package com.edzo.idozito;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Sípszó-szintetizátor. Nincs hangfájl: minden hangot menet közben állítunk elő
 * (AudioTrack + szinusz), így az APK apró marad. Statikus, kontextus nélkül hívható.
 */
public final class Beeper {

    private Beeper() {}

    /** Globális hangerő-szorzó (0..1), a Beállításokból állítható. */
    public static volatile float masterVolume = 0.8f;

    /** Egy választható síphang: név + hangszekvencia. Egy elem: {frekvencia, hossz(ms), utána szünet(ms), hangerő}. */
    public static final class Sound {
        public final String name;
        public final double[][] seq;
        public Sound(String name, double[][] seq) { this.name = name; this.seq = seq; }
    }

    /** A választható hangok. Az index kerül mentésre a beállításokban. */
    public static final Sound[] SOUNDS = {
            new Sound("Síp – magas",   new double[][]{{920, 160, 0, 0.55}}),
            new Sound("Síp – mély",    new double[][]{{430, 320, 0, 0.50}}),
            new Sound("Dupla síp",     new double[][]{{920, 140, 60, 0.55}, {920, 170, 0, 0.55}}),
            new Sound("Tripla síp",    new double[][]{{900, 110, 55, 0.50}, {900, 110, 55, 0.50}, {900, 160, 0, 0.55}}),
            new Sound("Csengő",        new double[][]{{1250, 90, 25, 0.50}, {1650, 240, 0, 0.45}}),
            new Sound("Kürt",          new double[][]{{300, 380, 0, 0.60}}),
            new Sound("Emelkedő",      new double[][]{{600, 120, 20, 0.50}, {820, 120, 20, 0.50}, {1040, 210, 0, 0.55}}),
            new Sound("Ereszkedő",     new double[][]{{1040, 120, 20, 0.50}, {820, 120, 20, 0.50}, {600, 210, 0, 0.55}}),
            new Sound("Gong",          new double[][]{{200, 500, 0, 0.60}}),
    };

    public static Sound soundAt(int idx) {
        if (idx < 0 || idx >= SOUNDS.length) idx = 0;
        return SOUNDS[idx];
    }

    /** Kiválasztott hang lejátszása. */
    public static void play(int idx) { tone(soundAt(idx).seq); }

    /** Rövid visszaszámláló csipogás (3-2-1). */
    public static void tick() { tone(new double[][]{{760, 90, 0, 0.35}}); }

    /** Záró dallam az edzés végén. */
    public static void finish() {
        tone(new double[][]{{660, 220, 40, 0.5}, {840, 220, 40, 0.5}, {1040, 340, 0, 0.5}});
    }

    /** Hangszekvencia lejátszása háttérszálon (nem blokkolja a hívót). */
    public static void tone(final double[][] seq) {
        new Thread(() -> {
            for (double[] p : seq) {
                synth(p[0], (int) p[1], p[3]);
                if (p[2] > 0) {
                    try { Thread.sleep((long) p[2]); } catch (InterruptedException ignored) { return; }
                }
            }
        }).start();
    }

    private static void synth(double freq, int durMs, double vol) {
        final int sr = 44100;
        int n = (int) ((long) durMs * sr / 1000);
        if (n <= 0) return;
        short[] buf = new short[n];
        int fade = Math.max(1, Math.min(n / 8, sr / 200));
        double gain = vol * Math.max(0f, Math.min(1f, masterVolume));
        for (int i = 0; i < n; i++) {
            double env = 1.0;
            if (i < fade) env = i / (double) fade;
            else if (i > n - fade) env = (n - i) / (double) fade;
            double sample = Math.sin(2.0 * Math.PI * freq * i / sr);
            buf[i] = (short) (gain * env * sample * 32767.0);
        }
        int min = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bytes = Math.max(min, n * 2);
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, sr,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bytes, AudioTrack.MODE_STATIC);
        try {
            at.write(buf, 0, n);
            at.play();
            Thread.sleep(durMs + 40);
        } catch (Exception ignored) {
        } finally {
            try { at.stop(); } catch (Exception ignored) {}
            try { at.release(); } catch (Exception ignored) {}
        }
    }
}
