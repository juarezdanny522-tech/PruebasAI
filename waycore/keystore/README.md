# Keystore de desarrollo

`dev-release.p12` es un keystore de SOLO DESARROLLO para firmar el APK de release
en CI. Alias `pruebasai`.

**No usar para publicación en Play Store.** Genera uno nuevo para producción:

```bash
keytool -genkeypair -v -keystore release.p12 -storetype PKCS12 \
  -alias waycore -keyalg RSA -keysize 2048 -validity 10000
```
