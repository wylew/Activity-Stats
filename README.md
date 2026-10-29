# Activity Stats

An Android dashboard and home-screen widgets for your Strava activities, styled after Material 3 Expressive.

- **Widgets:** one configurable roll-up widget. For each widget you choose the activity (**Run**, **Ride** or **All**), **Daily** or **Weekly**, the headline number (count, distance or time) and a color. At 2×2 it shows the headline number; at 4×2 and up it adds day bars (weekly) or today's list (daily). Colors follow your wallpaper (Material You).
- **Dashboard:** Week / Month / Year roll-ups with a distance hero card, a comparison against the same point last period, moving time, pace, elevation, heart rate, a goal bar and recent activities.
- **Data:** Strava API (OAuth, single athlete). Activities are cached on the phone and refreshed hourly.

Design mockups live on the "Rollup" canvas in Claude.

## Setup

1. Create a Strava API app at <https://www.strava.com/settings/api>.
   - **Authorization Callback Domain:** `localhost`
2. Add the credentials to `local.properties` in the project root (this file is git-ignored):
   ```properties
   strava.clientId=12345
   strava.clientSecret=your_client_secret
   ```
3. Open in Android Studio, sync Gradle and run on a phone with Android 12 (API 31) or newer.
4. Tap **Connect Strava account**, approve, and the first sync backfills from January 1 of last year.
5. Long-press the home screen → Widgets → Activity Stats → **Activity roll-up**.

### Strava API notes

- New apps allow 1 connected athlete (upgradable to 10 without review), which is all this app needs.
- Strava's Standard API tier requires an active Strava subscription on the developer account.
- The client secret is compiled into the APK. That's fine for a build only you install; move the token exchange to a server before sharing builds.

## Project layout

```
app/src/main/java/com/wylew/activitystats/
  data/            SettingsStore (DataStore), ActivityStore (JSON cache), ActivityRepository (sync)
  data/strava/     StravaAuth (OAuth + refresh), StravaApi (GET /athlete/activities)
  domain/          Activity model, RollupCalculator (daily/weekly/monthly/yearly), formatting
  sync/            SyncWorker + SyncScheduler (hourly WorkManager)
  ui/dashboard/    Compose dashboard + ViewModel
  ui/widgetconfig/ Widget setup screen with live preview
  ui/settings/     Strava connection, units
  ui/components/   CookieShape, WavyProgress, RollupBars
  widget/          Glance widget, per-widget settings
```

## Toolchain

AGP 8.13, Kotlin 2.2, Compose BOM 2025.10 (Material 3 1.4), Glance 1.1, minSdk 31, target/compile 36. Android Studio may offer upgrades; they should be safe to accept.

Material 3 Expressive components (`MaterialExpressiveTheme`, `MaterialShapes`, wavy progress indicators) are still internal in stable material3 1.4, so the app draws its own cookie shapes and wavy progress. Swap to the official APIs when material3 1.5 goes stable.

## Run tests

```
./gradlew test
```

## Roadmap

- Garmin via Health Connect, with Garmin + Strava duplicates merged (start time ±2 min, distance ±3%)
- Editable goals, streak widget, 2×2 goal-ring widget
- Room database once Health Connect data arrives
- Roboto Flex for wide, heavy stat numerals
