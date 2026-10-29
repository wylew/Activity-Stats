# Activity Stats

An Android dashboard and home-screen widgets for your Garmin and Strava workouts, styled after Material 3 Expressive. Free: no accounts, API keys or subscriptions. It reads from Health Connect on your phone.

- **Widgets:** one configurable roll-up widget. For each widget you choose the activity (**Run**, **Ride** or **All**), **Daily** or **Weekly**, the headline number (count, distance or time) and a color. At 2×2 it shows the headline number; at 4×2 and up it adds day bars (weekly) or today's list (daily). Colors follow your wallpaper (Material You).
- **Dashboard:** Week / Month / Year roll-ups with a distance hero card, a comparison against the same point last period, moving time, pace, elevation, heart rate, a goal bar and recent activities.
- **Theming:** Settings → Theme. Pick System, Light or Dark, then either **Match wallpaper** (Material You) or **Pick your own colors**: two seed colors (primary and accent) generate full light and dark Material 3 schemes, with presets like Holiday (red + green). The app, system bars and widgets all follow it.
- **Data:** Health Connect, which Garmin Connect and Strava both write to. When the same run arrives from both, it's counted once (the Garmin copy wins; a Strava title is kept). Refreshed hourly and on open. The Strava API is supported as an optional extra for people with a Strava subscription.

Design mockups live on the "Rollup" canvas in Claude.

## Setup

1. Open in Android Studio, sync Gradle and run on a phone with Android 12 (API 31) or newer. Android 14+ has Health Connect built in; on 12–13 the app offers to install it.
2. Turn on sharing to Health Connect:
   - **Garmin Connect:** More → Settings → Health Connect → allow.
   - **Strava:** You → Settings → Manage Apps and Devices → Health Connect.
3. In Activity Stats, tap **Allow Health Connect access** and grant everything, including **past data** (for totals before today) and **background access** (so widgets update while the app is closed).
4. Long-press the home screen → Widgets → Activity Stats → **Activity roll-up**.

Data stays on the phone. Activity Stats only reads from Health Connect.

### Optional: Strava API

Only if you have a Strava subscription (Strava requires one for API access since June 2026). It adds richer Strava data and works on phones without Health Connect.

1. Create an API app at <https://www.strava.com/settings/api> with **Authorization Callback Domain** `localhost`.
2. Add to `local.properties` (git-ignored):
   ```properties
   strava.clientId=12345
   strava.clientSecret=your_client_secret
   ```
3. Rebuild, then Settings → **Connect Strava account**. The secret is compiled into the APK, so keep builds to yourself.

### Publishing note

Sideloaded and debug builds can read Health Connect freely. Releasing on Google Play requires filling out Play's Health apps declaration for the data types used.

## Project layout

```
app/src/main/java/com/wylew/activitystats/
  data/            SettingsStore (DataStore), ActivityStore (JSON cache), ActivityRepository (sync)
  data/healthconnect/ HealthConnectSource (exercise sessions + distance/elevation/HR totals)
  data/strava/     Optional: StravaAuth (OAuth + refresh), StravaApi (GET /athlete/activities)
  domain/          Activity model, RollupCalculator (daily/weekly/monthly/yearly), Dedupe, formatting
  sync/            SyncWorker + SyncScheduler (hourly WorkManager)
  ui/dashboard/    Compose dashboard + ViewModel
  ui/widgetconfig/ Widget setup screen with live preview
  ui/settings/     Strava connection, units
  theme/           ColorScience (CIELAB tonal palettes), SchemeGenerator (two seeds → M3 roles), ThemeSettings
  ui/components/   CookieShape, WavyProgress, RollupBars
  widget/          Glance widget, per-widget settings
```

## Toolchain

AGP 8.13, Kotlin 2.2, Compose BOM 2025.10 (Material 3 1.4), Glance 1.1, Health Connect 1.1, minSdk 31, target/compile 36. Android Studio may offer upgrades; they should be safe to accept.

Material 3 Expressive components (`MaterialExpressiveTheme`, `MaterialShapes`, wavy progress indicators) are still internal in stable material3 1.4, so the app draws its own cookie shapes and wavy progress. Swap to the official APIs when material3 1.5 goes stable.

## Run tests

```
./gradlew test
```

## Roadmap

- Setting to choose which copy wins when a run is in both Garmin and Strava
- Editable goals, streak widget, 2×2 goal-ring widget
- Room database once Health Connect data arrives
- Roboto Flex for wide, heavy stat numerals
