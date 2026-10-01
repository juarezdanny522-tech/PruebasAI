# PruebasAI 🤖 — Asistente de IA 100% local para Android

App de Android que ejecuta un **modelo de inteligencia artificial completamente en tu móvil**:
sin nube, sin cuentas, sin enviar tus datos a ningún servidor.

- 🧠 Motor: **LiteRT-LM** de Google (el mismo que usa Google AI Edge Gallery)
- 📲 **El modelo se descarga desde la propia app** (catálogo integrado de Hugging Face o URL personalizada)
- 💬 Chat con respuestas en streaming, pensamiento visible, parada en caliente
- ⚙️ Temperatura, top-p, top-k, longitud de contexto, hilos de CPU y aceleración GPU
- 📴 Una vez descargado el modelo, funciona **sin conexión a internet**
- 🔧 APK compilado automáticamente por **GitHub Actions** en cada cambio

---

## 📥 Cómo conseguir el APK (gracias a GitHub)

1. Abre la pestaña **Actions** de este repositorio → workflow **Android CI** → la última ejecución verde.
2. En la parte inferior de la ejecución, sección **Artifacts**, descarga `PruebasAI-APKs`.
3. O más fácil: abre la pestaña **Releases** → descarga **`PruebasAI.apk`** de la prerelease `dev-build`.

También se puede lanzar una compilación a mano: botón **Run workflow** en Actions.

> ⚠️ El APK se firma con una [clave de desarrollo](keystore/README.md) (solo para pruebas personales).
> Para Google Play hay que generar un keystore propio.

### Instalar en el móvil

1. Copia `PruebasAI.apk` al teléfono y ábrelo.
2. Acepta «instalar de orígenes desconocidos» para tu navegador/gestor de archivos.
3. Abre **PruebasAI**.

*(Requiere Android 12 o superior.)*

## 📦 Cómo usar la app

1. Ve a la pestaña **Modelos** y descarga uno:
   - **Gemma 3 270M** (304 MB) → para probar en cualquier móvil
   - **Qwen 2.5 1.5B** (1.5 GB) → muy buen español
   - **Gemma 4 E2B** (2.4 GB) → **recomendado** (calidad/velocidad)
   - **Gemma 4 E4B** (3.4 GB) → máxima calidad (móviles con 8 GB+ de RAM)
2. Pulsa **Cargar** en el modelo descargado.
3. ¡Chatea! La respuesta aparece token a token.

Los modelos se guardan en el almacenamiento interno de la app
(`Android/data/com.pruebasai.app/files/models/`) y se pueden borrar desde la app.

## 🛠️ Compilar desde el código

```bash
./gradlew assembleDebug assembleRelease
```

- APK debug: `app/build/outputs/apk/debug/app-debug.apk`
- APK release (firmado con la clave de desarrollo): `app/build/outputs/apk/release/app-release.apk`

Requisitos: JDK 17 y Android SDK (compileSdk 36). El resto lo descarga Gradle.

## 🧪 Tecnología

| Pieza | Detalle |
|---|---|
| Motor de IA | [`com.google.ai.edge.litertlm:litertlm-android`](https://github.com/google-ai-edge/LiteRT-LM) 0.17.1 |
| Modelos | [`litert-community`](https://huggingface.co/litert-community) en Hugging Face (formato `.litertlm`) |
| UI | Kotlin + Jetpack Compose (Material 3) |
| Descargas | `DownloadManager` (sigue descargando en segundo plano y reanuda cortes) |
| CI | GitHub Actions → APK como artefacto y en Releases |

## 📝 Notas

- Elige la variante del modelo según tu móvil: los archivos «universales» (sin sufijos) van bien en CPU y GPU; los `*-gpu` están optimizados para GPU.
- Si el modelo va lento, baja la **longitud de contexto** o sube los **hilos de CPU** en Ajustes.
- La app solo usa internet para **descargar** los modelos. Todo el razonamiento ocurre en el dispositivo.

## WayCore (Karbys + WayHat) incluido

Este repo también contiene **WayCore v0.7.0** en la carpeta `waycore/`: la app de los lentes inteligentes WayHat con su asistente **Karbys** y **IA 100% local (LiteRT-LM)** — modelos descargables desde la app, modos Automático/Local/Gemini y control de WayHat por function calling. GitHub Actions compila `WayCore.apk` junto con los APK de PruebasAI en cada push. Más detalles en `waycore/README.md`.
