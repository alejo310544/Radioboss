package com.doceestereo.radioboss12;

import android.app.*;
import android.os.Bundle;
import android.os.Handler;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.*;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int BG=0xff0a1020, CARD=0xff151f33, MUTED=0xff9cabc4, TEAL=0xff4de0c1, BLUE=0xff3478f6;
    private static final int PICK_AUDIO=44;
    private static final int REQUEST_SYSTEM_AUDIO_CAPTURE=78;
    private String pendingStreamHost, pendingStreamPassword;
    private int pendingStreamPort;
    private static final int MODE_MUSIC=1, MODE_FX=2, MODE_AD=3;
    private int pickMode=MODE_MUSIC, activeTab=0;
    private final ArrayList<AudioItem> music=new ArrayList<>(), effects=new ArrayList<>(), ads=new ArrayList<>();
    private final HashMap<Integer,MediaPlayer> effectPlayers=new HashMap<>();
    private final HashMap<Integer,MediaPlayer> adPlayers=new HashMap<>();
    private MediaPlayer musicPlayer;
    private LinearLayout root, body, nav;
    private TextView clock, headerStatus;
    private final Handler handler=new Handler();
    private final HashMap<Integer,String> lastPlayedMinute=new HashMap<>();
    private EditText hostField, portField, mountField, passField, userField;
    private TextView streamState;
    private ShoutcastMicStreamer streamEngine;
    private final Runnable clockTask=new Runnable(){ public void run(){ updateClock(); checkSchedules(); handler.postDelayed(this,10000); }};

    private static class AudioItem {
        Uri uri; String name; float volume=0.65f; String schedule="";
        AudioItem(Uri u,String n){uri=u;name=n;}
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);buildShell();showTab(0);handler.post(clockTask);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView txt(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(8),dp(6),dp(8),dp(6));return t;}
    private Button btn(String s,int color){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);b.setMinHeight(dp(44));b.setBackground(rounded(color,14));return b;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout card(){LinearLayout c=vertical();c.setPadding(dp(14),dp(12),dp(14),dp(14));c.setBackground(rounded(CARD,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(10));c.setLayoutParams(p);return c;}
    private TextView title(String s){TextView t=txt(s,16,Color.WHITE);t.setTypeface(null,Typeface.BOLD);t.setPadding(dp(2),dp(3),dp(2),dp(10));return t;}
    private void buildShell(){
        LinearLayout shell=vertical();shell.setBackgroundColor(BG);setContentView(shell);
        LinearLayout top=horizontal();top.setPadding(dp(16),dp(10),dp(12),dp(8));
        LinearLayout brand=vertical();TextView over=txt("RADIOBOSS 12  •  CONSOLA DIGITAL",10,TEAL);over.setTypeface(null,Typeface.BOLD);TextView name=txt("12 ESTÉREO",25,Color.WHITE);name.setTypeface(null,Typeface.BOLD);TextView freq=txt("101.3 FM  ·  CABINA",11,MUTED);brand.addView(over);brand.addView(name);brand.addView(freq);top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        Button stream=btn("◉",0xff20364c);stream.setTextSize(22);stream.setContentDescription("Configuración de transmisión");stream.setOnClickListener(v->showTab(4));top.addView(stream,new LinearLayout.LayoutParams(dp(54),dp(52)));
        shell.addView(top);
        LinearLayout stat=horizontal();stat.setPadding(dp(16),0,dp(16),dp(6));clock=txt("--:--:--",12,TEAL);stat.addView(clock,new LinearLayout.LayoutParams(0,-2,1));headerStatus=txt("● AUDIO LOCAL",10,0xff7bf0b3);stat.addView(headerStatus);shell.addView(stat);
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);body=vertical();body.setPadding(dp(14),dp(4),dp(14),dp(18));sc.addView(body);shell.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        nav=horizontal();nav.setPadding(dp(5),dp(7),dp(5),dp(7));nav.setBackgroundColor(0xff10192a);String[] labels={"Cabina","Botonera","Publicidad","Horarios","Ajustes"};String[] icons={"♫","▦","▣","◷","⚙"};
        for(int i=0;i<labels.length;i++){final int idx=i;LinearLayout cell=vertical();cell.setGravity(Gravity.CENTER);TextView ic=txt(icons[i],18,TEAL);ic.setGravity(Gravity.CENTER);TextView lab=txt(labels[i],9,Color.WHITE);lab.setGravity(Gravity.CENTER);cell.addView(ic);cell.addView(lab);cell.setPadding(0,dp(3),0,dp(3));cell.setOnClickListener(v->showTab(idx));nav.addView(cell,new LinearLayout.LayoutParams(0,-2,1));}shell.addView(nav);
    }
    private void showTab(int tab){activeTab=tab;body.removeAllViews();switch(tab){case 0:buildCabin();break;case 1:buildEffects();break;case 2:buildAds();break;case 3:buildSchedule();break;default:buildSettings();}}
    private void addIntro(String h,String sub){LinearLayout c=card();TextView t=txt(h,20,Color.WHITE);t.setTypeface(null,Typeface.BOLD);c.addView(t);c.addView(txt(sub,12,MUTED));body.addView(c);}
    private void buildCabin(){
        addIntro("EN EL AIRE · CABINA","La música y los efectos usan reproductores independientes.");
        LinearLayout c=card();c.addView(title("♫  CANAL DE MÚSICA"));c.addView(txt(musicPlayer!=null&&musicPlayer.isPlaying()?"Música reproduciéndose":"Selecciona una pista para iniciar",12,MUTED));
        LinearLayout controls=horizontal();Button add=btn("＋ Música",BLUE);add.setOnClickListener(v->choose(MODE_MUSIC));Button stop=btn("■ Detener música",0xff394760);stop.setOnClickListener(v->stopMusic());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),dp(3),dp(3),dp(3));controls.addView(add,p);controls.addView(stop,p);c.addView(controls);
        SeekBar vol=new SeekBar(this);vol.setMax(100);vol.setProgress(75);vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int x,boolean u){if(musicPlayer!=null)musicPlayer.setVolume(x/100f,x/100f);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});c.addView(txt("Volumen de música",11,MUTED));c.addView(vol);c.addView(title("LISTA MUSICAL"));if(music.isEmpty())c.addView(txt("Todavía no has añadido canciones.",12,MUTED));else for(int i=0;i<music.size();i++){final int n=i;LinearLayout r=horizontal();TextView nm=txt("♫  "+music.get(i).name,12,Color.WHITE);r.addView(nm,new LinearLayout.LayoutParams(0,-2,1));Button play=btn("▶",0xff245e56);play.setOnClickListener(v->playMusic(n));r.addView(play,new LinearLayout.LayoutParams(dp(54),dp(44)));c.addView(r);}body.addView(c);
        LinearLayout quick=horizontal();Button fx=btn("▦  Abrir botonera",0xff673ab7);fx.setOnClickListener(v->showTab(1));Button ad=btn("▣  Publicidad",0xffa75c12);ad.setOnClickListener(v->showTab(2));quick.addView(fx,new LinearLayout.LayoutParams(0,-2,1));quick.addView(ad,new LinearLayout.LayoutParams(0,-2,1));body.addView(quick);
    }
    private void buildEffects(){addIntro("BOTONERA DE EFECTOS","Toca un cuadro para lanzarlo encima de la música. No detiene el canal musical.");LinearLayout actions=horizontal();Button add=btn("＋ Añadir efecto",0xff6636ba);add.setOnClickListener(v->choose(MODE_FX));Button stop=btn("■ Parar efectos",0xff394760);stop.setOnClickListener(v->{for(MediaPlayer p:new ArrayList<>(effectPlayers.values()))release(p);effectPlayers.clear();});actions.addView(add,new LinearLayout.LayoutParams(0,-2,1));actions.addView(stop,new LinearLayout.LayoutParams(0,-2,1));body.addView(actions);
        if(effects.isEmpty()){LinearLayout c=card();c.addView(txt("La botonera está vacía. Añade jingles, cortinas o efectos para crear tus cuadros.",13,MUTED));body.addView(c);return;}
        for(int start=0;start<effects.size();start+=3){LinearLayout row=horizontal();for(int i=start;i<Math.min(start+3,effects.size());i++){final int ix=i;AudioItem item=effects.get(i);LinearLayout pad=vertical();pad.setGravity(Gravity.CENTER);pad.setPadding(dp(4),dp(9),dp(4),dp(7));int[] colors={0xff5734a6,0xff125d69,0xff8a4c17,0xff244f9c,0xff8b3155,0xff28624c};pad.setBackground(rounded(colors[i%colors.length],15));TextView icon=txt("▶",20,Color.WHITE);icon.setGravity(Gravity.CENTER);TextView nm=txt(item.name,10,Color.WHITE);nm.setGravity(Gravity.CENTER);nm.setMaxLines(2);pad.addView(icon);pad.addView(nm);pad.setOnClickListener(v->playEffect(ix));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(100),1);lp.setMargins(dp(3),dp(4),dp(3),dp(4));row.addView(pad,lp);}body.addView(row);
            for(int i=start;i<Math.min(start+3,effects.size());i++){final int ix=i;AudioItem item=effects.get(i);LinearLayout volrow=horizontal();TextView n=txt(item.name,10,MUTED);n.setMaxLines(1);volrow.addView(n,new LinearLayout.LayoutParams(dp(95),-2));SeekBar seek=new SeekBar(this);seek.setMax(100);seek.setProgress((int)(item.volume*100));seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int val,boolean u){effects.get(ix).volume=val/100f;MediaPlayer p=effectPlayers.get(ix);if(p!=null)try{p.setVolume(effects.get(ix).volume,effects.get(ix).volume);}catch(Exception ignored){}}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});volrow.addView(seek,new LinearLayout.LayoutParams(0,dp(36),1));body.addView(volrow);}}
    }
    private void buildAds(){addIntro("PUBLICIDAD COMERCIAL","Cuñas separadas de la música, con volumen propio y opción de horario.");Button add=btn("＋ Importar publicidad",0xffb56a17);add.setOnClickListener(v->choose(MODE_AD));body.addView(add);if(ads.isEmpty()){LinearLayout c=card();c.addView(txt("Aquí aparecerán tus cuñas comerciales.",13,MUTED));body.addView(c);}for(int i=0;i<ads.size();i++){final int ix=i;AudioItem a=ads.get(i);LinearLayout c=card();c.addView(txt("▣  "+a.name,14,Color.WHITE));c.addView(txt(a.schedule.isEmpty()?"Sin horario programado":"Todos los días · "+a.schedule,11,MUTED));LinearLayout row=horizontal();Button play=btn(adPlayers.containsKey(ix)?"■ Detener cuña":"▶ Probar cuña",0xff875018);play.setOnClickListener(v->{MediaPlayer current=adPlayers.get(ix);if(current!=null){stopAd(ix);play.setText("▶ Probar cuña");}else{playAd(ix,play);play.setText("■ Detener cuña");}});Button sched=btn("◷ Programar",0xff284b7a);sched.setOnClickListener(v->setSchedule(ix));row.addView(play,new LinearLayout.LayoutParams(0,-2,1));row.addView(sched,new LinearLayout.LayoutParams(0,-2,1));c.addView(row);SeekBar s=new SeekBar(this);s.setMax(100);s.setProgress((int)(a.volume*100));s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int val,boolean u){ads.get(ix).volume=val/100f;MediaPlayer p=adPlayers.get(ix);if(p!=null)try{p.setVolume(ads.get(ix).volume,ads.get(ix).volume);}catch(Exception ignored){}}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});c.addView(txt("Volumen de esta cuña",10,MUTED));c.addView(s);body.addView(c);}}
    private void buildSchedule(){addIntro("PROGRAMACIÓN AUTOMÁTICA","Programa una hora para cada cuña. Esta versión revisa horarios mientras la aplicación permanece abierta.");LinearLayout c=card();c.addView(title("HORARIOS CONFIGURADOS"));boolean any=false;for(AudioItem a:ads)if(!a.schedule.isEmpty()){any=true;c.addView(txt("◷  "+a.schedule+"  —  "+a.name,12,Color.WHITE));}if(!any)c.addView(txt("Todavía no hay horarios. Entra a Publicidad y pulsa Programar.",12,MUTED));body.addView(c);LinearLayout note=card();note.addView(txt("Importante",13,0xffffc46b));note.addView(txt("Para garantizar anuncios aunque Android cierre la aplicación o el teléfono se reinicie, hay que añadir un servicio en segundo plano y alarmas del sistema. Eso se debe probar aparte en el teléfono.",12,MUTED));body.addView(note);}
    private void buildSettings(){addIntro("AJUSTES DE TRANSMISIÓN","SHOUTcast 1 · transmite el micrófono y, con permiso de Android, el audio interno compatible.");LinearLayout c=card();c.addView(title("◉  SERVIDOR SHOUTcast 1"));hostField=field("IP o dirección del hosting");portField=field("Puerto");mountField=field("Punto de montaje (ej. /live)");userField=field("Usuario (si aplica)");passField=field("Contraseña");passField.setInputType(129);c.addView(hostField);c.addView(portField);c.addView(mountField);c.addView(userField);c.addView(passField);android.content.SharedPreferences sp=getSharedPreferences("stream",0);hostField.setText(sp.getString("host",""));portField.setText(sp.getString("port",""));mountField.setText(sp.getString("mount",""));userField.setText(sp.getString("user",""));passField.setText(sp.getString("pass",""));Button save=btn("Guardar configuración",BLUE);save.setOnClickListener(v->{sp.edit().putString("host",hostField.getText().toString()).putString("port",portField.getText().toString()).putString("mount",mountField.getText().toString()).putString("user",userField.getText().toString()).putString("pass",passField.getText().toString()).apply();Toast.makeText(this,"Datos guardados en el teléfono",Toast.LENGTH_SHORT).show();});c.addView(save);streamState=txt("Estado: listo para conectar · codificador MP3 64 kbps",11,MUTED);c.addView(streamState);
        LinearLayout streamButtons=horizontal();Button startStream=btn("▶ INICIAR EMISIÓN",0xff087d68);Button stopStream=btn("■ DETENER",0xff8d3345);streamButtons.addView(startStream,new LinearLayout.LayoutParams(0,-2,1));streamButtons.addView(stopStream,new LinearLayout.LayoutParams(0,-2,1));c.addView(streamButtons);
        startStream.setOnClickListener(v->{
            String h=hostField.getText().toString().trim();String ps=passField.getText().toString();String po=portField.getText().toString().trim();
            if(h.isEmpty()||po.isEmpty()||ps.isEmpty()){Toast.makeText(this,"Completa IP, puerto y contraseña de fuente",Toast.LENGTH_LONG).show();return;}
            if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},77);Toast.makeText(this,"Concede el permiso y vuelve a pulsar INICIAR EMISIÓN",Toast.LENGTH_LONG).show();return;}
            int port;try{port=Integer.parseInt(po);}catch(Exception ex){Toast.makeText(this,"El puerto debe ser numérico",Toast.LENGTH_LONG).show();return;}
            sp.edit().putString("host",h).putString("port",po).putString("mount",mountField.getText().toString()).putString("user",userField.getText().toString()).putString("pass",ps).apply();
            sp.edit().putString("host",h).putString("port",po).putString("mount",mountField.getText().toString()).putString("user",userField.getText().toString()).putString("pass",ps).apply();
            pendingStreamHost=h; pendingStreamPort=port; pendingStreamPassword=ps;
            if(android.os.Build.VERSION.SDK_INT>=29){
                android.media.projection.MediaProjectionManager mpm=(android.media.projection.MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
                streamState.setText("Estado: esperando permiso para capturar audio interno…");
                startActivityForResult(mpm.createScreenCaptureIntent(),REQUEST_SYSTEM_AUDIO_CAPTURE);
            } else {
                startStreamWithProjection(null);
                Toast.makeText(this,"Android 9 o anterior: solo se puede transmitir el micrófono desde esta versión.",Toast.LENGTH_LONG).show();
            }
        });
        stopStream.setOnClickListener(v->{if(streamEngine!=null)streamEngine.stop();if(streamState!=null)streamState.setText("Estado: solicitando desconexión…");if(headerStatus!=null)headerStatus.setText("● AUDIO LOCAL");});
        body.addView(c);
        LinearLayout mic=card();mic.addView(title("MEZCLA DE AUDIO DEL TELÉFONO"));mic.addView(txt("En Android 10 o superior, al iniciar la emisión Android pedirá permiso para capturar audio interno. La app mezcla esa captura compatible con el micrófono y la codifica en MP3. Android no permite capturar absolutamente todo: algunas aplicaciones bloquean la captura, y llamadas, audio protegido/DRM y ciertos sonidos del sistema pueden quedar fuera. La captura interna tampoco garantiza seguir el volumen físico del teléfono.",12,MUTED));body.addView(mic);
        LinearLayout warning=card();warning.addView(txt("Importante para SHOUTcast 1",13,0xffffc46b));warning.addView(txt("Usa el puerto de conexión de DJ/fuente que aparece en el panel, no el puerto público de escucha si son distintos. Android solo deja capturar audio interno compatible: algunas apps, llamadas y audio protegido quedan fuera. No compartas la contraseña en el chat.",12,MUTED));body.addView(warning);
    }
    private void startStreamWithProjection(android.media.projection.MediaProjection projection){
        if(pendingStreamHost==null||pendingStreamPassword==null)return;
        if(streamEngine!=null)streamEngine.stop();
        streamEngine=new ShoutcastMicStreamer();
        streamState.setText("Estado: conectando…");
        streamEngine.start(pendingStreamHost,pendingStreamPort,pendingStreamPassword,projection,this,msg->runOnUiThread(()->{
            if(streamState!=null)streamState.setText("Estado: "+msg);
            if(headerStatus!=null)headerStatus.setText(msg.startsWith("SHOUTcast conectado")?"● TRANSMITIENDO":"● STREAM");
        }));
    }
    private EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setHintTextColor(0xff71819c);e.setTextColor(Color.WHITE);e.setTextSize(14);e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setBackground(rounded(0xff0c1424,10));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.setMargins(0,dp(4),0,dp(5));e.setLayoutParams(p);return e;}
    private void choose(int mode){pickMode=mode;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_AUDIO);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);
        if(req==REQUEST_SYSTEM_AUDIO_CAPTURE){
            if(res==RESULT_OK&&data!=null&&android.os.Build.VERSION.SDK_INT>=29){
                android.media.projection.MediaProjectionManager mpm=(android.media.projection.MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
                android.media.projection.MediaProjection projection=mpm.getMediaProjection(res,data);
                startStreamWithProjection(projection);
            } else {if(streamState!=null)streamState.setText("Estado: permiso de audio interno cancelado");Toast.makeText(this,"Sin ese permiso solo se puede emitir el micrófono. Vuelve a iniciar para intentarlo otra vez.",Toast.LENGTH_LONG).show();}
            return;
        }
        if(req==PICK_AUDIO&&res==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}String n=u.getLastPathSegment();if(n==null)n="Audio";int k=n.lastIndexOf('/');if(k>=0)n=n.substring(k+1);AudioItem a=new AudioItem(u,n);if(pickMode==MODE_MUSIC)music.add(a);else if(pickMode==MODE_FX)effects.add(a);else ads.add(a);showTab(pickMode==MODE_MUSIC?0:pickMode==MODE_FX?1:2);}}
    private MediaPlayer createPlayer(AudioItem a,Runnable done){try{MediaPlayer p=new MediaPlayer();p.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());p.setDataSource(this,a.uri);p.setVolume(a.volume,a.volume);p.setOnPreparedListener(MediaPlayer::start);p.setOnCompletionListener(mp->{release(mp);if(done!=null)done.run();});p.setOnErrorListener((mp,w,e)->{release(mp);return true;});p.prepareAsync();return p;}catch(Exception e){Toast.makeText(this,"No se pudo abrir este audio",Toast.LENGTH_SHORT).show();return null;}}
    private void playMusic(int i){stopMusic();if(i<0||i>=music.size())return;musicPlayer=createPlayer(music.get(i),()->{if(i+1<music.size())playMusic(i+1);});}
    private void playEffect(int i){if(i<0||i>=effects.size())return;MediaPlayer old=effectPlayers.remove(i);if(old!=null)release(old);MediaPlayer p=createPlayer(effects.get(i),()->effectPlayers.remove(i));if(p!=null)effectPlayers.put(i,p);}
    private void playAd(int i){playAd(i,null);}
    private void playAd(int i,Button button){if(i<0||i>=ads.size())return;MediaPlayer existing=adPlayers.get(i);if(existing!=null){stopAd(i);if(button!=null)button.setText("▶ Probar cuña");return;}MediaPlayer p=createPlayer(ads.get(i),()->{adPlayers.remove(i);if(button!=null)runOnUiThread(()->button.setText("▶ Probar cuña"));});if(p!=null)adPlayers.put(i,p);}
    private void stopAd(int i){MediaPlayer p=adPlayers.remove(i);if(p!=null)release(p);}
    private void stopMusic(){if(musicPlayer!=null){release(musicPlayer);musicPlayer=null;}}
    private void release(MediaPlayer p){try{p.setOnCompletionListener(null);p.setOnErrorListener(null);p.stop();}catch(Exception ignored){}try{p.release();}catch(Exception ignored){}}
    private void setSchedule(int i){if(i<0||i>=ads.size())return;TimePickerDialog d=new TimePickerDialog(this,(view,hour,minute)->{ads.get(i).schedule=String.format(Locale.US,"%02d:%02d",hour,minute);lastPlayedMinute.remove(i);showTab(2);},8,0,true);d.setTitle("Hora diaria de la cuña");d.show();}
    private void checkSchedules(){String hm=new SimpleDateFormat("HH:mm",Locale.US).format(new Date());String day=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date());for(int i=0;i<ads.size();i++){AudioItem a=ads.get(i);if(!a.schedule.isEmpty()&&a.schedule.equals(hm)&&!day.equals(lastPlayedMinute.get(i))){lastPlayedMinute.put(i,day);playAd(i);}}}
    private void updateClock(){if(clock!=null)clock.setText(new SimpleDateFormat("EEE, dd MMM · HH:mm:ss",new Locale("es","CO")).format(new Date()));}
    @Override protected void onDestroy(){handler.removeCallbacks(clockTask);if(streamEngine!=null)streamEngine.stop();stopMusic();for(MediaPlayer p:effectPlayers.values())release(p);for(MediaPlayer p:adPlayers.values())release(p);effectPlayers.clear();adPlayers.clear();super.onDestroy();}
}
