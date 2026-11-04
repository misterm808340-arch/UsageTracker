package com.usagetracker.app.ads

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.usagetracker.app.BuildConfig

/**
 * Central place for AdMob configuration.
 *
 * Test ad unit IDs (Google's official samples) are used in debug builds. Release builds
 * expect the developer to replace [BANNER_AD_UNIT_ID] / [INTERSTITIAL_AD_UNIT_ID] with their
 * own production ad unit IDs *and* the App ID meta-data in `AndroidManifest.xml`.
 *
 * Docs:
 * - Banner:       https://developers.google.com/admob/android/banner
 * - Interstitial: https://developers.google.com/admob/android/interstitial
 * - Test ads:     https://developers.google.com/admob/android/test-ads
 */
object AdMob {

    private const val TAG = "AdMob"

    // ---- Test ad unit IDs (Google sample). DO NOT ship to production. ----
    private const val TEST_BANNER_AD_UNIT_ID       = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // ---- Production ad unit IDs.
    // Replace these with YOUR ad unit IDs before publishing.
    private const val PROD_BANNER_AD_UNIT_ID       = "ca-app-pub-3940256099942544/6300978111" // TODO: replace with real banner ad unit
    private const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // TODO: replace with real interstitial ad unit

    val bannerAdUnitId: String =
        if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    val interstitialAdUnitId: String =
        if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    @Volatile private var initialized = false

    /**
     * Initialize the Mobile Ads SDK on a background thread. Safe to call multiple times.
     * Adds this device as a test device in debug builds so test ads are always returned.
     */
    fun initialize(application: Application) {
        if (initialized) return
        initialized = true

        // Mark this device as a test device in debug builds to avoid ad policy
        // violations during development. In release, no test device IDs are set.
        val requestConfig = RequestConfiguration.Builder()
            .setTestDeviceIds(
                if (BuildConfig.DEBUG) listOf(AdRequest.DEVICE_ID_EMULATOR) else emptyList()
            )
            .build()
        MobileAds.setRequestConfiguration(requestConfig)

        // initialize() handles threading internally and may invoke the callback
        // on whichever thread it picks. We just log the per-adapter status.
        MobileAds.initialize(application) { status ->
            status.adapterStatusMap.forEach { (cls, adapterStatus) ->
                Log.i(
                    TAG,
                    "Adapter: $cls -> latency=${adapterStatus.latency}ms, " +
                        "initialization=${adapterStatus.initializationState}"
                )
            }
        }

        // Pre-load an interstitial so it's ready when the user navigates to a detail page.
        InterstitialAdManager.preload(application)
    }
}
