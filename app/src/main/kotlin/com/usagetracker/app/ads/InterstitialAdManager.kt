package com.usagetracker.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Loads and shows an interstitial ad.
 *
 * The ad is preloaded on app launch ([preload]). When the caller wants to show it
 * ([show]), if the ad is ready it is displayed; otherwise we kick off another load
 * and the caller is skipped silently — we never block navigation on the ad.
 *
 * AdMob policy:
 *  - Interstitials must be shown at a natural transition point. We tie this to
 *    app-detail navigation (a clear content break) so it does not interrupt the user
 *    mid-action.
 *  - We do not show an interstitial more than once per [INTERSTITIAL_MIN_INTERVAL_MS]
 *    to avoid spamming.
 */
object InterstitialAdManager {

    private const val TAG = "AdMobInterstitial"
    private const val INTERSTITIAL_MIN_INTERVAL_MS = 60_000L // 1 minute between ads

    @Volatile private var interstitialAd: InterstitialAd? = null
    @Volatile private var isLoading = false
    @Volatile private var lastShownAt = 0L
    @Volatile private var fullScreenShowing = false

    /**
     * Start loading an interstitial ad in the background. Safe to call multiple times;
     * the actual load is only triggered if no ad is loaded and no load is in progress.
     */
    fun preload(context: Context) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        val appContext = context.applicationContext
        InterstitialAd.load(
            appContext,
            AdMob.interstitialAdUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            fullScreenShowing = false
                            preload(appContext) // refresh for next time
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            interstitialAd = null
                            fullScreenShowing = false
                            preload(appContext)
                        }
                        override fun onAdShowedFullScreenContent() {
                            fullScreenShowing = true
                            lastShownAt = System.currentTimeMillis()
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.w(TAG, "Failed to load interstitial: code=${error.code} msg=${error.message}")
                }
            }
        )
    }

    /**
     * Show the interstitial ad now if ready and at least [INTERSTITIAL_MIN_INTERVAL_MS]
     * has passed since the last show. Otherwise no-op (caller proceeds with navigation).
     */
    fun show(activity: Activity) {
        val ad = interstitialAd
        if (ad == null) {
            preload(activity.applicationContext)
            return
        }
        if (fullScreenShowing) return
        if (System.currentTimeMillis() - lastShownAt < INTERSTITIAL_MIN_INTERVAL_MS) return
        ad.show(activity)
    }

    /**
     * Helper for activities that want to tie interstitial shows to their own lifecycle
     * (e.g. an app-open interstitial). Not used by default but exposed for future use.
     */
    fun lifecycleObserver(activity: Activity): DefaultLifecycleObserver =
        object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                preload(activity.applicationContext)
            }
        }
}
