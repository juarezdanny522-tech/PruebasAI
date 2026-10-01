# Firma de desarrollo ⚠️

Este directorio contiene un **keystore de desarrollo** (`dev-release.p12`) con clave
pública y conocida (`pruebasai-dev-2026`).

- Sirve para que todos los APKs de CI tengan la **misma firma** y se puedan
  instalar como actualización sin desinstalar la app anterior.
- **NUNCA** se debe usar para publicar en Google Play ni para producción.
- Para una publicación real, genera tu propio keystore y guárdalo como secreto
  de GitHub (no lo subas al repositorio).

```
keytool -genkeypair -v -keystore release.p12 -keyalg RSA -keysize 4096 \
        -validity 10000 -alias pruebasai -storetype PKCS12
```
