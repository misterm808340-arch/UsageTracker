package com.usagetracker.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.usagetracker.app.UsageTrackerApplication

/**
 * Re-arms the BlockerService after device boot or app update so that active
 * blocks continue to be enforced (Android kills all services on reboot).
 *
 * INFERRED — NOT VERIFIED.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val svc = Intent(context, BlockerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(svc)
                } else {
                    context.startService(svc)
                }
            }
        }
    }
}
