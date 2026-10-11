package com.doceestereo.radioboss12;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.AudioPlaybackCaptureConfiguration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.naman14.androidlame.AndroidLame;
import com.naman14.androidlame.LameBuilder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/** SHOUTcast v1 source + MP3 encoder. Mixes microphone and capturable Android playback on Android 10+. */
final class ShoutcastMicStreamer {
    interface Listener { void onStatus(String message); }
    private static final int RATE = 22050;
    private static final int CHUNK = 1024;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Socket socket;
    private volatile AudioRecord micRecorder;
    private volatile AudioRecord playbackRecorder;
    private volatile AndroidLame lame;
    private volatile MediaProjection projection;
    private MediaProjection.Callback projectionCallback;
    private Thread worker;

    boolean isRunning() { return running.get(); }

    synchronized void start(String host, int port, String password, MediaProjection mediaProjection, Context context, Listener listener) {
        if (running.get()) return;
        projection = mediaProjection;
        running.set(true);
        worker = new Thread(() -> streamLoop(host, port, password, context, listener), "RadioBoss12-Stream");
        worker.start();
    }

    private void status(Listener l, String s) { if (l != null) l.onStatus(s); }

    private void streamLoop(String rawHost, int port, String password, Context context, Listener listener) {
        try {
            String host = rawHost.trim().replaceFirst("^https?://", "").replaceAll("/.*$", "");
            if (host.isEmpty() || port < 1 || port > 65535 || password == null || password.isEmpty())
                throw new IllegalArgumentException("Completa dirección, puerto y contraseña de fuente.");

            status(listener, "Conectando con SHOUTcast 1…");
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 10000);
            socket.setSoTimeout(10000);
            InputStream in = new BufferedInputStream(socket.getInputStream());
            OutputStream out = new BufferedOutputStream(socket.getOutputStream());
            out.write((password + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
            String response = readLine(in, 1024);
            if (response == null || !(response.startsWith("OK") || response.startsWith("ICY 200")))
                throw new IllegalStateException("El servidor rechazó la fuente: " + (response == null ? "sin respuesta" : response));
            // After the SHOUTcast 1 source password handshake, raw MP3 bytes must follow.
            // Sending HTTP-style headers here would be encoded as audible/corrupt stream data.
            socket.setSoTimeout(0);

            int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (min <= 0) throw new IllegalStateException("El teléfono no permite iniciar la captura de audio.");
            int bufferBytes = Math.max(min * 2, CHUNK * 2 * 4);
            micRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferBytes);
            if (micRecorder.getState() != AudioRecord.STATE_INITIALIZED)
                throw new IllegalStateException("No se pudo inicializar el micrófono. Revisa el permiso.");

            boolean playbackReady = false;
            if (Build.VERSION.SDK_INT >= 29 && projection != null) {
                projectionCallback = new MediaProjection.Callback() {
                    @Override public void onStop() {
                        running.set(false);
                        try { if (playbackRecorder != null && playbackRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) playbackRecorder.stop(); } catch (Exception ignored) {}
                    }
                };
                projection.registerCallback(projectionCallback, new Handler(Looper.getMainLooper()));
                AudioFormat playbackFormat = new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
                AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(projection)
                        .addMatchingUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(android.media.AudioAttributes.USAGE_GAME).build();
                playbackRecorder = new AudioRecord.Builder().setAudioFormat(playbackFormat)
                        .setBufferSizeInBytes(bufferBytes).setAudioPlaybackCaptureConfig(config).build();
                playbackReady = playbackRecorder.getState() == AudioRecord.STATE_INITIALIZED;
                if (!playbackReady) { playbackRecorder.release(); playbackRecorder = null; }
            }

            lame = new LameBuilder().setInSampleRate(RATE).setOutSampleRate(RATE)
                    .setOutChannels(1).setOutBitrate(64).build();
            short[] mic = new short[CHUNK];
            short[] playback = new short[CHUNK];
            short[] mixed = new short[CHUNK];
            byte[] mp3 = new byte[8192];
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            micRecorder.startRecording();
            if (playbackReady) playbackRecorder.startRecording();
            final boolean capturingPlayback = playbackReady;
            status(listener, capturingPlayback
                    ? "SHOUTcast conectado · mezclando micrófono + audio capturable del teléfono (64 kbps)"
                    : "SHOUTcast conectado · solo micrófono; audio interno no disponible en este Android");

            while (running.get()) {
                int mn = micRecorder.read(mic, 0, CHUNK, AudioRecord.READ_BLOCKING);
                if (mn < 0) throw new IllegalStateException("Error leyendo el micrófono: " + mn);
                int pn = 0;
                if (capturingPlayback) {
                    // Non-blocking prevents a silent/uncapturable app from stalling the live microphone.
                    pn = playbackRecorder.read(playback, 0, CHUNK, AudioRecord.READ_NON_BLOCKING);
                    if (pn < 0) pn = 0;
                }
                int maxVolume = audioManager == null ? 0 : audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int currentVolume = audioManager == null ? maxVolume : audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                float deviceVolumeGain = maxVolume > 0 ? (float) currentVolume / maxVolume : 1.0f;
                for (int i = 0; i < mn; i++) {
                    int p = i < pn ? Math.round(playback[i] * deviceVolumeGain) : 0;
                    int sum = mic[i] + p;
                    // Soft headroom guard: avoid harsh clipping when music and mic overlap.
                    if (sum > 30000) sum = 30000 + (sum - 30000) / 4;
                    if (sum < -30000) sum = -30000 + (sum + 30000) / 4;
                    mixed[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sum));
                }
                int bytes = lame.encode(mixed, mixed, mn, mp3);
                if (bytes > 0) { out.write(mp3, 0, bytes); out.flush(); }
            }
            int flushed = lame.flush(mp3);
            if (flushed > 0) { out.write(mp3, 0, flushed); out.flush(); }
        } catch (Exception e) {
            if (running.get()) status(listener, "Error de emisión: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        } finally {
            running.set(false);
            try { if (micRecorder != null) { if (micRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) micRecorder.stop(); micRecorder.release(); } } catch (Exception ignored) {}
            micRecorder = null;
            try { if (playbackRecorder != null) { if (playbackRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) playbackRecorder.stop(); playbackRecorder.release(); } } catch (Exception ignored) {}
            playbackRecorder = null;
            try { if (lame != null) lame.close(); } catch (Exception ignored) {}
            lame = null;
            try { if (socket != null) socket.close(); } catch (Exception ignored) {}
            socket = null;
            if (projection != null) {
                try { if (projectionCallback != null) projection.unregisterCallback(projectionCallback); } catch (Exception ignored) {}
                try { projection.stop(); } catch (Exception ignored) {}
            }
            projection = null; projectionCallback = null;
            status(listener, "Emisión detenida");
        }
    }

    private static String readLine(InputStream in, int limit) throws Exception {
        StringBuilder b = new StringBuilder(); int c;
        while (b.length() < limit && (c = in.read()) != -1) { if (c == '\n') break; if (c != '\r') b.append((char)c); }
        return b.length() == 0 ? null : b.toString();
    }

    synchronized void stop() {
        running.set(false);
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        try { if (micRecorder != null && micRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) micRecorder.stop(); } catch (Exception ignored) {}
        try { if (playbackRecorder != null && playbackRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) playbackRecorder.stop(); } catch (Exception ignored) {}
    }
}
