package com.dustinmcafee.dongadeuce.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.os.IBinder
import com.dustinmcafee.dongadeuce.server.LobbyManager
import com.dustinmcafee.dongadeuce.server.ServerConfig
import com.dustinmcafee.dongadeuce.platform.createServer
import com.dustinmcafee.dongadeuce.tls.generateOrLoadCertificate
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import com.dustinmcafee.dongadeuce.platform.ServerWrapper

/**
 * Android foreground service that runs the DongADeuce dedicated game server.
 * Keeps the server alive even when the app is in the background.
 */
class DedicatedServerService : Service() {

    private var server: ServerWrapper? = null
    private var lobbyManager: LobbyManager? = null
    private var serviceScope: CoroutineScope? = null
    private var notificationManager: ServerNotificationManager? = null
    private var stopReceiver: BroadcastReceiver? = null

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _serverPort = MutableStateFlow(9090)
        val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

        private val _activeGameCount = MutableStateFlow(0)
        val activeGameCount: StateFlow<Int> = _activeGameCount.asStateFlow()

        private val _serverIpAddress = MutableStateFlow("")
        val serverIpAddress: StateFlow<String> = _serverIpAddress.asStateFlow()

        private val _serverFingerprint = MutableStateFlow<String?>(null)
        val serverFingerprint: StateFlow<String?> = _serverFingerprint.asStateFlow()

        private const val PREFS_NAME = "dongadeuce_server"
        private const val KEY_RUNNING = "server_running"
        private const val KEY_PORT = "server_port"
        private const val KEY_MAX_GAMES = "server_max_games"
        private const val KEY_MAX_PLAYERS = "server_max_players"
        private const val KEY_TLS_ENABLED = "server_tls_enabled"

        fun startServer(context: Context, port: Int, maxGames: Int, maxPlayers: Int, tlsEnabled: Boolean = false) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_RUNNING, true)
                .putInt(KEY_PORT, port)
                .putInt(KEY_MAX_GAMES, maxGames)
                .putInt(KEY_MAX_PLAYERS, maxPlayers)
                .putBoolean(KEY_TLS_ENABLED, tlsEnabled)
                .apply()

            val intent = Intent(context, DedicatedServerService::class.java).apply {
                putExtra(KEY_PORT, port)
                putExtra(KEY_MAX_GAMES, maxGames)
                putExtra(KEY_MAX_PLAYERS, maxPlayers)
                putExtra(KEY_TLS_ENABLED, tlsEnabled)
            }
            context.startForegroundService(intent)
        }

        fun stopServer(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_RUNNING, false).apply()

            context.stopService(Intent(context, DedicatedServerService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (_isRunning.value) return START_STICKY

        val port = intent?.getIntExtra(KEY_PORT, 9090) ?: 9090
        val maxGames = intent?.getIntExtra(KEY_MAX_GAMES, 100) ?: 100
        val maxPlayers = intent?.getIntExtra(KEY_MAX_PLAYERS, 6) ?: 6
        val tlsEnabled = intent?.getBooleanExtra(KEY_TLS_ENABLED, false) ?: false

        notificationManager = ServerNotificationManager(this)
        val notification = notificationManager!!.buildNotification(port, 0, if (tlsEnabled) "TLS" else null)
        startForeground(ServerNotificationManager.NOTIFICATION_ID, notification)

        // Register stop receiver
        stopReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                stopServer(this@DedicatedServerService)
            }
        }
        registerReceiver(
            stopReceiver,
            IntentFilter(ServerNotificationManager.ACTION_STOP),
            RECEIVER_NOT_EXPORTED
        )

        // Heavy work (cert generation, Netty startup) must be off the main thread
        CoroutineScope(Dispatchers.IO).launch {
            startDedicatedServer(port, maxGames, maxPlayers, tlsEnabled)
        }

        return START_STICKY
    }

    private fun startDedicatedServer(port: Int, maxGames: Int, maxPlayers: Int, tlsEnabled: Boolean = false) {
        val config = ServerConfig(
            port = port,
            maxGames = maxGames,
            maxPlayersPerGame = maxPlayers,
            tlsEnabled = tlsEnabled
        )
        val manager = LobbyManager(config)
        lobbyManager = manager

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        serviceScope = scope

        val tlsConfig = if (tlsEnabled) {
            try {
                val keystorePath = java.io.File(filesDir, "server.jks").absolutePath
                val certInfo = generateOrLoadCertificate(keystorePath = keystorePath)
                _serverFingerprint.value = certInfo.fingerprint
                certInfo.toServerTlsConfig()
            } catch (e: Exception) {
                android.util.Log.e("DongADeuceServer", "TLS cert generation failed, starting without TLS", e)
                _serverFingerprint.value = null
                null
            }
        } else {
            _serverFingerprint.value = null
            null
        }

        val srv = createServer(port, tlsConfig = tlsConfig, module = {
            install(WebSockets) {
                pingPeriodMillis = 15000
                timeoutMillis = 30000
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                get("/api/health") {
                    call.respondText(
                        buildJsonObject {
                            put("status", "ok")
                            put("activeGames", manager.getRoomCount())
                        }.toString(),
                        ContentType.Application.Json
                    )
                }

                get("/api/games") {
                    val games = manager.listOpenGames()
                    call.respondText(
                        buildJsonObject {
                            putJsonArray("games") {
                                games.forEach { info ->
                                    add(buildJsonObject {
                                        put("code", info.code)
                                        put("playerCount", info.playerCount)
                                        put("createdAt", info.createdAt)
                                    })
                                }
                            }
                        }.toString(),
                        ContentType.Application.Json
                    )
                }

                post("/api/games") {
                    val room = manager.createGame()
                    if (room == null) {
                        call.respondText(
                            buildJsonObject { put("error", "Server is full") }.toString(),
                            ContentType.Application.Json,
                            HttpStatusCode.ServiceUnavailable
                        )
                        return@post
                    }
                    call.respondText(
                        buildJsonObject { put("code", room.code) }.toString(),
                        ContentType.Application.Json,
                        HttpStatusCode.Created
                    )
                }

                delete("/api/games/{code}") {
                    val code = call.parameters["code"] ?: run {
                        call.respondText(
                            buildJsonObject { put("error", "Missing game code") }.toString(),
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest
                        )
                        return@delete
                    }
                    manager.removeGame(code)
                    call.respondText(
                        buildJsonObject { put("status", "removed") }.toString(),
                        ContentType.Application.Json
                    )
                }

                webSocket("/game/{code}") {
                    val code = call.parameters["code"]
                    if (code == null) {
                        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing game code"))
                        return@webSocket
                    }

                    val room = manager.getRoom(code)
                    if (room == null) {
                        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Game not found: $code"))
                        return@webSocket
                    }

                    room.handleConnection(this)

                    if (room.isEmpty()) {
                        manager.removeGame(code)
                    }
                }
            }
        })

        server = srv
        srv.start()

        _serverPort.value = port
        _serverIpAddress.value = getDeviceIpAddress()
        _isRunning.value = true

        // Periodic cleanup + notification update
        scope.launch {
            while (isActive) {
                delay(30_000)
                manager.cleanupIdleRooms()
                val count = manager.getRoomCount()
                _activeGameCount.value = count
                updateNotification(port, count)
            }
        }
    }

    private fun updateNotification(port: Int, gameCount: Int) {
        val notification = notificationManager?.buildNotification(port, gameCount) ?: return
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm.notify(ServerNotificationManager.NOTIFICATION_ID, notification)
    }

    private fun getDeviceIpAddress(): String {
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return "unknown"
        val linkProperties: LinkProperties = cm.getLinkProperties(network) ?: return "unknown"

        return linkProperties.linkAddresses
            .map { it.address }
            .firstOrNull { !it.isLoopbackAddress && it.address.size == 4 }
            ?.hostAddress ?: "unknown"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        server?.stop(500, 1000)
        serviceScope?.cancel()
        _isRunning.value = false
        _activeGameCount.value = 0
        _serverIpAddress.value = ""
        _serverFingerprint.value = null
    }
}

