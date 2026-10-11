package com.doceestereo.radioboss12;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import java.util.ArrayList;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private final ArrayList<Uri> tracks = new ArrayList<>();
    private final ArrayList<String> names = new ArrayList<>();
    private MediaPlayer player;
    private TextView now, clock, status;
    private LinearLayout playlist;
    private int current = -1;
    private static final int PICK_AUDIO = 44;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(12,16,27));
        getWindow().setNavigationBarColor(Color.rgb(12,16,27));
        buildUi();
        updateClock();
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius); return d;
    }
    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size);
        t.setTextColor(color); t.setPadding(12, 9, 12, 9); return t;
    }
    private Button button(String s, int color) {
        Button b = new Button(this); b.setText(s); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setBackground(bg(color, 18)); return b;
    }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1);
        root.setPadding(16,16,16,24); root.setBackgroundColor(Color.rgb(12,16,27));
        scroll.addView(root); setContentView(scroll);

        TextView brand = text("12 ESTÉREO 101.3 FM", 24, Color.WHITE);
        brand.setTypeface(null, Typeface.BOLD); root.addView(brand);
        clock = text("--:--:--", 18, Color.rgb(80,220,190)); root.addView(clock);
        status = text("CABINA LOCAL · Sin transmisión", 13, Color.LTGRAY); root.addView(status);

        root.addView(section("REPRODUCTOR / PLAYLIST"));
        LinearLayout controls = new LinearLayout(this); controls.setOrientation(0);
        Button add = button("＋ Importar audio", 0xff1667d9);
        add.setOnClickListener(v -> pickAudio()); controls.addView(add, new LinearLayout.LayoutParams(0,-2,1));
        Button stop = button("■ Detener", 0xff4b5263);
        stop.setOnClickListener(v -> stopPlayer()); controls.addView(stop, new LinearLayout.LayoutParams(0,-2,1));
        root.addView(controls);
        now = text("Selecciona un audio para comenzar", 15, Color.WHITE); root.addView(now);
        playlist = new LinearLayout(this); playlist.setOrientation(1); root.addView(playlist);

        root.addView(section("BOTONERA DE EFECTOS"));
        TextView hint = text("Importa audios desde el teléfono y tócalos para reproducirlos. En esta versión inicial los botones se crean al importar.", 13, Color.LTGRAY);
        root.addView(hint);
        Button fx = button("＋ Importar efecto / jingle", 0xff8b42c7);
        fx.setOnClickListener(v -> pickAudio()); root.addView(fx);

        root.addView(section("PUBLICIDAD"));
        Button ads = button("＋ Importar cuña publicitaria", 0xffd97706);
        ads.setOnClickListener(v -> pickAudio()); root.addView(ads);
        root.addView(text("La programación automática de tandas se incorporará en la siguiente etapa.", 13, Color.LTGRAY));

        root.addView(section("MICRÓFONO / TALK OVER"));
        Button mic = button("Solicitar acceso al micrófono", 0xff087f8c);
        mic.setOnClickListener(v -> {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 77);
            else new AlertDialogBuilder(this).showMessage("Permiso concedido. La mezcla Talk Over y el envío del micrófono a Icecast aún requieren integrar el motor de transmisión.");
        });
        root.addView(mic);

        root.addView(section("ICECAST · TRANSMISIÓN ONLINE"));
        root.addView(text("Servidor, puerto, punto de montaje y contraseña se configurarán en la siguiente etapa. No introduzcas contraseñas reales en esta versión de prueba.", 13, Color.LTGRAY));
        Button stream = button("Estado: desconectado", 0xffb42332);
        stream.setOnClickListener(v -> new AlertDialogBuilder(this).showMessage("La interfaz está preparada como base. Para transmitir audio real hace falta integrar y probar un codificador/source client compatible con Icecast."));
        root.addView(stream);
        root.addView(text("Versión inicial · Android · RadioBoss 12", 12, Color.GRAY));
    }
    private TextView section(String s) {
        TextView t = text(s, 17, 0xff62e6ca);
        t.setTypeface(null, Typeface.BOLD); t.setPadding(12,22,12,8); return t;
    }
    private void pickAudio() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("audio/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_AUDIO);
    }
    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req,res,data);
        if (req == PICK_AUDIO && res == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(Exception ignored) {}
            String name = uri.getLastPathSegment();
            if (name == null) name = "Audio " + (names.size()+1);
            int slash = name.lastIndexOf('/');
            if (slash >= 0) name = name.substring(slash+1);
            tracks.add(uri); names.add(name);
            Button row = button("▶  " + name, 0xff222c40);
            row.setOnClickListener(v -> play(tracks.indexOf(uri)));
            playlist.addView(row, new LinearLayout.LayoutParams(-1,-2));
            now.setText("Añadido: " + name);
        }
    }
    private void play(int index) {
        try {
            stopPlayer(); current = index;
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setDataSource(this, tracks.get(index));
            player.setOnPreparedListener(mp -> { mp.start(); now.setText("Reproduciendo: " + names.get(index)); });
            player.setOnCompletionListener(mp -> { if (current+1 < tracks.size()) play(current+1); else now.setText("Playlist terminada"); });
            player.prepareAsync();
        } catch(Exception e) { now.setText("No se pudo reproducir este archivo."); }
    }
    private void stopPlayer() {
        if (player != null) { try { if(player.isPlaying()) player.stop(); } catch(Exception ignored) {} player.release(); player=null; }
    }
    private void updateClock() {
        clock.setText(new SimpleDateFormat("EEEE, dd MMM · HH:mm:ss", new Locale("es","CO")).format(new Date()));
        clock.postDelayed(this::updateClock, 1000);
    }
    @Override protected void onDestroy() { stopPlayer(); super.onDestroy(); }

    // Small helper to avoid introducing an external UI dependency.
    private static class AlertDialogBuilder {
        private final Activity a;
        AlertDialogBuilder(Activity a) { this.a = a; }
        void showMessage(String msg) {
            new android.app.AlertDialog.Builder(a).setTitle("RadioBoss 12").setMessage(msg)
                .setPositiveButton("Entendido", (d,w) -> {}).show();
        }
    }
}
