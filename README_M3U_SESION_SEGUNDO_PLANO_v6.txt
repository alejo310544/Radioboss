RadioBoss12 - versión M3U, sesión y música en segundo plano

Base: RadioBoss12_Consola_Multicanal_v2, sin motor SHOUTcast ni mezcla de audio interno.

Cambios incluidos:
- Importar canciones y crear una playlist en el orden de la lista.
- Exportar la lista a un archivo .m3u e importar un .m3u creado por la app.
- Reproducir la lista completa en orden y pasar automáticamente a la siguiente pista.
- Guardar automáticamente canciones, efectos, cuñas, horarios y volúmenes en el teléfono; también hay botón Guardar sesión.
- Servicio de reproducción en primer plano con notificación persistente y bloqueo parcial para mantener la música mientras se cambia de app o se apaga la pantalla.
- Botón HORA que intenta decir la hora en español usando la voz instalada en Android.
- No incluye conexión SHOUTcast ni motor de mezcla.

Importante sobre M3U:
- Para que una lista se pueda reabrir de forma fiable, este proyecto guarda las URI content:// de los audios elegidos mediante el selector de Android. Las rutas absolutas de un M3U creado en un PC (por ejemplo C:\\Musica\\tema.mp3) no apuntan automáticamente a archivos del teléfono; esos audios deben importarse/seleccionarse en Android.
- El archivo .m3u es una lista de referencias a los audios, no comprime ni contiene los archivos MP3.

Límites y pruebas:
- La música usa un servicio Android en primer plano y debería continuar al cambiar de app. Algunos fabricantes pueden detener procesos por ahorro de batería.
- La botonera de efectos y los anuncios de prueba siguen siendo reproductores de la pantalla principal; sus horarios no están garantizados si Android cierra la actividad. Este cambio no convierte todavía toda la automatización de anuncios en un servicio de fondo.
- El botón HORA usa TextToSpeech instalado en el dispositivo y reproduce la voz localmente; no transmite al servidor.
- El ZIP no se ha compilado en este entorno porque no hay Gradle instalado. Ejecutar GitHub Actions (workflow build-apk.yml) y comprobar el resultado en el teléfono antes de usarlo al aire.
