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
 * Debug builds always use Google's official sample ad unit IDs (and tag this
 * device as a test device) so we never trigger a policy violation during
 * development. Release builds use the real production ad unit IDs owned by
 * this app's AdMob account.
 *
 * Docs:
 * - Banner:       https://developers.google.com/admob/android/banner
 * - Interstitial: https://developers.google.com/admob/android/interstitial
 * - Rewarded:     https://developers.google.com/admob/android/rewarded
 * - Test ads:     https://developers.google.com/admob/android/test-ads
 */
object AdMob {

    private const val TAG = "AdMob"

    // ---- Test ad unit IDs (Google sample). Used in debug builds only. ----
    private const val TEST_BANNER_AD_UNIT_ID        = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_AD_UNIT_ID  = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_AD_UNIT_ID      = "ca-app-pub-3940256099942544/5224354917"

    // ---- Production ad unit IDs (real AdMob account). Used in release builds. ----
    private const val PROD_BANNER_AD_UNIT_ID        = "ca-app-pub-4359575771288892/3029669119"
    private const val PROD_INTERSTITIAL_AD_UNIT_ID  = "ca-app-pub-4359575771288892/7166410589"
    private const val PROD_REWARDED_AD_UNIT_ID      = "ca-app-pub-4359575771288892/9965953412"

    val bannerAdUnitId: String =
        if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    val interstitialAdUnitId: String =
        if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    val rewardedAdUnitId: String =
        if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID

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
        // Pre-load a rewarded ad so it is ready when the user opens Settings.
        RewardedAdManager.preload(application)
    }
}
