package com.minesweeper.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plays short synthesized sound effects. No audio files are needed: every effect is generated
 * as PCM samples once, then played on a background thread so the UI never stalls.
 * If the machine has no audio device, effects are silently skipped.
 */
public final class SoundPlayer {

    /** The available effects. */
    public enum Effect { REVEAL, FLAG, EXPLOSION, WIN }

    private static final float SAMPLE_RATE = 44_100f;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;
    private static final double VOLUME = 0.25;
    private static final double MILLIS_PER_SECOND = 1000.0;

    private static final double REVEAL_FREQ = 880;
    private static final int REVEAL_MS = 35;
    private static final double FLAG_FREQ = 620;
    private static final int FLAG_MS = 70;
    private static final int EXPLOSION_MS = 450;
    private static final double[] WIN_NOTES = {523.25, 659.25, 783.99, 1046.50}; // C5 E5 G5 C6
    private static final int WIN_NOTE_MS = 110;
    private static final double DECAY_RATE = 5.0;
    private static final long NOISE_SEED = 7L;

    private final Map<Effect, byte[]> samples = new EnumMap<>(Effect.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "minesweeper-sound");
        t.setDaemon(true);
        return t;
    });
    private final AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS, true, false);
    private volatile boolean enabled;

    /** Pre-renders every effect. */
    public SoundPlayer() {
        samples.put(Effect.REVEAL, tone(REVEAL_FREQ, REVEAL_MS));
        samples.put(Effect.FLAG, tone(FLAG_FREQ, FLAG_MS));
        samples.put(Effect.EXPLOSION, noise(EXPLOSION_MS));
        samples.put(Effect.WIN, arpeggio(WIN_NOTES, WIN_NOTE_MS));
    }

    /** @param enabled whether effects should play */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @return whether effects play */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Plays an effect asynchronously if sound is enabled.
     *
     * @param effect the effect
     */
    public void play(Effect effect) {
        if (!enabled) {
            return;
        }
        byte[] data = samples.get(effect);
        executor.submit(() -> write(data));
    }

    /** Stops the background thread. */
    public void shutdown() {
        executor.shutdownNow();
    }

    private void write(byte[] data) {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();
            line.write(data, 0, data.length);
            line.drain();
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            // No usable audio device: ignore, sound is optional.
        }
    }

    private static byte[] tone(double frequency, int millis) {
        int n = sampleCount(millis);
        double[] wave = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / SAMPLE_RATE;
            double envelope = Math.exp(-DECAY_RATE * i / (double) n);
            wave[i] = Math.sin(2 * Math.PI * frequency * t) * envelope;
        }
        return toPcm(wave);
    }

    private static byte[] noise(int millis) {
        int n = sampleCount(millis);
        double[] wave = new double[n];
        Random random = new Random(NOISE_SEED);
        double smoothed = 0;
        for (int i = 0; i < n; i++) {
            // Simple low-pass filter gives a deeper "boom" than raw white noise.
            smoothed += (random.nextDouble() * 2 - 1 - smoothed) * 0.15;
            double envelope = Math.exp(-DECAY_RATE * i / (double) n);
            wave[i] = smoothed * 3 * envelope;
        }
        return toPcm(wave);
    }

    private static byte[] arpeggio(double[] notes, int noteMillis) {
        int perNote = sampleCount(noteMillis);
        double[] wave = new double[perNote * notes.length];
        for (int k = 0; k < notes.length; k++) {
            for (int i = 0; i < perNote; i++) {
                double t = i / SAMPLE_RATE;
                double envelope = Math.exp(-DECAY_RATE * 0.6 * i / (double) perNote);
                wave[k * perNote + i] = Math.sin(2 * Math.PI * notes[k] * t) * envelope;
            }
        }
        return toPcm(wave);
    }

    private static int sampleCount(int millis) {
        return (int) (SAMPLE_RATE * millis / MILLIS_PER_SECOND);
    }

    private static byte[] toPcm(double[] wave) {
        byte[] pcm = new byte[wave.length * 2];
        for (int i = 0; i < wave.length; i++) {
            double clipped = Math.max(-1, Math.min(1, wave[i] * VOLUME));
            short value = (short) (clipped * Short.MAX_VALUE);
            pcm[2 * i] = (byte) (value & 0xFF);
            pcm[2 * i + 1] = (byte) ((value >> 8) & 0xFF);
        }
        return pcm;
    }
}
