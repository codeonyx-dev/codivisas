# BCV Aquí

App Android 100% nativa (Kotlin + Jetpack Compose) que muestra las tasas oficiales del Banco Central de Venezuela (BCV), las compara con Binance P2P USDT/VES y el mercado paralelo, e incluye convertidor, widget, notificaciones y calculadora de arbitraje.

## Requisitos

- Android Studio Ladybug (2024.2) o superior
- JDK 17
- Android SDK 35
- Dispositivo/emulador con API 26+

## Cómo abrir y ejecutar

1. Clona o abre esta carpeta en Android Studio.
2. Espera a que Gradle sincronice las dependencias.
3. Conecta un dispositivo o inicia un emulador.
4. Pulsa **Run** (▶) sobre el módulo `app`.

Desde terminal (con Gradle wrapper):

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Fuentes de datos

| Mercado | Fuente | API | Autenticación |
|---------|--------|-----|---------------|
| BCV oficial | bcv.today | `GET https://bcv.today/api/v1/rate.json` | No |
| Paralelo | DolarAPI VE | `GET https://ve.dolarapi.com/v1/dolares` | No |
| Binance P2P | Binance (interno) | `POST https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search` | No |
| Cotizave (opcional) | Cotizave | `GET https://api.cotizave.com/v1/fx/rates` | Sí (API key) |

Monedas BCV soportadas: **USD, EUR, CNY, TRY, RUB**.

## Configurar API keys (opcional)

### Cotizave

Si quieres datos agregados adicionales de Cotizave, agrega tu clave en `local.properties`:

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
COTIZAVE_API_KEY=ctz_live_tu_clave_aqui
```

Sin clave, la app funciona igual con las fuentes gratuitas.

## Cambiar endpoints o intervalos

Edita `app/src/main/java/com/codeonyx/bcvaqui/data/ApiConfig.kt`:

```kotlin
const val BCV_RATE_URL = "https://bcv.today/api/v1/"
const val WIDGET_UPDATE_INTERVAL_MINUTES = 30L   // mínimo WorkManager: 15
const val DEFAULT_BACKGROUND_INTERVAL_HOURS = 4L
```

Los intervalos de notificación se configuran desde **Ajustes** en la app (1, 2, 4, 6, 12, 24 h o desactivado).

## Funcionalidades

### Pantalla principal
- Tasa grande con 2 decimales (`1 USD = 785.07 VES`)
- Selector de moneda (USD, EUR, CNY, TRY, RUB)
- Botón **Copiar tasa**
- Convertidor entre monedas BCV y VES
- Mensaje amigable sin internet + última tasa en caché

### Dashboard multi-fuente
- BCV oficial vs Binance P2P vs Paralelo
- Columna de diferencia porcentual vs BCV

### Calculadora de arbitraje
- Compra en un mercado, vende en otro
- Ganancia en VES y porcentaje

### Widget
- Muestra la moneda preferida (`USD: 785.07 VES`)
- Se actualiza cada ~30 min y al abrir la app
- Tap abre la app

### Notificaciones
- Cambios de tasa (subió/bajó) con valor anterior y nuevo
- Alertas de desviación configurable (ej. ±5% vs BCV)
- WorkManager en segundo plano

## Estructura del proyecto

```
app/src/main/java/com/codeonyx/bcvaqui/
├── data/
│   ├── ApiConfig.kt          # URLs e intervalos
│   ├── api/                  # Retrofit clients
│   ├── model/                # DTOs y modelos
│   ├── local/                # DataStore
│   └── repository/           # Capa de datos
├── domain/FxCalculator.kt    # Conversión y arbitraje
├── ui/                       # Compose screens
├── widget/                   # Home screen widget
├── worker/                   # WorkManager
└── notification/             # Notificaciones locales
```

## Notas legales

Las tasas BCV provienen de fuentes públicas que reflejan datos del Banco Central de Venezuela. Binance P2P y el dólar paralelo son referencias de mercado, **no oficiales**. Esta app es informativa; verifica siempre antes de operaciones financieras.

## Licencia

Proyecto personal — libre para modificar y usar.
