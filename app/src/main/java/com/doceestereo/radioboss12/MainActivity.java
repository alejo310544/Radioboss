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
    private static final int BG = 0xff090e1b;
    private static final int CARD = 0xff141e31;
    private static final int MUTED = 0xff9eabc3;
    private LinearLayout root;

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(1);
        c.setPadding(14, 12, 14, 14); c.setBackground(bg(CARD, 22));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 7, 0, 9); c.setLayoutParams(lp); return c;
    }
    private TextView heading(String s) {
        TextView t = text(s, 16, Color.WHITE); t.setTypeface(null, Typeface.BOLD);
        t.setPadding(2, 3, 2, 10); return t;
    }
    private void gap(LinearLayout parent, int h) {
        View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1,h));
    }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root = new LinearLayout(this); root.setOrientation(1);
        root.setPadding(16, 14, 16, 28); root.setBackgroundColor(BG);
        scroll.addView(root); setContentView(scroll);

        LinearLayout top = new LinearLayout(this); top.setOrientation(0); top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout brandCol = new LinearLayout(this); brandCol.setOrientation(1);
        TextView over = text("RADIOBOSS 12  /  ESTUDIO DIGITAL", 10, 0xff62e6ca); over.setTypeface(null,Typeface.BOLD);
        TextView brand = text("12 ESTÉREO", 29, Color.WHITE); brand.setTypeface(null,Typeface.BOLD); brand.setPadding(8,1,0,0);
        TextView freq = text("101.3 FM  ·  CONTROL DE CABINA", 11, MUTED); freq.setPadding(8,0,0,8);
        brandCol.addView(over); brandCol.addView(brand); brandCol.addView(freq);
        top.addView(brandCol,new LinearLayout.LayoutParams(0,-2,1));
        TextView liveTag=text("● LOCAL",11,0xff7bf0b3); liveTag.setTypeface(null,Typeface.BOLD); liveTag.setPadding(12,9,12,9); liveTag.setBackground(bg(0xff173b36,30)); top.addView(liveTag);
        root.addView(top);
        clock = text("--:--:--", 14, 0xff62e6ca); clock.setPadding(8,2,8,8); root.addView(clock);

        LinearLayout hero = card(); hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xff183b58,0xff34265d}));
        TextView small = text("PANEL PRINCIPAL",10,0xff9cefe1); small.setTypeface(null,Typeface.BOLD); hero.addView(small);
        now = text("Listo para salir al aire",19,Color.WHITE); now.setTypeface(null,Typeface.BOLD); now.setPadding(10,6,8,5); hero.addView(now);
        status = text("Cabina local · transmisión online desconectada",12,0xffd0d8e9); hero.addView(status);
        root.addView(hero);

        LinearLayout playerCard=card(); playerCard.addView(heading("🎚  REPRODUCTOR DE AUDIO"));
        TextView sub=text("Importa música y toca una pista para reproducirla.",12,MUTED); playerCard.addView(sub);
        LinearLayout controls=new LinearLayout(this); controls.setOrientation(0); controls.setPadding(0,8,0,4);
        Button add=button("＋  AÑADIR AUDIO",0xff2368ee); add.setTextSize(12); add.setOnClickListener(v->pickAudio());
        Button stop=button("■  DETENER",0xff354158); stop.setTextSize(12); stop.setOnClickListener(v->stopPlayer());
        LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,dp(52),1); half.setMargins(3,3,5,3); controls.addView(add,half);
        LinearLayout.LayoutParams half2=new LinearLayout.LayoutParams(0,dp(52),1); half2.setMargins(5,3,3,3); controls.addView(stop,half2); playerCard.addView(controls);
        playerCard.addView(text("LISTA DE REPRODUCCIÓN",10,0xff62e6ca));
        playlist=new LinearLayout(this); playlist.setOrientation(1); playlist.setPadding(0,4,0,0); playerCard.addView(playlist);
        root.addView(playerCard);

        root.addView(heading("ATAJOS DE CABINA"));
        LinearLayout grid1=new LinearLayout(this); grid1.setOrientation(0);
        LinearLayout fxCard=miniCard("♫","EFECTOS","Jingles y cortinas",0xff7c3aed);
        Button fx=button("＋ Importar",0xff6334bc); fx.setTextSize(12); fx.setOnClickListener(v->pickAudio()); fxCard.addView(fx);
        LinearLayout adCard=miniCard("▣","PUBLICIDAD","Cuñas comerciales",0xffd97706);
        Button ads=button("＋ Importar",0xffa9550a); ads.setTextSize(12); ads.setOnClickListener(v->pickAudio()); adCard.addView(ads);
        LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,-2,1); cell.setMargins(0,0,6,0); grid1.addView(fxCard,cell);
        LinearLayout.LayoutParams cell2=new LinearLayout.LayoutParams(0,-2,1); cell2.setMargins(6,0,0,0); grid1.addView(adCard,cell2); root.addView(grid1);

        LinearLayout grid2=new LinearLayout(this); grid2.setOrientation(0);
        LinearLayout micCard=miniCard("🎙","MICRÓFONO","Permisos y voz",0xff0891a2);
        Button mic=button("Activar acceso",0xff087f8c); mic.setTextSize(12); mic.setOnClickListener(v->{
            if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},77);
            else new AlertDialogBuilder(this).showMessage("Permiso del micrófono concedido. Para mezclar voz con música y transmitirla online todavía se requiere integrar el motor de transmisión.");
        }); micCard.addView(mic);
        LinearLayout streamCard=miniCard("◉","TRANSMISIÓN","Estado de Icecast",0xffdc3545);
        Button stream=button("Desconectado",0xff9f2636); stream.setTextSize(12); stream.setOnClickListener(v->new AlertDialogBuilder(this).showMessage("Esta versión administra audio local. La transmisión real por Icecast necesita configurar servidor y codificador de audio.")); streamCard.addView(stream);
        LinearLayout.LayoutParams c3=new LinearLayout.LayoutParams(0,-2,1); c3.setMargins(0,0,6,0); grid2.addView(micCard,c3);
        LinearLayout.LayoutParams c4=new LinearLayout.LayoutParams(0,-2,1); c4.setMargins(6,0,0,0); grid2.addView(streamCard,c4); root.addView(grid2);

        TextView foot=text("12 ESTÉREO 101.3 FM  •  CONSOLA MÓVIL",10,0xff68758d); foot.setGravity(android.view.Gravity.CENTER); foot.setPadding(0,18,0,8); root.addView(foot);
    }
    private LinearLayout miniCard(String emoji,String title,String subtitle,int accent) {
        LinearLayout c=new LinearLayout(this); c.setOrientation(1); c.setPadding(11,12,11,12); c.setBackground(bg(CARD,20));
        TextView icon=text(emoji,25,accent); icon.setPadding(2,0,2,4); c.addView(icon);
        TextView h=text(title,13,Color.WHITE); h.setTypeface(null,Typeface.BOLD); h.setPadding(2,1,2,1); c.addView(h);
        TextView d=text(subtitle,10,MUTED); d.setPadding(2,0,2,8); c.addView(d); return c;
    }
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
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
            Button row = button("▶    " + name + "     ·     TOCAR", 0xff202d44);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.LEFT);
            row.setTextSize(13); row.setPadding(12,4,12,4);
            row.setOnClickListener(v -> play(tracks.indexOf(uri)));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(50)); rowLp.setMargins(0,3,0,3);
            playlist.addView(row, rowLp);
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
