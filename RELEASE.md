# Publicar una nueva versión de SpreadVE

## Firma (producción)

Release se firma con el keystore local (no está en Git):

- `keystore/spreadve-release.jks` — clave privada
- `keystore.properties` — contraseñas (ver `keystore.properties.example`)

**Haz backup de ambos en un lugar seguro.** Si los pierdes, no podrás actualizar la app sobre instalaciones existentes.

## 1. Subir versión en Gradle

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

No incluyas `keystore/` ni `keystore.properties`.

## 4. Compilar APK firmado

```bash
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/app-release.apk`.

## 5. Crear release en GitHub

Copia el APK con el nombre público antes de subir (el `#nombre` de `gh` solo pone la etiqueta, no el nombre de descarga):

```bash
cp app/build/outputs/apk/release/app-release.apk SpreadVE.apk
gh release create v1.2.0 SpreadVE.apk --title "SpreadVE 1.2.0" --notes "Notas del release"
```

En `update.json`, `apkUrl` debe coincidir exactamente con el asset subido, por ejemplo:
`https://github.com/codeonyx-dev/spread-ve/releases/download/v1.2.0/SpreadVE.apk`

## Notas

- Los usuarios deben instalar siempre APKs firmados con la **misma clave**.
- Si alguien tiene instalada una build firmada con debug, debe **desinstalar** antes de instalar la firmada con el keystore de producción.
- Tras publicar, espera ~1 minuto a que `update.json` en GitHub esté actualizado antes de probar la notificación.
