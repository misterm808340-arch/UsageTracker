package com.usagetracker.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Loads and shows a rewarded ad.
 *
 * The ad is preloaded on app launch ([preload]). Callers can call [show] to
 * display it; if the ad is not ready yet we silently skip (the user's action
 * is not blocked). After the user dismisses the ad, [onReward] is invoked
 * if the ad actually completed (the reward callback fires from the SDK).
 *
 * AdMob policy:
 *  - Rewarded ads must only be shown when the user has explicitly opted in
 *    (e.g. tapped a "Watch ad to support us" button), never automatically.
 *  - The reward itself must be something of value to the user; here we use
 *    it as an optional "support the developer" hook that doesn't gate any
 *    app functionality.
 */
object RewardedAdManager {

    private const val TAG = "AdMobRewarded"

    @Volatile private var rewardedAd: RewardedAd? = null
    @Volatile private var isLoading = false
    @Volatile private var fullScreenShowing = false

    /**
     * Start loading a rewarded ad in the background. Safe to call multiple times;
     * the actual load is only triggered if no ad is loaded and no load is in progress.
     */
    fun preload(context: Context) {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        val appContext = context.applicationContext
        RewardedAd.load(
            appContext,
            AdMob.rewardedAdUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            fullScreenShowing = false
                            preload(appContext) // refresh for next time
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            rewardedAd = null
                            fullScreenShowing = false
                            preload(appContext)
                        }
                        override fun onAdShowedFullScreenContent() {
                            fullScreenShowing = true
                        }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    Log.w(TAG, "Failed to load rewarded: code=${error.code} msg=${error.message}")
                }
            }
        )
    }

    /**
     * Show the rewarded ad now if ready. Once the user finishes watching,
     * [onReward] is invoked (the SDK fires the reward callback synchronously
     * during the show call, before the user dismisses the full screen).
     *
     * If the ad is not loaded yet, we kick off a preload and skip silently.
     */
    fun show(activity: Activity, onReward: (Int) -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            preload(activity.applicationContext)
            return
        }
        if (fullScreenShowing) return
        ad.show(activity) { rewardItem ->
            // rewardItem contains the reward amount and type configured in AdMob.
            Log.i(TAG, "Reward earned: amount=${rewardItem.amount} type=${rewardItem.type}")
            onReward(rewardItem.amount)
        }
    }

    /** Whether a rewarded ad is currently loaded and ready to show. */
    fun isReady(): Boolean = rewardedAd != null && !fullScreenShowing
}
