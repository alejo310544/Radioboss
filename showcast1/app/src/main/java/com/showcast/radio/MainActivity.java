package com.showcast.radio;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_PLAYLIST_AUDIO = 101;
    private static final int PICK_PAD_AUDIO = 102;
    private final ArrayList<Uri> playlist = new ArrayList<>();
    private final ArrayList<String> playlistNames = new ArrayList<>();
    private final Handler handler = new Handler();
    private MediaPlayer player;
    private TextView clock, nowPlaying, playlistText, micStatus;
    private EditText serverIp, serverPort, serverPassword, stationName;
    private int padToLoad = -1;
    private final Uri[] pads = new Uri[8];
    private final String[] padNames = new String[8];

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        updateClock();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 200);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(28)); root.setBackgroundColor(0xFF101820);
        scroll.addView(root); setContentView(scroll);
        TextView brand = text("SHOWCAST 1", 28, 0xFF42D6C8); brand.setGravity(Gravity.CENTER); root.addView(brand);
        clock = text("--:--:--", 24, 0xFFFFFFFF); clock.setGravity(Gravity.CENTER); root.addView(clock);
        TextView tagline = text("AUTOMATIZACIÓN RADIAL MÓVIL", 12, 0xFFB8C7D1); tagline.setGravity(Gravity.CENTER); root.addView(tagline);
        section(root, "PLAYLIST");
        nowPlaying = text("Sin audio en reproducción", 15, 0xFFFFFFFF); root.addView(nowPlaying);
        Button importPlaylist = button("＋ Importar audio a playlist"); root.addView(importPlaylist);
        importPlaylist.setOnClickListener(v -> pickAudio(PICK_PLAYLIST_AUDIO));
        LinearLayout playback = new LinearLayout(this); playback.setOrientation(LinearLayout.HORIZONTAL);
        Button play = button("▶ Reproducir"); Button pause = button("Ⅱ Pausar"); Button next = button("⏭ Siguiente");
        playback.addView(play, weight()); playback.addView(pause, weight()); playback.addView(next, weight()); root.addView(playback);
        play.setOnClickListener(v -> playCurrent()); pause.setOnClickListener(v -> { if (player != null && player.isPlaying()) player.pause(); });
        next.setOnClickListener(v -> { if (!playlist.isEmpty()) { int idx = currentIndex(); playIndex((idx + 1) % playlist.size()); } });
        playlistText = text("Playlist vacía", 13, 0xFFD8E2E8); root.addView(playlistText);

        section(root, "BOTONERA DE EFECTOS Y CUÑAS");
        TextView hint = text("Pulsa Cargar en cada botón para elegir un audio de tu teléfono.", 12, 0xFFB8C7D1); root.addView(hint);
        for (int i=0; i<8; i++) {
            final int index = i;
            LinearLayout line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL);
            Button fire = button("EFECTO " + (i+1)); Button load = button("Cargar audio");
            line.addView(fire, weight()); line.addView(load, weight()); root.addView(line);
            fire.setOnClickListener(v -> playPad(index)); load.setOnClickListener(v -> { padToLoad=index; pickAudio(PICK_PAD_AUDIO); });
        }

        section(root, "MICRÓFONO / TALK OVER");
        micStatus = text("Micrófono: permiso solicitado. El envío de voz al servidor requiere el motor de transmisión.", 13, 0xFFD8E2E8); root.addView(micStatus);
        Button mic = button("Solicitar permiso de micrófono"); root.addView(mic);
        mic.setOnClickListener(v -> { if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},200); else Toast.makeText(this,"Permiso de micrófono concedido",Toast.LENGTH_SHORT).show(); });

        section(root, "TRANSMISIÓN ONLINE · SHOUTcast v1");
        serverIp = edit("IP o dirección del servidor"); root.addView(serverIp);
        serverPort = edit("Puerto"); serverPort.setInputType(2); root.addView(serverPort);
        serverPassword = edit("Contraseña de transmisión"); serverPassword.setInputType(129); root.addView(serverPassword);
        stationName = edit("Nombre de la emisora (opcional)"); root.addView(stationName);
        Button connect = button("Conectar / Transmitir"); root.addView(connect);
        TextView streamNote = text("La pantalla de configuración está preparada, pero esta versión inicial todavía no envía audio al servidor. Para transmitir de verdad hay que integrar y probar un codificador/cliente SHOUTcast compatible con tu tipo de servidor.", 12, 0xFFFFD27A); root.addView(streamNote);
        connect.setOnClickListener(v -> Toast.makeText(this,"Configuración guardada solo en pantalla. El motor SHOUTcast aún no está integrado.",Toast.LENGTH_LONG).show());
        TextView footer = text("SHOWCAST 1 · Versión inicial", 11, 0xFF81939F); footer.setGravity(Gravity.CENTER); root.addView(footer);
    }

    private void pickAudio(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("audio/*");
        startActivityForResult(intent, requestCode);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri=data.getData(); getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String name = uri.getLastPathSegment(); if (name == null) name="Audio seleccionado";
        if (requestCode == PICK_PLAYLIST_AUDIO) { playlist.add(uri); playlistNames.add(name); refreshPlaylist(); if (playlist.size()==1) playIndex(0); }
        else if (requestCode == PICK_PAD_AUDIO && padToLoad >= 0) { pads[padToLoad]=uri; padNames[padToLoad]=name; Toast.makeText(this,"Audio cargado en EFECTO "+(padToLoad+1),Toast.LENGTH_SHORT).show(); padToLoad=-1; }
    }
    private void playCurrent() { if (playlist.isEmpty()) { Toast.makeText(this,"Importa primero un audio",Toast.LENGTH_SHORT).show(); return; } if (player != null && player.isPlaying()) return; if (player != null) { player.start(); return; } playIndex(0); }
    private void playIndex(int index) {
        if (playlist.isEmpty()) return; releasePlayer();
        try { player = new MediaPlayer(); player.setDataSource(this, playlist.get(index)); player.setOnPreparedListener(mp -> { mp.start(); nowPlaying.setText("Reproduciendo: " + playlistNames.get(index)); }); player.setOnCompletionListener(mp -> { if (playlist.size()>1) playIndex((index+1)%playlist.size()); }); player.setOnErrorListener((mp, what, extra) -> { Toast.makeText(this,"No se pudo reproducir este formato de audio",Toast.LENGTH_SHORT).show(); releasePlayer(); return true; }); player.prepareAsync(); }
        catch (IOException | IllegalStateException e) { Toast.makeText(this,"No se pudo abrir el audio",Toast.LENGTH_SHORT).show(); releasePlayer(); }
    }
    private void playPad(int index) { if (pads[index]==null) { Toast.makeText(this,"Pulsa Cargar audio primero",Toast.LENGTH_SHORT).show(); return; } try { MediaPlayer p=new MediaPlayer(); p.setDataSource(this,pads[index]); p.setOnPreparedListener(MediaPlayer::start); p.setOnCompletionListener(MediaPlayer::release); p.setOnErrorListener((mp,w,x)->{mp.release();return true;}); p.prepareAsync(); } catch (Exception e) { Toast.makeText(this,"No se pudo reproducir el efecto",Toast.LENGTH_SHORT).show(); } }
    private int currentIndex() { if (player == null) return -1; return 0; }
    private void refreshPlaylist() { StringBuilder s=new StringBuilder(); for(int i=0;i<playlistNames.size();i++) s.append(i+1).append(". ").append(playlistNames.get(i)).append('\n'); playlistText.setText(s.toString()); }
    private void releasePlayer() { if(player!=null){try{player.release();}catch(Exception ignored){} player=null;} }
    private void updateClock() { if(clock!=null) clock.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date())); handler.postDelayed(this::updateClock,1000); }
    private void section(LinearLayout root,String title) { TextView t=text(title,16,0xFF42D6C8); t.setPadding(0,dp(20),0,dp(8)); root.addView(t); }
    private TextView text(String value,int size,int color) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setPadding(0,dp(5),0,dp(5)); return t; }
    private Button button(String label) { Button b=new Button(this); b.setText(label); b.setTextColor(0xFFFFFFFF); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF176B70)); return b; }
    private EditText edit(String hint) { EditText e=new EditText(this); e.setSingleLine(true); e.setHint(hint); e.setTextColor(0xFFFFFFFF); e.setHintTextColor(0xFF9BAAB3); e.setPadding(dp(12),dp(4),dp(12),dp(4)); return e; }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,1f); }
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); releasePlayer(); super.onDestroy(); }
}
