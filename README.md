# Comunidad Conectada
Aplicación nativa Android Java, vistas XML y Gradle Groovy. Mínimo Android 7 (API 24).

## Compilar
Usar JDK 17, SDK Android 35 y Gradle Wrapper 8.9. Abrir con Android Studio o ejecutar `./gradlew assembleEmulatorDebug testEmulatorDebugUnitTest lintEmulatorDebug`.
El flavor `emulator` usa exclusivamente Firebase local (`10.0.2.2:9099` Auth, `10.0.2.2:9000` RTDB, proyecto `demo-comunidad`). No envía FCM real.
El flavor `production` exige un proyecto Firebase propio: registrar `cl.aik1a.comunidadconectada`, copiar `google-services.json` a `app/` (ignorado por Git), habilitar email/contraseña y Realtime Database. Sin configuración, muestra un error y no inicia servicios. No habilitar facturación.

El APK local se genera en `app/build/outputs/apk/emulator/debug/`. No versionar APK, configuración Firebase, claves administrativas ni datos locales.

## Firebase local y pruebas

Desde `backend/`:

```sh
npm ci
npm test
npm run test:integration
npx firebase emulators:start --project demo-comunidad --config ../firebase.json --only auth,database
```

Con los emuladores ejecutándose, en otra consola del mismo equipo:

```sh
FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_DATABASE_EMULATOR_HOST=127.0.0.1:9000 node seed-emulator.js
```

Crea dos cuentas ficticias, vecino y administrador, ambas en vivienda 311A. Sus credenciales se guardan únicamente en `backend/.emulator-data/credentials.json`, ignorado por Git. La asignación de administrador ocurre mediante Admin SDK confiable, nunca desde el registro Android. El registro de nuevos vecinos permite varios UID por vivienda. Recuperación de contraseña local: el enlace aparece en la consola de Auth Emulator; no envía correo real.

Ejecutar `./gradlew connectedEmulatorDebugAndroidTest` con un emulador Android iniciado, sin sesión previa. Para demostrar comunicación, abrir dos dispositivos virtuales contra el mismo Firebase local. El alias `10.0.2.2` corresponde al equipo anfitrión del emulador; un teléfono físico necesita configuración local de red específica y no se incluye como endpoint público.

Si el entorno corporativo exporta HTTP_PROXY/HTTPS_PROXY, los clientes RTDB Node pueden intentar pasar conexiones locales por ese proxy. Ejecutar las pruebas con esas variables retiradas únicamente para el proceso local (`env -u HTTP_PROXY -u HTTPS_PROXY -u http_proxy -u https_proxy npm run test:integration`).

## Firebase real sin activar facturación

1. Crear un proyecto Firebase exclusivo y registrar el applicationId indicado. Habilitar email/contraseña y crear RTDB en modo cerrado. Descargar la configuración Android después de crear RTDB para incluir `firebase_url`.
2. Copiar el archivo externo a `app/google-services.json`. Publicar `database.rules.json` en la instancia correcta mediante consola o Firebase CLI. Usar `./gradlew assembleProductionDebug`.
3. Registrar dos cuentas ficticias desde la app. Desde la consola RTDB, cambiar exclusivamente `users/UID/role` a `admin` en una cuenta. El registro asigna siempre `neighbor` en `comunidad01`; esta es una comunidad de demostración abierta a registros, no un sistema de invitaciones.
4. Activar la API FCM HTTP v1 si el proyecto lo requiere. Obtener credenciales administrativas solo en el equipo confiable, fuera del repositorio. No incluirlas en Android, logs ni mensajes. No desplegar Cloud Functions ni habilitar un plan de pago.
5. Ejecutar el emisor local con Node 20+ y Application Default Credentials:

```sh
cd backend
npm ci
export GOOGLE_APPLICATION_CREDENTIALS=/ruta/privada/service-account.json
export FIREBASE_DATABASE_URL=https://TU-INSTANCIA.firebasedatabase.app
npm start
```

La URL debe ser la de tu instancia RTDB (puede terminar en `firebaseio.com` según región). Las credenciales requieren acceso administrativo a esa instancia y envío FCM. Mantener el proceso abierto durante la demostración; no ofrece envío cuando el equipo está apagado.

El emisor escucha anuncios desde su primer arranque; omite el historial anterior. Conserva `notification-state.json` y el mismo directorio entre reinicios. No ejecutar varios emisores con archivos de estado distintos: el bloqueo local evita competencia solo sobre el mismo archivo. Antes de un envío registra el intento en disco. Si el proceso cae o el transporte falla después de iniciar el envío, su resultado queda incierto y no se reenvía automáticamente. Inspeccionar el estado antes de cualquier reintento manual; esta decisión evita duplicados a costa de posibles notificaciones omitidas. La persistencia RTDB del anuncio es independiente.

FCM usa mensajes de datos con `announcementId` y `communityId`, sin título/contenido en el payload. El servicio Android vuelve a consultar el perfil y el anuncio con la sesión activa antes de mostrar una notificación. Al tocarla, la actividad vuelve a validar comunidad y acceso. El permiso Android 13+ se solicita al entrar; si se deniega, los anuncios siguen disponibles en la lista. `force-stop`, políticas de batería y ausencia de Google Play Services pueden impedir recibir FCM. Probar los estados reales; no se garantiza entrega a todos.

Los tokens son privados por UID/instalación. Se registran al iniciar sesión/refrescar y se eliminan antes de cerrar sesión. Si falla esa eliminación, se solicita reintentar el cierre con conexión. El APK no contiene claves Admin SDK.

## Datos y permisos

`users/UID` contiene nombre, vivienda, comunidad y rol. Solo el dueño lee ese perfil; puede cambiar su nombre. Vivienda, comunidad y rol quedan inmutables desde el cliente. Chat, productos y anuncios guardan identidad y fecha de servidor. Los anuncios y publicaciones son inmutables en esta versión. Las reglas validan autoría, campos y membresía, y deniegan por defecto las demás rutas.

Las listas consultan los últimos 50 registros mediante `orderByChild('createdAt').limitToLast(50)`; las reglas rechazan lecturas de listas sin ese límite. Un anuncio concreto permite lectura a miembros para abrir notificaciones. Al cerrar una pantalla se retira su listener. Un envío pendiente conserva su operación mientras el proceso sigue vivo y no puede enviarse otra vez desde la misma cuenta/canal hasta completarse. Las pruebas Node con Emulator Suite verifican permisos directamente, fuera de la interfaz.
