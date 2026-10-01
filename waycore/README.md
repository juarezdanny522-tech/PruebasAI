# WayCore + WayHat v0.7.0 — Karbys con IA 100% local

WayCore es la app de los lentes inteligentes **WayHat**. **Karbys** es su cerebro: una asistente de voz en español que puede responder y controlar los lentes. Desde esta versión, Karbys puede pensar **dentro del teléfono** con modelos de lenguaje de Google AI Edge (LiteRT-LM): funciona **sin internet y sin clave API**. También sigue funcionando con Gemini en la nube.

El firmware está en `WayHat_v6_WayCore.ino` (ESP32-S3, Bluetooth SPP, bocina MAX98357A, micrófono INMP441, sensores HC-SR04 y TF-Luna, pantalla braille).

## Novedades de la app v0.7.0

- **IA local dentro del teléfono (LiteRT-LM)**: Karbys puede responder con modelos que se ejecutan en el teléfono. Sin internet, sin nube y sin costo por consulta.
- **Descarga de modelos desde la app**: en la sección **CEREBRO DE KARBYS** puedes descargar el modelo que prefieras; queda guardado en el teléfono y se puede cambiar o volver a descargar cuando quieras:
  - `Gemma 3 270M` (304 MB): la más ligera, respuestas cortas.
  - `Qwen2.5 1.5B` (1.6 GB): excelente en español, recomendada si tienes espacio.
  - `Gemma 4 E2B` (2.6 GB): la más capaz (recomendada en teléfonos potentes).
- **Tres modos de pensamiento**:
  - **Automático** (por defecto): usa el modelo local si está listo y recurre a Gemini solo si hace falta.
  - **Local**: solo el modelo del teléfono.
  - **Gemini**: nube con tu clave API.
- **Karbys controla WayHat también con IA local**: las mismas herramientas seguras de siempre (sensibilidad, modo SAFE/CHAT, avisos sonoros, prueba del buzzer, telemetría) están disponibles para el modelo local mediante function calling de LiteRT-LM.
- Todo lo de v0.6.0 se mantiene: palabra clave confiable, "¿Dime?" con voz, clave API configurable dentro de la app.

## Novedades de la app v0.6.0

- **Clave API dentro de la app**: la primera vez que abres WayCore te pide la
  clave API de Gemini (ahora opcional: puedes usar la IA local sin clave); la
  guardas una vez y Karbys funciona sola (queda en el almacenamiento privado
  del teléfono). Puedes cambiarla con el botón CAMBIAR CLAVE API.
- **"¿Dime?" en lugar del pitido**: cuando Karbys te va a escuchar (por palabra
  clave, por el botón HABLAR o después de responderte), lo anuncia con su voz
  diciendo "¿Dime?" en vez del antiguo sonidito.
- **Palabra clave más confiable**: ahora acepta variantes de pronunciación
  ("Oye Karbys", "Hey Karbis", "Her karbys", e incluso solo "Karbys"), usa
  coincidencia difusa para errores del reconocedor y se reinicia sola si el
  reconocedor se queda colgado.
- **Comando en una sola frase**: puedes decir "Oye Karbys, ¿qué hora es?" y
  Karbys atiende la pregunta de inmediato.
- **Pausa por voz corregida**: "Pausa Karbys" ahora pausa de verdad, y decir
  "Oye Karbys" la reactiva.

## Descargar el APK

El APK se compila automáticamente con GitHub Actions en cada cambio y se
publica en [Releases](https://github.com/juarezdanny522-tech/PruebasAI/releases)
de este repo. Descarga `WayCore.apk` (arm64-v8a, para tu teléfono) en Android e
instálalo. La rama `apk-latest` de waycore30 siempre tiene el APK más reciente
con checksums `SHA256SUMS.txt`.

Los modelos de IA se descargan **desde dentro de la app** (botón BAJAR) — hace
falta internet solo la primera vez; después, todo funciona en el teléfono.

## Funciones

- Karbys recibe en cada consulta un contexto fresco con:
  - HC-SR04 derecho, izquierdo y trasero.
  - TF-Luna.
  - Distancia más cercana.
  - Temperatura y humedad DHT11.
  - Estado de cada sensor.
  - Sensibilidad actual.
  - Modo SAFE/CHAT.
  - Estado de avisos sonoros.
  - Estado de conexión de WayHat.
  - Batería del teléfono.
  - Ubicación y antigüedad de la lectura GPS.
  - Hora local.
- Los datos de sensores son tratados como fuente de verdad: Karbys no debe inventar valores.
- Karbys (local o Gemini) puede usar Function Calling para controlar, de forma limitada y validada:
  - sensibilidad de WayHat (20–150 cm),
  - modo SAFE/CHAT,
  - avisos sonoros,
  - prueba del buzzer,
  - actualización inmediata de telemetría.
- Todos los comandos de hardware tienen una lista blanca y confirmación desde el ESP32.
- El enlace Bluetooth continúa siendo independiente de reconocimiento de voz y TTS.
- WayHat mantiene su seguridad local aunque la IA o Internet no estén disponibles.

## Modelos locales (información técnica)

Se usan modelos de [litert-community en Hugging Face](https://huggingface.co/litert-community) convertidos a formato `.litertlm` con el motor [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) (`litertlm-android:0.17.1`). La app los descarga con DownloadManager y los carga con la API de generación de Google AI Edge (samplers k40/p0.95/t0.7, contexto de 4096 tokens).

## API key

Con la IA local **no necesitas ninguna clave**: al abrir la app por primera vez
elige "USAR IA LOCAL SIN CLAVE" y descarga un modelo desde la sección CEREBRO
DE KARBYS.

Si prefieres Gemini en la nube, la app te pide la clave API de Gemini la
primera vez que se abre y la guarda en el teléfono; no hace falta configurar
nada más.

Opcional para quien compile el proyecto: también puedes incrustar la clave en
el build creando `local.properties` con:

`GEMINI_API_KEY=TU_CLAVE`

(o definiendo el secreto `GEMINI_API_KEY` en GitHub Actions). Si la clave está
incrustada, la app la usa directamente sin pedir nada.

## Bluetooth

El teléfono debe tener `WayHat-Karbys` vinculado. WayCore abre la conexión SPP automáticamente usando el UUID estándar de Bluetooth Classic.

## Compilar con GitHub Actions

En cada push GitHub Actions compila los APK de WayCore (arm64-v8a y x86_64) y
los publica en la release `dev-build` de este repo junto con los de PruebasAI.
