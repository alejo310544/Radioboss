package com.example.radiobossmobile;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.util.Calendar;

public class MainActivity extends Activity {

    private EditText etServerIp, etPort, etPassword;
    private Button btnToggleStream, btnLoadPlaylist, btnScheduleAd, btnSayTime, btnFx1, btnFx2;
    private ToggleButton btnMicToggle;
    private boolean isStreaming = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Vincular componentes de la interfaz
        etServerIp = findViewById(R.id.etServerIp);
        etPort = findViewById(R.id.etPort);
        etPassword = findViewById(R.id.etPassword);
        btnToggleStream = findViewById(R.id.btnToggleStream);
        btnLoadPlaylist = findViewById(R.id.btnLoadPlaylist);
        btnScheduleAd = findViewById(R.id.btnScheduleAd);
        btnSayTime = findViewById(R.id.btnSayTime);
        btnFx1 = findViewById(R.id.btnFx1);
        btnFx2 = findViewById(R.id.btnFx2);
        btnMicToggle = findViewById(R.id.btnMicToggle);

        // Control de Transmisión streaming (Shoutcast/Icecast para listen2myradio)
        btnToggleStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = etServerIp.getText().toString();
                String port = etPort.getText().toString();
                String pass = etPassword.getText().toString();

                if(ip.isEmpty() || port.isEmpty() || pass.isEmpty()){
                    Toast.makeText(MainActivity.this, "Por favor configura los datos de listen2myradio", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isStreaming) {
                    // Aquí se inicializa la conexión socket con el codificador de audio
                    isStreaming = true;
                    btnToggleStream.setText("Detener Transmisión");
                    Toast.makeText(MainActivity.this, "Conectando al servidor stream...", Toast.LENGTH_SHORT).show();
                } else {
                    isStreaming = false;
                    btnToggleStream.setText("Iniciar Transmisión En Vivo");
                    Toast.makeText(MainActivity.this, "Transmisión finalizada", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Botón de locución horaria automática
        btnSayTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar rightNow = Calendar.getInstance();
                int hour = rightNow.get(Calendar.HOUR_OF_DAY);
                int minute = rightNow.get(Calendar.MINUTE);
                Toast.makeText(MainActivity.this, "Locución: Son las " + hour + " con " + minute + " minutos.", Toast.LENGTH_SHORT).show();
                // Integra un reproductor de voz o pista horaria indexada
            }
        });

        // Simulador de Playlists .m3u, Efectos de sonido y Horarios de pauta comercial
        btnLoadPlaylist.setOnClickListener(v -> Toast.makeText(this, "Explorador de Archivos: Selecciona tu archivo .m3u", Toast.LENGTH_SHORT).show());
        btnScheduleAd.setOnClickListener(v -> Toast.makeText(this, "Módulo de Cuñas: Define las horas de reproducción automática", Toast.LENGTH_SHORT).show());
        btnFx1.setOnClickListener(v -> Toast.makeText(this, "Reproduciendo Efecto: Aplausos", Toast.LENGTH_SHORT).show());
        btnFx2.setOnClickListener(v -> Toast.makeText(this, "Reproduciendo Efecto: Corneta de aire", Toast.LENGTH_SHORT).show());
        
        btnMicToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(MainActivity.this, "Micrófono Activado. Tu voz se mezclará con la música", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Micrófono Silenciado", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
