RADIOBOSS 12 — motor de codificación y fuente SHOUTcast 1 (v4)

Esta versión agrega una primera ruta de emisión real:
- captura el micrófono del teléfono con AudioRecord;
- codifica PCM a MP3 CBR de 64 kbps usando TAndroidLame/LAME;
- abre una conexión TCP al puerto de fuente/DJ SHOUTcast 1;
- envía la contraseña de fuente y los encabezados de estación;
- incorpora controles INICIAR EMISIÓN y DETENER en Ajustes.

CONFIGURACIÓN
1. En Ajustes introduce el host/IP y el puerto de FUENTE/DJ del panel Listen2MyRadio.
2. Introduce la contraseña de fuente (no la contraseña de escucha ni la del panel, salvo que el proveedor indique que son la misma).
3. Guarda y pulsa INICIAR EMISIÓN. Concede permiso al micrófono cuando Android lo solicite.
4. Mira el texto de estado para ver si conecta o si el servidor rechaza los datos.

IMPORTANTE — LIMITACIÓN HONESTA
Este motor inicial transmite el MICRÓFONO solamente. La música, los efectos y las cuñas del reproductor actual todavía se escuchan localmente y no se enrutan al flujo de MP3. Para una consola completa hace falta sustituir o complementar los reproductores actuales por decodificadores PCM y alimentar sus señales al mezclador común (con volumen independiente, ducking/Talk Over y automatización) antes de codificar. No se debe anunciar la app como consola de emisión completa todavía.

SHOUTcast 1 normalmente autentica con contraseña de fuente y no usa mount para seleccionar el stream. Usa el puerto de conexión de fuente que muestre el panel; puede ser distinto del puerto público de escucha. El comportamiento concreto depende de la configuración de Listen2MyRadio.

Dependencia de codificación: com.github.naman14:TAndroidLame:1.1 (licencia GPL-3.0). El proyecto requiere acceso a JitPack durante la compilación. No se ha podido ejecutar Gradle en este entorno, por lo que el APK debe validarse con GitHub Actions.
