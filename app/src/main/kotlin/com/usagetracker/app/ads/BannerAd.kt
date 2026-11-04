package com.usagetracker.app.ads

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * A full-width banner ad composable.
 *
 * Uses the SDK's adaptive banner sizing via
 * [AdSize.getCurrentOrientationAnchoredAdaptiveBannerSize] so the ad fills the
 * screen width on every device while keeping a sensible height.
 *
 * AdMob policy notes:
 *  - Test devices are tagged in debug builds (see [AdMob.initialize]).
 *  - Ad units must have content padding ≥ 4dp around them to avoid accidental
 *    clicks; here we give 8dp horizontal padding.
 *  - We must call [AdView.destroy] when the composable leaves composition to
 *    avoid leaking the underlying view.
 *
 * @param modifier any layout modifier. Defaults to `Modifier` (the composable
 *   itself fills the available width).
 */
@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val adView = remember {
        AdView(ctx).apply {
            // Adaptive banner: sized for current orientation at ~320 dp width.
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, 320))
            adUnitId = AdMob.bannerAdUnitId
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    // Load the ad once per AdView instance (not on every recomposition).
    LaunchedEffect(adView) {
        adView.loadAd(AdRequest.Builder().build())
    }

    // Free the AdView when the composable leaves the composition.
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        AndroidView(factory = { adView })
    }
}
