package com.dustinmcafee.dongadeuce.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Receives BOOT_COMPLETED and schedules server restart if it was running before reboot.
 */
class ServerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("dongadeuce_server", Context.MODE_PRIVATE)
        val wasRunning = prefs.getBoolean("server_running", false)

        if (wasRunning) {
            val workRequest = OneTimeWorkRequestBuilder<ServerRestartWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
