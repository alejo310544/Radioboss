# RadioVoz Móvil (base inicial Flutter)

## Qué incluye esta primera base
- Reproductor local de audio y avance automático entre pistas.
- Importación básica de playlists M3U/M3U8.
- Botonera de efectos: añadir archivos y renombrar botones (mantén pulsado).
- Listado de cuñas/comerciales.
- Pantalla para organizar eventos por hora.
- Pantalla para guardar datos del servidor de streaming.
- Workflow de GitHub Actions que compila un APK Android y lo publica como artefacto.

## Importante: funciones pendientes
Esta es una base funcional para seguir construyendo, no una automatización de emisión profesional completa. La transmisión real a Listen2MyRadio, captura/mezcla del micrófono, ducking real del micrófono sobre el flujo transmitido, ejecución fiable en segundo plano y ejecución automática de horarios aún necesitan integración y pruebas específicas. La etiqueta “AL AIRE” solo indica reproducción local y no confirma transmisión.

## Cómo usarlo
1. Sube todos los archivos de este proyecto a la raíz de tu repositorio GitHub.
2. Comprueba que `lib/main.dart`, `pubspec.yaml`, `android/` y `.github/workflows/main.yml` están en las rutas indicadas.
3. Haz commit/push a `main` o ejecuta manualmente el workflow desde Actions.
4. Cuando termine, abre el workflow correcto y descarga el artefacto `RadioVoz-Movil-APK`.

## No subas contraseñas
No guardes contraseñas reales del servidor en archivos versionados ni en el repositorio. La app de prueba las guarda localmente en el teléfono.
