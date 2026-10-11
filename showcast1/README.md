# Showcast 1 — proyecto Android Java

Proyecto Android nativo en Java. Configuración de compilación elegida para CI: Android Gradle Plugin 8.13.2, Gradle 8.13, JDK 17 y compileSdk 36.

## Funciones incluidas en esta versión inicial
- Importar audio del teléfono a una playlist.
- Reproducción, pausa y siguiente (la selección de siguiente en esta versión es básica).
- Ocho botones de audio con opción de cargar un archivo en cada botón.
- Reloj digital.
- Solicitud de permiso para micrófono.
- Pantalla de configuración SHOUTcast v1: dirección, puerto, contraseña y nombre de emisora.

## Importante: funciones aún no implementadas
Esta es una base inicial compilable, no una automatización radial completa. La transmisión online real, el envío del micrófono, la mezcla Talk Over con atenuación automática, el guardado persistente de la configuración y la edición completa de nombres/colores de pads aún necesitan desarrollo e integración. El botón de transmisión avisa esta limitación y no finge conectarse.

## Compilar en GitHub
1. Sube el contenido de este ZIP a un repositorio GitHub.
2. Entra a Actions y ejecuta `Compilar Showcast 1` (o haz un push a `main`).
3. Cuando termine, descarga el artefacto `Showcast1-debug-apk`.

El flujo instala Gradle 8.13 y Java 17. No se incluye `gradlew` ni el JAR del wrapper; GitHub Actions instala Gradle directamente.
