# Publicar una nueva versión de SpreadVE

## 1. Subir version en Gradle

En `app/build.gradle.kts`:

```kotlin
versionCode = 3        // siempre +1
versionName = "1.2.0"
```

## 2. Actualizar `update.json` (raíz del repo)

```json
{
  "versionCode": 3,
  "versionName": "1.2.0",
  "apkUrl": "https://github.com/codeonyx-dev/spread-ve/releases/download/v1.2.0/SpreadVE.apk",
  "releaseNotes": "Describe los cambios aquí"
}
```

La app consulta este archivo cada 12 h y al abrirse. Si `versionCode` es mayor que el instalado, muestra una notificación **Descargar e instalar**.

## 3. Commit y push

```bash
git add -A
git commit -m "Release v1.2.0"
git push origin main
```

## 4. Compilar APK

```bash
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/app-release.apk`.

## 5. Crear release en GitHub

```bash
gh release create v1.2.0 app/build/outputs/apk/release/app-release.apk#SpreadVE.apk --title "SpreadVE 1.2.0" --notes "Notas del release"
```

## Notas

- Los usuarios deben instalar siempre APKs firmados con la **misma clave**. Hoy release usa la firma debug del proyecto (desarrollo). Para producción, configura un keystore propio.
- Tras publicar, espera ~1 minuto a que `update.json` en GitHub esté actualizado antes de probar la notificación.
