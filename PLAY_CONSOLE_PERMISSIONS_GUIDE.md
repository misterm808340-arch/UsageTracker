# Play Console — Fixing the "Sensitive app permissions" rejection

The Play Console flagged versionCode 6 with the error:

> **Release contains undeclared permissions. Update your declaration in Sensitive app permissions.**

After upgrading to **v1.2.3 (versionCode 7, targetSdk 36)** the same set of special
permissions still apply. They are intrinsic to the app's functionality and cannot
be removed. You must declare each one in the Play Console
**App content → Sensitive app permissions** page.

Open: <https://play.google.com/console/u/0/developers/{your-developer-id}/app/{app-id}/policy-declaration>

## Special permissions that require a declaration

Play Console's "Sensitive app permissions" page tracks *all* permissions in the
Google Play Policy Center "high‑risk / sensitive" list. Our release declares
the following ones (the merged manifest in the AAB was used to produce this list):

| # | Permission | Why we use it | What to enter in the form |
|---|---|---|---|
| 1 | `android.permission.PACKAGE_USAGE_STATS` | Core feature. The app's entire purpose is tracking per‑app usage time. We read `UsageStatsManager.queryAndAggregateUsageStats()` and `queryEvents()` to show daily/weekly usage breakdowns and history charts. | Permission type: **Usage access**. Select: **Yes — this app needs usage access.** In the justification box, write: *"UsageTracker displays the user's per‑app screen‑time usage and historical charts. It uses UsageStatsManager to aggregate time‑spent data for the user only. The data is stored entirely on‑device and never leaves the device."* Add a demo video link showing the user granting the permission in Settings → Apps → Special access → Usage access. |
| 2 | `android.permission.SYSTEM_ALERT_WINDOW` | The blocker feature draws a full‑screen "stay focused" overlay on top of user‑selected apps. | Permission type: **Display over other apps**. Select: **Yes — this app needs to draw over other apps.** Justification: *"UsageTracker's app‑blocking feature shows a blocking screen over user‑selected apps to help the user stay focused. The overlay is triggered only when the user opens an app they have explicitly added to their block list, and it disappears immediately when they leave the app."* |
| 3 | `android.permission.QUERY_ALL_PACKAGES` | We need to enumerate installed apps to display their names, icons, and let the user pick which apps to track/block. | Permission type: **Query all packages**. This is the most strictly reviewed permission. **Make sure you select one of the approved use‑cases**. For an app like UsageTracker, the closest valid use‑case is *"App drawer / launcher / app‑management functionality that needs to show installed apps to the user."* Justification: *"UsageTracker lets the user view a list of all installed apps and select which ones to track usage for and which to block. We use PackageManager to look up app labels and icons so the user can recognize apps in our picker. We do not transmit the list of installed packages off‑device."* |

## Permissions that do **not** need a Sensitive permissions declaration

The Play Console "Sensitive app permissions" page only covers the
**high‑risk** permission groups above. The following permissions in our manifest
are *normal/standard* and don't require filling in the form:

- `INTERNET` — required by AdMob
- `ACCESS_NETWORK_STATE` — required by AdMob
- `WAKE_LOCK` — auto‑merged by the AdMob SDK
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` — for the blocker service. *Note: with `FOREGROUND_SERVICE_SPECIAL_USE`, you must additionally declare the special‑use subtype in the Play Console **App content → Special access** page (not the Sensitive permissions page).* The subtype is described in `AndroidManifest.xml` via `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` = `"App blocker overlay: shows a blocking screen over user-selected apps to help the user stay focused."`
- `POST_NOTIFICATIONS` — Android 13+ runtime permission, normal
- `RECEIVE_BOOT_COMPLETED` — normal, to restart the blocker service after reboot
- `ACCESS_NOTIFICATION_POLICY` — Do Not Disturb access (only used by the optional "blocker stops notifications" toggle, off by default). On the Play Console form this *might* be flagged as "Notification policy access" — if you see it listed, declare it as: *"The app optionally silences notifications for user‑selected apps during focus sessions, by toggling the Do Not Disturb policy. The user must explicitly grant this permission in Settings → Apps → Special access → Do Not Disturb."*

## AdMob-related permissions (auto-merged)

The Google Mobile Ads SDK adds these to the merged manifest automatically:

- `ACCESS_ADSERVICES_AD_ID` — Privacy Sandbox / Topics API
- `ACCESS_ADSERVICES_ATTRIBUTION` — Attribution Reporting API
- `ACCESS_ADSERVICES_TOPICS` — Topics API

These are not in the "Sensitive app permissions" list and need no declaration.
They are part of the Privacy Sandbox and are flagged under
**App content → Privacy — Ads** (separate page) — there you must complete the
**Privacy Sandbox** form (it's a single toggle saying whether you participate).

## Required companion disclosures

While you're on the App content page, you also need to fill in (most are
unrelated to the rejected release, but Play will block publishing without them):

1. **Privacy Policy URL** — Required. If you don't have one, publish a simple
   page that says: *"UsageTracker collects only on‑device usage statistics and
   user preferences. No data is transmitted off the device. We use Google AdMob
   to show ads; AdMob may collect device and ad‑interaction data as described in
   its privacy policy."* Upload it to a public URL.
2. **Ads form** — Confirm the app contains ads (because of AdMob).
3. **App access** — Confirm the app does not require login.
4. **Content rating** — Fill in the IARC questionnaire (select "No" for
   violence, etc.). Recommended rating: Everyone.
5. **Target audience** — Select 18+ or 13+ as appropriate.
6. **News app** — No.
7. **Government apps** — No.
8. **Financial features** — No.
9. **Special access** — Foreground service special use (see above).

## Debug symbols (.so files) warning

The Play Console warns:

> **App Bundle contains native code, but no debug symbols file was uploaded.**

This is a *warning*, not an error — your release can still be published.
To silence it, extract the unstripped `.so` files from the build and upload
them as a native debug symbols .zip:

```bash
# Inside /home/z/my-project/download/UsageTracker
# (build outputs already exist)
mkdir -p apks/native-debug-symbols
cp -r app/build/intermediates/cxx/Debug/* apks/native-debug-symbols/ 2>/dev/null || true
# OR, more reliably, extract from the symbols directory created by AGP
find app/build -name "*.so" -path "*/intermediates/*" -not -path "*stripped*" 2>/dev/null
```

If the project has no native code (no `externalNativeBuild` block in
`app/build.gradle.kts`), the `.so` files come from the AndroidX libraries
(`libandroidx.graphics.path.so`, `libdatastore_shared_counter.so`) — these
are *not* your symbols and you don't need to upload debug symbols for them.
Just acknowledge the warning in Play Console ("I understand") and proceed.

## Steps to publish

1. Upload the new AAB: `UsageTracker-v1.2.3-sdk36-release.aab`
   (located at `/home/z/my-project/download/UsageTracker/apks/`).
2. Wait for Play Console to rescan permissions (takes ~5–10 minutes).
3. Go to **App content → Sensitive app permissions** and fill in the form
   for the three permissions listed in the table above.
4. Go to **App content → Special access** and add a "Foreground service
   special use" entry with the subtype text from the manifest.
5. (Optional) Complete the **Privacy Sandbox** form if it appears.
6. Re‑submit the release for review.
