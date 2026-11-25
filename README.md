# Comunidad Conectada
Aplicación nativa Android Java, vistas XML y Gradle Groovy. Mínimo Android 7 (API 24).

## Compilar
Usar JDK 17, SDK Android 35 y Gradle Wrapper 8.9. Abrir con Android Studio o ejecutar `./gradlew assembleEmulatorDebug testEmulatorDebugUnitTest lintEmulatorDebug`.
El flavor `emulator` usa exclusivamente Firebase local (`10.0.2.2:9099` Auth, `10.0.2.2:9000` RTDB, proyecto `demo-comunidad`). No envía FCM real.
El flavor `production` exige un proyecto Firebase propio: registrar `cl.aik1a.comunidadconectada`, copiar `google-services.json` a `app/` (ignorado por Git), habilitar email/contraseña y Realtime Database. Sin configuración, muestra un error y no inicia servicios. No habilitar facturación.

El APK local se genera en `app/build/outputs/apk/emulator/debug/`. No versionar APK, configuración Firebase, claves administrativas ni datos locales.
