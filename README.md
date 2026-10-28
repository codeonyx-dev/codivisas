# Codivisas

App Android 100% nativa (Kotlin + Jetpack Compose) que muestra las tasas oficiales del Banco Central de Venezuela (BCV) y Binance P2P USDT/VES, e incluye convertidor, widgets, notificaciones y utilidades de pago.

## Requisitos

- Android Studio Ladybug (2024.2) o superior
- JDK 17
- Android SDK 35
- Dispositivo/emulador con API 26+

## CÃ³mo abrir y ejecutar

1. Clona o abre esta carpeta en Android Studio.
2. Espera a que Gradle sincronice las dependencias.
3. Conecta un dispositivo o inicia un emulador.
4. Pulsa **Run** (â–¶) sobre el mÃ³dulo `app`.

Desde terminal (con Gradle wrapper):

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Fuentes de datos

| Mercado | Fuente | API | AutenticaciÃ³n |
|---------|--------|-----|---------------|
| BCV oficial | bcv.today | `GET https://bcv.today/api/v1/rate.json` | No |
| Binance P2P | Binance (interno) | `POST https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search` | No |

Monedas BCV soportadas: **USD, EUR, CNY, TRY, RUB**.

## Cambiar endpoints o intervalos

Edita `app/src/main/java/com/codeonyx/bcvaqui/data/ApiConfig.kt`:

```kotlin
const val BCV_RATE_URL = "https://bcv.today/api/v1/"
const val WIDGET_UPDATE_INTERVAL_MINUTES = 30L   // mÃ­nimo WorkManager: 15
const val DEFAULT_BACKGROUND_INTERVAL_HOURS = 4L
```

Los intervalos de notificaciÃ³n se configuran desde **Ajustes** en la app (1, 2, 4, 6, 12, 24 h o desactivado).

## Funcionalidades

### Pantalla principal
- Tasa grande con 2 decimales (`1 USD = 785.07 VES`)
- Selector de moneda (USD, EUR, CNY, TRY, RUB)
- BotÃ³n **Copiar tasa**
- Convertidor entre monedas BCV y VES
- Mensaje amigable sin internet + Ãºltima tasa en cachÃ©

### Widget
- Muestra la moneda preferida (`USD: 785.07 VES`)
- Se actualiza cada ~30 min y al abrir la app
- Tap abre la app

### Notificaciones
- Cambios de tasa (subiÃ³/bajÃ³) con valor anterior y nuevo
- Alertas de desviaciÃ³n configurable (ej. Â±5% vs BCV)
- WorkManager en segundo plano

## Estructura del proyecto

```
app/src/main/java/com/codeonyx/bcvaqui/
â”œâ”€â”€ data/
â”‚   â”œâ”€â”€ ApiConfig.kt          # URLs e intervalos
â”‚   â”œâ”€â”€ api/                  # Retrofit clients
â”‚   â”œâ”€â”€ model/                # DTOs y modelos
â”‚   â”œâ”€â”€ local/                # DataStore
â”‚   â””â”€â”€ repository/           # Capa de datos
â”œâ”€â”€ domain/FxCalculator.kt    # ConversiÃ³n y cÃ¡lculos
â”œâ”€â”€ ui/                       # Compose screens
â”œâ”€â”€ widget/                   # Home screen widget
â”œâ”€â”€ worker/                   # WorkManager
â””â”€â”€ notification/             # Notificaciones locales
```

## Notas legales

Las tasas BCV provienen de fuentes pÃºblicas que reflejan datos del Banco Central de Venezuela. Binance P2P es referencia de mercado, **no oficial**. Esta app es informativa; verifica siempre antes de operaciones financieras.

## Licencia

Proyecto personal â€” libre para modificar y usar.
