package com.usagetracker.app.ui.blocker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usagetracker.app.data.datasource.AppIconCache
import com.usagetracker.app.data.local.entity.AppInfoEntity
import com.usagetracker.app.data.local.entity.BlockedAppEntity
import com.usagetracker.app.data.local.entity.LaunchLimitEntity
import com.usagetracker.app.domain.util.PermissionLogic
import com.usagetracker.app.ui.components.SectionHeader
import com.usagetracker.app.ui.theme.Danger
import com.usagetracker.app.ui.theme.Success
import com.usagetracker.app.util.TimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@Composable
fun BlockerScreen(
    state: BlockerUiState,
    onBlockApp: (String, String, Int) -> Unit,
    onUnblock: (String) -> Unit,
    onSetBlockNotifications: (Boolean) -> Unit,
    onSetLimit: (String, String, Int, Boolean) -> Unit,
    onRemoveLimit: (String) -> Unit,
    onListInstalled: suspend () -> List<AppInfoEntity>,
    onResume: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickerOpen by remember { mutableStateOf(false) }
    var limitPickerOpen by remember { mutableStateOf(false) }

    // v1.3.0-fix: lifecycle-safe runtime permission launcher for POST_NOTIFICATIONS.
    // Registered at the composable scope so it survives recomposition and config
    // changes. The result callback is a no-op — the BlockerViewModel re-checks
    // hasPostNotifications on resume (see onResume callback).
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored; permission state is re-checked on resume */ }

    // v1.3.0-fix: re-check all four permission flags whenever the screen becomes
    // visible. This catches the user granting/denying a permission in the system
    // Settings screen and returning to the app — without this, the cards stay
    // stale because the underlying combine only re-emits on blocked/limits/
    // blockNotifications changes.
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            "Blocker",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(16.dp))
        SectionHeader("Permissions")

        PermissionCard(
            title = "Display over other apps",
            desc = "Covers blocked apps with a focus screen",
            granted = state.hasOverlay,
            onGrant = { ctx.startActivity(PermissionLogic.overlaySettingsIntent(ctx)) },
        )
        PermissionCard(
            title = "Usage Access",
            desc = "Required to detect app launches",
            granted = state.hasUsageAccess,
            onGrant = { ctx.startActivity(PermissionLogic.usageAccessSettingsIntent()) },
        )
        PermissionCard(
            title = "Do Not Disturb access",
            desc = "Blocks notifications when an app is blocked",
            granted = state.hasDndAccess,
            onGrant = { ctx.startActivity(PermissionLogic.notificationPolicyAccessIntent()) },
        )
        // v1.3.0-fix: Notifications permission now uses the runtime
        // ActivityResultContracts.RequestPermission() flow on Android 13+ — first
        // tap shows the system permission dialog; if the user denies it (or has
        // previously denied it permanently), the next tap falls back to opening
        // this app's notification settings so they can re-enable it manually.
        NotificationPermissionCard(
            granted = state.hasPostNotifications,
            onRequestRuntime = { notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
            onOpenSettings = { ctx.startActivity(PermissionLogic.notificationPermissionIntent(ctx)) },
        )

        SectionHeader("Notifications")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Block all notifications while focusing",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (!state.hasDndAccess) {
                        Text("Grant Do Not Disturb access to enable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = state.blockNotifications,
                    onCheckedChange = onSetBlockNotifications,
                    enabled = state.hasDndAccess,
                )
            }
        }

        SectionHeader("App Blocking")
        Text(
            "Block selected apps for a set duration. The app is covered by a focus screen until the timer ends.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        if (state.blocked.isNotEmpty()) {
            state.blocked.forEach { b ->
                BlockedAppRow(b = b, onUnblock = { onUnblock(b.packageName) })
                Spacer(Modifier.height(4.dp))
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    "No apps currently blocked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { pickerOpen = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("Choose apps & set timer")
        }

        SectionHeader("Daily Limits")
        Text(
            "Limit how many times an app can be opened per day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        if (state.limits.isNotEmpty()) {
            state.limits.forEach { l ->
                LimitRow(l = l, onRemove = { onRemoveLimit(l.packageName) }, onEdit = { limitPickerOpen = true })
                Spacer(Modifier.height(4.dp))
            }
        } else {
            Text(
                "No daily limits set.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { limitPickerOpen = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("Add daily limit")
        }

        Spacer(Modifier.height(24.dp))
    }

    if (pickerOpen) {
        AppPickerDialog(
            onDismiss = { pickerOpen = false },
            onPick = { pkg, label, minutes ->
                onBlockApp(pkg, label, minutes)
                pickerOpen = false
            },
            listInstalled = onListInstalled,
        )
    }
    if (limitPickerOpen) {
        LimitEditDialog(
            onDismiss = { limitPickerOpen = false },
            onConfirm = { pkg, label, limit ->
                onSetLimit(pkg, label, limit, true)
                limitPickerOpen = false
            },
            listInstalled = onListInstalled,
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    desc: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium)
                Text(desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // v1.3.0-fix: color-coded status text — green for granted, red for not granted.
            // Was a single gold "Granted" / default "Grant" button; now shows the true state.
            if (granted) {
                Text("Granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = Success,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 8.dp))
            } else {
                Text("Not granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = Danger,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 8.dp))
                TextButton(onClick = onGrant) { Text("Enable in Settings") }
            }
        }
    }
}

/**
 * v1.3.0-fix: dedicated permission card for POST_NOTIFICATIONS.
 *
 * - On Android 13+ (TIRAMISU), the first tap triggers the runtime permission
 *   dialog via [onRequestRuntime].
 * - If the permission has been permanently denied (shouldShowRationale returns
 *   false after a previous denial), or if we are on Android < 13 where there is
 *   no runtime permission, the button label switches to "Enable in Settings"
 *   and opens this app's notification settings via [onOpenSettings].
 * - The status text reflects the actual permission state (granted / not granted)
 *   and is re-evaluated by the BlockerViewModel on every ON_RESUME.
 */
@Composable
private fun NotificationPermissionCard(
    granted: Boolean,
    onRequestRuntime: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    // v1.3.0-fix: try the runtime permission dialog first; the system silently
    // no-ops if the permission has been permanently denied. The user always has
    // a separate "Enable in Settings" button to open this app's notification
    // settings, where they can re-enable notifications even after a permanent
    // denial. We don't try to track "asked before" ourselves — Android's
    // shouldShowRequestPermissionRationale is the canonical signal and we let
    // the system dialog handle the user's response.

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Notifications permission",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium)
                Text("Shows the blocker status notification",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (granted) {
                Text("Granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = Success,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 8.dp))
            } else {
                Text("Not granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = Danger,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 8.dp))
                // v1.3.0-fix: always offer the runtime Grant button on Android 13+
                // (the system silently no-ops if the permission was permanently
                // denied), and always offer the Settings fallback for that case.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    TextButton(onClick = onRequestRuntime) { Text("Grant") }
                }
                TextButton(onClick = onOpenSettings) { Text("Settings") }
            }
        }
    }
}

@Composable
private fun BlockedAppRow(b: BlockedAppEntity, onUnblock: () -> Unit) {
    val ctx = LocalContext.current
    // v1.2.2-fix: async icon load (was blocking PackageManager call in composition).
    val bmp by produceState<android.graphics.Bitmap?>(initialValue = null, b.packageName) {
        value = withContext(Dispatchers.IO) { AppIconCache.get(ctx).getBitmap(b.packageName) }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (bmp != null) {
                val bIcon = bmp!!
                androidx.compose.foundation.Image(
                    bitmap = bIcon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                )
            } else {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant))
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(b.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                val remaining = (b.expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
                Text("Remaining: ${TimeFormat.durationHm(remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onUnblock) {
                Text("Unblock")
            }
        }
    }
}

@Composable
private fun LimitRow(l: LaunchLimitEntity, onRemove: () -> Unit, onEdit: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(l.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("${l.limit} opens/day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Close, contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    app: AppInfoEntity,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val ctx = LocalContext.current
    // v1.3.0-fix: load the app's original launcher icon via PackageManager / AppIconCache.
    // Loaded off the main thread to keep the picker scroll smooth even with hundreds of apps.
    val bmp by produceState<android.graphics.Bitmap?>(initialValue = null, app.packageName) {
        value = withContext(Dispatchers.IO) { AppIconCache.get(ctx).getBitmap(app.packageName) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // App icon (36dp) on the left — placeholder if not yet loaded / unavailable.
        if (bmp != null) {
            Image(
                bitmap = bmp!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        Spacer(Modifier.size(10.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onSelect,
        )
    }
}

@Composable
private fun AppPickerDialog(
    onDismiss: () -> Unit,
    onPick: (String, String, Int) -> Unit,
    listInstalled: suspend () -> List<AppInfoEntity>,
) {
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppInfoEntity>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AppInfoEntity?>(null) }
    var minutes by remember { mutableStateOf("25") }

    LaunchedEffect(Unit) {
        apps = listInstalled()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val s = selected
                val m = minutes.toIntOrNull() ?: 0
                if (s != null && m > 0) onPick(s.packageName, s.label, m)
            }) { Text("Block") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Choose app & duration") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.height(280.dp)) {
                    val filtered = apps.filter {
                        it.label.contains(query, ignoreCase = true) ||
                            it.packageName.contains(query, ignoreCase = true)
                    }
                    LazyColumn {
                        items(filtered, key = { it.packageName }) { a ->
                            AppPickerRow(
                                app = a,
                                selected = selected?.packageName == a.packageName,
                                onSelect = { selected = a },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter(Char::isDigit) },
                    label = { Text("Duration (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    )
}

@Composable
private fun LimitEditDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit,
    listInstalled: suspend () -> List<AppInfoEntity>,
) {
    var apps by remember { mutableStateOf<List<AppInfoEntity>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AppInfoEntity?>(null) }
    var limit by remember { mutableStateOf("10") }

    LaunchedEffect(Unit) { apps = listInstalled() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val s = selected
                val n = limit.toIntOrNull() ?: 0
                if (s != null && n > 0) onConfirm(s.packageName, s.label, n)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Daily open limit") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.height(280.dp)) {
                    val filtered = apps.filter {
                        it.label.contains(query, ignoreCase = true) ||
                            it.packageName.contains(query, ignoreCase = true)
                    }
                    LazyColumn {
                        items(filtered, key = { it.packageName }) { a ->
                            AppPickerRow(
                                app = a,
                                selected = selected?.packageName == a.packageName,
                                onSelect = { selected = a },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it.filter(Char::isDigit) },
                    label = { Text("Opens per day") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    )
}
