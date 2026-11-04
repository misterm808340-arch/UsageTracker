# Usage Tracker v1.2.2 — reconstruction

> **Status: INFERRED — NOT VERIFIED.** The original AAB was never delivered to
> this build environment (only a 12-frame contact sheet from the reference
> video). Every class name, field name, table column, and architectural seam
> in this project is a plausible reconstruction based on (a) the user's
> prompt and (b) the visible UI in the contact sheet. None of it was verified
> against the original DEX / R8 mapping / manifest. If you have the original
> `.aab`, re-upload it for an evidence-based rebuild.

## Package / version

- `applicationId` = `com.usagetracker.app`
- `versionName` = `1.2.2`
- `versionCode` = `5`
- `minSdk` = 26, `targetSdk` = 35, `compileSdk` = 35

## Tech stack

Kotlin 2.0.21 · AGP 8.10.1 · Jetpack Compose 1.7.6 · Material3 1.3.1 ·
Room 2.6.1 (KSP) · DataStore 1.1.1 · WorkManager 2.10.0 ·
Navigation Compose 2.8.5 · Coroutines 1.9.0 · Lifecycle 2.8.7 ·
Core KTX 1.15.0 · Google Mobile Ads (AdMob) 23.6.0.

## Privacy

- INTERNET permission is declared for AdMob ads. All *usage data* stays on device.
- Usage data (sessions, daily totals, app lists) is 100% local: stored in a
  Room database (`usage_tracker.db`); never sent off device.
- AdMob (Google Mobile Ads SDK 23.6.0) is integrated for ad monetization.
  In debug builds only test ads are served (emulator and registered test devices).
  In release builds, replace the test App ID + ad unit IDs with production ones
  (see `app/src/main/kotlin/com/usagetracker/app/ads/AdMob.kt` and the
  `com.google.android.gms.ads.APPLICATION_ID` meta-data in `AndroidManifest.xml`).
- You must publish a Privacy Policy on the Play Store listing disclosing AdMob's
  data handling before shipping live ad units.

## Architecture

```
UI (Compose) → ViewModel → Repository → DataSource / Room → Android API
```

- `UsageTrackerApplication` — DI root; owns singletons + ViewModelFactory.
- `MainActivity` — single Compose activity, hosts `UsageTrackerNavHost`.
- `AppViewModelFactory` — manual `ViewModelProvider.Factory` injecting
  repositories into every screen ViewModel.
- `UsageRepositoryImpl`, `BlockerRepositoryImpl`, `SettingsRepositoryImpl` —
  repository layer.
- `UsageStatsDataSource`, `InstalledAppsDataSource`, `AppIconCache` —
  thin Android-API wrappers.
- `SessionDetector`, `UsageAggregator` — pure-Kotlin usage engine.
- `UsageBucket`, `DaySlice`, `EpochDay`, `UsageEventRecord`, `AppUsageInfo`,
  `AppRanking`, `TimelineEntry`, `DailyUsage`, `HourlyUsage` — domain models.
- `AppListSorter`, `SystemPackageFilter`, `PermissionLogic`, `CsvBuilder`,
  `NotificationPolicyController` — domain utilities / policies.
- `BlockerService`, `BootReceiver`, `UsageSyncWorker` — background components.

## Database

Room, version 1, single DB file `usage_tracker.db`, 5 tables:

| Table | Primary key | Purpose |
|---|---|---|
| `app_info` | `id` (autogen), unique `packageName` | cached installed-app metadata |
| `usage_sessions` | `(packageName, startTimeMs)` | one row per foreground session |
| `daily_usage` | `(epochDay, packageName)` | pre-aggregated per-day per-app totals |
| `blocked_apps` | `packageName` | actively blocked app rows |
| `launch_limits` | `packageName` | per-app daily open limit |

## Usage engine

`UsageStatsManager.queryEvents(start, end)` → flat
`UsageEventRecord` stream → `SessionDetector.detect()` → list of
`UsageBucket`s (one per foreground session, split at local-day boundaries)
→ `UsageAggregator` produces:

- per-app totals + open-counts (Overview, Apps),
- 24-bucket hourly histogram for today,
- N-day totals for the 7/30-day charts,
- timeline rows,
- per-day per-app aggregates (History + App Detail).

Rules (honest):

- Buckets < 1 second are dropped (UI flicker / dialog taps).
- Buckets > 6 hours are capped (anti-stuck-foreground).
- Buckets spanning local midnight are split.
- Open count ≈ session count when event-based (ACTIVITY_RESUMED events).
- The estimate degrades for ranges > ~7 days because Android's
  `queryEvents` window is finite.

## Screens

Bottom navigation: Overview · Apps · Blocker · History · Settings.

- **Onboarding** — title, subtitle, grant-Usage-Access button, skip.
- **Overview** — Today's usage card, range chips (Hourly / 7 days / 30 days),
  chart, Most Used Apps, Most Opened Apps, Usage Timeline.
- **Apps** — search field, period chips (Today / Yesterday / 7d / 30d),
  sort dropdown (Most used / Most opened / Name), app rows with real
  `PackageManager` icons.
- **App Detail** — Today's usage, 30-day chart, day-by-day history.
- **Blocker** — 4 permission cards (Overlay / Usage Access / DND / Post-notif),
  Notifications switch, blocked-app rows with remaining time + unblock,
  daily-limits rows with edit/remove, app picker dialog, limit edit dialog.
- **History** — date nav (prev/next/pick via Material 3 DatePicker),
  daily summary card, top apps, app breakdown, timeline.
- **Settings** — Usage Access status, theme radio (System/Light/Dark),
  system-apps toggle, refresh, CSV export (via `FileProvider` + share sheet),
  clear cache, delete all data, AdMob disclosure, about.

## AdMob integration

The `com.usagetracker.app.ads` package wraps all ad logic:

- `AdMob.kt` — central config; initializes `MobileAds`, sets the device as a test
  device in debug builds, exposes banner/interstitial ad unit IDs.
- `BannerAd.kt` — Compose composable rendering an adaptive banner via
  `AndroidView(AdView)`. Used on Overview, Apps, History, Settings.
- `InterstitialAdManager.kt` — preloads an interstitial on app launch; shows
  it on app-detail navigation with a 1-minute cooldown.

Default AdMob App ID and ad unit IDs are Google's official test values. Replace
them with your production IDs before shipping.

## Blocker

`BlockerService` is a foreground service (`FOREGROUND_SERVICE_SPECIAL_USE`).
It polls `UsageStatsManager` every 1 second to detect the foreground app; if
a package is on the `blocked_apps` table and not yet expired, it inflates a
full-screen `WindowManager` overlay (`TYPE_APPLICATION_OVERLAY`) telling the
user the app is blocked. The overlay's "Close" button launches the home
intent — Android forbids us from force-stopping other apps. Auto-expiry is
handled by a coroutine timer per block, plus a `deleteExpired` pass on
every WorkManager sync.

Honest limitations:

- We cannot kill the target app — only cover it.
- We cannot intercept the launch before it happens — only after.
- Detection is polled, so the overlay may take up to ~1 s to appear.
- Requires `SYSTEM_ALERT_WINDOW` (Settings.canDrawOverlays).
- Requires `ACCESS_NOTIFICATION_POLICY` to silence notifications via DND.

`BootReceiver` re-arms `BlockerService` after `BOOT_COMPLETED` /
`LOCKED_BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`.

`UsageSyncWorker` (periodic, 6-hour interval) pulls the last 24 h of events
and prunes expired blocks.

## How to build (on your machine)

1. Install **Android Studio Ladybug (2024.2)** or newer + JDK 17.
2. Unzip this project.
3. `File → Open` the project folder.
4. Let Gradle sync. The first sync will download AGP 8.10.1, KSP
   2.0.21-1.0.28, Compose 1.7.6, etc.
5. Plug in a real device (the app needs real usage stats — emulator works
   but you'll have to generate usage history first).
6. `Run → Run 'app'`. Accept the on-screen permission prompts on the device:
   - Usage Access (`Settings → Apps → Special access → Usage access`)
   - Display over other apps
   - Do Not Disturb access
   - Notifications (Android 13+)
7. To produce a release AAB: `Build → Generate Signed Bundle / APK → Android App Bundle`.
   To produce an APK: same flow, pick APK, or `./gradlew assembleRelease`.

## What's NOT verified

Per the original prompt's instructions, mark everything that was not
recovered from the AAB as `INFERRED — NOT VERIFIED`. That includes:

- All Kotlin class names (`UsageRepositoryImpl`, `BlockerRepositoryImpl`,
  `SessionDetector`, `UsageAggregator`, `UsageBucket`, `DaySlice`,
  `EpochDay`, `UsageEventRecord`, etc.).
- All Room table column names.
- All Compose layout details beyond what's visible in the 12-frame
  contact sheet (text strings, exact paddings, exact color values).
- The exact behaviour of the original `BlockerService` /
  `NotificationPolicyController` / `BootReceiver` / `UsageSyncWorker`.
- The WorkManager schedule cadence (6-hour guess).
- The CSV column layout.
- The onboarding flow text.

If the original AAB becomes available, this project should be re-built
from its manifest + R8 mapping, not patched on top of this reconstruction.

## File structure

```
UsageTracker/
├── build.gradle.kts            (root)
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── kotlin/com/usagetracker/app/
        │   ├── UsageTrackerApplication.kt
        │   ├── MainActivity.kt
        │   ├── ads/{AdMob, BannerAd, InterstitialAdManager}.kt
        │   ├── di/AppViewModelFactory.kt
        │   ├── data/
        │   │   ├── local/{UsageDatabase, dao/, entity/}
        │   │   ├── datastore/SettingsDataStore.kt
        │   │   ├── datasource/{UsageStatsDataSource, InstalledAppsDataSource, AppIconCache}
        │   │   ├── repository/{UsageRepositoryImpl, BlockerRepositoryImpl, SettingsRepositoryImpl}
        │   │   └── csv/CsvBuilder.kt   (actually under domain/util/)
        │   ├── domain/
        │   │   ├── model/{UsageBucket, DaySlice, EpochDay, UsageEventRecord, AppUsageInfo}
        │   │   ├── usage/{SessionDetector, UsageAggregator}
        │   │   ├── util/{AppListSorter, SystemPackageFilter, PermissionLogic, CsvBuilder}
        │   │   └── policy/NotificationPolicyController.kt
        │   ├── service/{BlockerService, BootReceiver}
        │   ├── work/UsageSyncWorker.kt
        │   ├── ui/
        │   │   ├── theme/{Color, Theme, Type, Shape}
        │   │   ├── navigation/UsageTrackerNavHost.kt
        │   │   ├── components/{AppRow, HourlyChart}
        │   │   ├── onboarding/{OnboardingScreen, OnboardingViewModel}
        │   │   ├── overview/{OverviewScreen, OverviewViewModel}
        │   │   ├── apps/{AppsScreen, AppsViewModel, AppDetailScreen, AppDetailViewModel}
        │   │   ├── blocker/{BlockerScreen, BlockerViewModel}
        │   │   ├── history/{HistoryScreen, HistoryViewModel}
        │   │   └── settings/{SettingsScreen, SettingsViewModel}
        │   └── util/TimeFormat.kt
        └── res/
            ├── values/{colors, strings, themes}.xml
            ├── values-night/themes.xml
            ├── layout/blocker_overlay.xml
            ├── drawable/{ic_launcher_background, ic_launcher_foreground}.xml
            ├── mipmap-anydpi-v26/{ic_launcher, ic_launcher_round}.xml
            └── xml/{backup_rules, file_paths}.xml
```
