# Activity Stats

An Android dashboard and home-screen widgets for your Garmin and Strava workouts, styled after Material 3 Expressive. Free: no accounts, API keys or subscriptions. Everything is read from Health Connect on your phone.

## What's in it

**Widgets.** Each has three styles, and each can be set to Run, Ride or All and a color.

| Widget | Styles | Options |
|---|---|---|
| **Totals** (4×2, resizable) | **Bars** (headline + day or week bars, or today's list) · **Badge** (number in a scalloped badge with a dot per day or week) · **Big number** | Daily, Weekly or Monthly; count, distance or time |
| **Streak** (2×2) | **Badge** (count + last 12 weeks as dots) · **Calendar** (12 weeks as a grid) · **Big number** | |
| **Latest activity** (4×1) | **Pill** · **Card** (distance, time, pace, heart rate) · **Compact** | |

Totals are plain totals; there are no goals.

**App** (bottom navigation):
- **Dashboard:** activity menu, Week / Month / Year button group, distance hero with bars and a vs-same-point-last-period delta, stat tiles in expressive shapes, recent workouts, and a Health Connect card (last sync, which apps contributed, duplicates merged).
- **Activities:** every workout grouped by week with weekly totals.
- **Widgets:** live previews of every widget in each style, with one-tap Add to home screen (adds the style you're previewing).
- **Settings:** Health Connect status, theme (light/dark/system; wallpaper colors or two custom seed colors), units.

**Duplicates.** Garmin Connect and Strava both write the same run to Health Connect. Activity Stats counts it once (start within 2 min, same sport, distance within 3%), keeps the Garmin numbers and carries over the Strava title.

## Setup

1. Open in Android Studio, sync Gradle and run on a phone with Android 12 (API 31) or newer. Android 14+ has Health Connect built in; on 12–13 the app offers to install it.
2. Turn on sharing to Health Connect:
   - **Garmin Connect:** More → Settings → Health Connect → allow.
   - **Strava:** You → Settings → Manage Apps and Devices → Health Connect.
3. In Activity Stats, tap **Allow Health Connect access** and grant everything, including **past data** (totals from before today) and **background access** (widgets update while the app is closed).
4. Open the **Widgets** tab, pick a style and tap **Add**. Long-press a placed widget and tap the pencil to change it.

Data stays on the phone. Activity Stats only reads from Health Connect.

Sideloaded and debug builds can read Health Connect freely. Releasing on Google Play requires Play's Health apps declaration.

## Project layout

```
app/src/main/java/com/wylew/activitystats/
  data/               SettingsStore (DataStore), ActivityStore (JSON cache), ActivityRepository (sync)
  data/healthconnect/ HealthConnectSource (exercise sessions + distance/elevation/HR totals)
  domain/             Activity model, RollupCalculator (daily/weekly/monthly/yearly), Dedupe, Streaks, formatting
  sync/               SyncWorker + SyncScheduler (hourly WorkManager)
  theme/              ColorScience (CIELAB tonal palettes), SchemeGenerator (two seeds → M3 roles), ThemeSettings
  ui/                 AppRoot (tabs), dashboard/, activities/, widgets/ (gallery), settings/, widgetconfig/ (setup + previews)
  ui/components/      CookieShape, RollupBars, ConnectedButtonGroup, ActivityRow
  widget/             Glance widgets (Totals, Streak, Latest), shared palette, per-widget settings
```

## Toolchain

AGP 8.13, Kotlin 2.2, Compose BOM 2025.10 (Material 3 1.4), Glance 1.1, Health Connect 1.1, minSdk 31, target/compile 36. Android Studio may offer upgrades; they should be safe to accept.

Material 3 Expressive components (`MaterialExpressiveTheme`, `MaterialShapes`, `ButtonGroup`) are still internal in stable material3 1.4, so the app draws its own cookie shapes and connected button group. Swap to the official APIs when material3 1.5 goes stable.

## Run tests

```
./gradlew test
```

## Roadmap

- Activity detail screen with splits and a route map
- Room database if the cache grows large
- Roboto Flex for wide, heavy stat numerals
