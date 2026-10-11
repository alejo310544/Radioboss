RADIOBOSS12 — CAPTURA DE AUDIO INTERNO + MICRÓFONO + MP3 + SHOUTcast 1

Qué incorpora esta versión
- Solicita el permiso del sistema Android para capturar reproducción interna (Android 10/API 29 o superior).
- Captura el micrófono y la reproducción interna compatible en paralelo y mezcla ambas señales.
- Ajusta la ganancia del audio interno según el volumen multimedia del teléfono.
- Codifica la mezcla como MP3 mono, 22.05 kHz, 64 kbps y la envía por conexión de fuente SHOUTcast 1.
- Conserva el botón de detener la emisión.

Límites importantes de Android
- No existe una API normal que permita capturar absolutamente todos los sonidos del teléfono.
- AudioPlaybackCapture solo recibe audio de aplicaciones que permiten captura y usan usos compatibles (por ejemplo, MEDIA o GAME).
- Llamadas telefónicas, audio protegido/DRM, algunas apps y algunos sonidos del sistema pueden no entrar en la mezcla.
- La ganancia del audio interno sigue aproximadamente el volumen multimedia; no es una copia exacta de todo el procesamiento de volumen del sistema.
- En Android 9 o anterior solo se transmite el micrófono.
- Para escuchar la señal en el servidor, usa el puerto de fuente/DJ de Listen2MyRadio, no necesariamente el puerto público de escucha.

Prueba
1. Compila desde GitHub Actions con el workflow incluido.
2. Instala el APK en un teléfono Android 10+.
3. En Ajustes, introduce host, puerto de fuente y contraseña de SHOUTcast 1.
4. Pulsa INICIAR EMISIÓN y acepta el diálogo de captura de audio del sistema.
5. Reproduce música en otra aplicación que permita captura; habla cerca del micrófono y verifica la escucha desde otro dispositivo.
6. Cambia el volumen multimedia y confirma cómo afecta al audio interno transmitido.

NOTA: Este entorno no tiene Gradle instalado; la compilación debe verificarse con el workflow de GitHub Actions y después probarse en un teléfono real. La compatibilidad de captura depende del Android, del fabricante y de la app que reproduce audio.
