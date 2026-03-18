package com.dustinmcafee.dongadeuce.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dustinmcafee.dongadeuce.MainActivity
import com.dustinmcafee.dongadeuce.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service that keeps the game session alive when the app is backgrounded.
 * Shows a persistent notification with the game status that brings the user back to the game.
 */
class GameSessionService : Service() {

    companion object {
        private const val CHANNEL_ID = "dongadeuce_game_session"
        private const val NOTIFICATION_ID = 1002
        private const val EXTRA_MODE = "game_mode"
        private const val EXTRA_SERVER = "server_address"

        private val _isActive = MutableStateFlow(false)
        val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

        fun start(context: Context, mode: String, serverAddress: String = "") {
            val intent = Intent(context, GameSessionService::class.java).apply {
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_SERVER, serverAddress)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GameSessionService::class.java))
        }

        fun updateNotification(context: Context, playerCount: Int, gamePhase: String) {
            if (!_isActive.value) return
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, buildNotification(context, "In Game", "$playerCount players - $gamePhase"))
        }

        /**
         * Update notification with recent game log events (shown when app is backgrounded).
         */
        fun updateWithLog(context: Context, latestEvents: List<String>) {
            if (!_isActive.value) return
            val nm = context.getSystemService(NotificationManager::class.java)
            val summary = latestEvents.joinToString("\n")
            nm.notify(NOTIFICATION_ID, buildNotification(context, "In Game", summary, useInboxStyle = true, events = latestEvents))
        }

        private fun buildNotification(
            context: Context,
            title: String,
            text: String,
            useInboxStyle: Boolean = false,
            events: List<String> = emptyList()
        ): Notification {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = "RETURN_TO_GAME"
            }
            val openPendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("DongADeuce - $title")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification_game)
                .setOngoing(true)
                .setContentIntent(openPendingIntent)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            if (useInboxStyle && events.isNotEmpty()) {
                val inboxStyle = NotificationCompat.InboxStyle()
                events.forEach { inboxStyle.addLine(it) }
                builder.setStyle(inboxStyle)
            }

            return builder.build()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: "Game"
        val server = intent?.getStringExtra(EXTRA_SERVER) ?: ""

        val text = if (server.isNotBlank()) "Connected to $server" else "Game in progress"
        val notification = buildNotification(this, mode, text)
        startForeground(NOTIFICATION_ID, notification)
        _isActive.value = true

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        _isActive.value = false
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Game Session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active game session - keeps the game alive in the background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
