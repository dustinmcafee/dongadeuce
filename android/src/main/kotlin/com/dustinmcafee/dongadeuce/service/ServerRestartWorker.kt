package com.dustinmcafee.dongadeuce.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Worker that restarts the dedicated server after device reboot.
 * Reads saved configuration from SharedPreferences.
 */
class ServerRestartWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("dongadeuce_server", Context.MODE_PRIVATE)

        val port = prefs.getInt("server_port", 9090)
        val maxGames = prefs.getInt("server_max_games", 100)
        val maxPlayers = prefs.getInt("server_max_players", 6)
        val tlsEnabled = prefs.getBoolean("server_tls_enabled", false)

        DedicatedServerService.startServer(applicationContext, port, maxGames, maxPlayers, tlsEnabled)

        return Result.success()
    }
}
