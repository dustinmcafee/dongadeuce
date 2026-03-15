package com.dustinmcafee.dongadeuce.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dustinmcafee.dongadeuce.MainActivity
import com.dustinmcafee.dongadeuce.R

/**
 * Manages notification channel and foreground notifications for the dedicated server service.
 */
class ServerNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "dongadeuce_server"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.dustinmcafee.dongadeuce.STOP_SERVER"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Dedicated Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "DongADeuce dedicated game server status"
            setShowBadge(false)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Build the foreground notification for the server service.
     */
    fun buildNotification(port: Int, gameCount: Int, tlsLabel: String? = null): Notification {
        // Tap notification to open app
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop server action button
        val stopIntent = Intent(ACTION_STOP).apply {
            setPackage(context.packageName)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("DongADeuce Server")
            .setContentText("Port $port${if (tlsLabel != null) " ($tlsLabel)" else ""} — $gameCount active game${if (gameCount != 1) "s" else ""}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Server",
                stopPendingIntent
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
