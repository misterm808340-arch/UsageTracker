package com.usagetracker.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usagetracker.app.R
import androidx.compose.ui.res.stringResource

/**
 * v1.3.1-fix: this screen now re-checks the Usage Access permission on every
 * ON_RESUME event. The previous implementation checked it once at ViewModel
 * init and never again, so when the user granted the permission in system
 * Settings and returned to the app, the screen kept showing the "Enable
 * Usage Access" button because the StateFlow still held the stale `false`
 * value. With this hook the screen calls `onResume` (wired to
 * `vm.refreshAccess()` in the NavHost), which queries AppOpsManager live and
 * updates the StateFlow — the composable then recomposes with the true
 * permission state.
 *
 * Also adds a `LaunchedEffect(state.hasUsageAccess)` that auto-navigates to
 * Overview when the permission transitions to granted. This catches the case
 * where the user grants the permission and returns — they don't have to
 * manually tap "Get Started" before the app recognizes the grant.
 *
 * `onResume` is intentionally NOT used to launch Settings (that would create
 * a loop); Settings is launched only from the explicit "Enable Usage Access"
 * button tap (`onGrantUsageAccess`).
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onGrantUsageAccess: () -> Unit,
    onSkip: () -> Unit,
    onGetStarted: () -> Unit,
    onRefresh: () -> Unit,
    onResume: () -> Unit = {},
) {
    // v1.3.1-fix: lifecycle-safe ON_RESUME hook. Mirrors the pattern already
    // used in BlockerScreen — observer is added in a DisposableEffect keyed on
    // the lifecycleOwner so it is removed on dispose and never leaks.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // v1.3.1-fix: when the permission transitions from denied → granted, fire
    // onGetStarted() automatically. This prevents the "stuck on onboarding
    // even though permission granted" loop. The parent's onGetStarted calls
    // vm.complete() and navigates to Overview with popUpTo inclusive.
    LaunchedEffect(state.hasUsageAccess) {
        if (state.hasUsageAccess) {
            onGetStarted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "v1.2.2",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        if (state.hasUsageAccess) {
            // Permission is granted — show the "Get Started" button as a
            // manual fallback in case the auto-navigation LaunchedEffect above
            // is suppressed (e.g. during a configuration change where
            // LaunchedEffect re-runs but onGetStarted is idempotent via
            // settingsRepo.setOnboarded already being true).
            Button(
                onClick = { onGetStarted(); onRefresh() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(stringResource(R.string.onboarding_get_started))
            }
        } else {
            Button(
                onClick = onGrantUsageAccess,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(stringResource(R.string.onboarding_grant_usage))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_skip))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Re-check access") }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.onboarding_privacy_note),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
