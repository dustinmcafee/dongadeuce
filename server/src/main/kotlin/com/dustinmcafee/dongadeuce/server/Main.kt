package com.dustinmcafee.dongadeuce.server

import com.dustinmcafee.dongadeuce.tls.ServerTlsConfig
import com.dustinmcafee.dongadeuce.tls.generateOrLoadCertificate
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.time.Duration

/**
 * JVM-specific: Load ServerConfig from environment variables.
 */
fun ServerConfig.Companion.fromEnv(): ServerConfig {
    return ServerConfig(
        port = System.getenv("PORT")?.toIntOrNull() ?: 9090,
        maxGames = System.getenv("MAX_GAMES")?.toIntOrNull() ?: 100,
        maxPlayersPerGame = System.getenv("MAX_PLAYERS")?.toIntOrNull() ?: 6,
        idleTimeoutMinutes = System.getenv("IDLE_TIMEOUT_MINUTES")?.toLongOrNull() ?: 60,
        tlsEnabled = System.getenv("TLS_ENABLED")?.toBooleanStrictOrNull() ?: false
    )
}

/**
 * DongADeuce Dedicated Game Server
 *
 * Provides REST API for game management and WebSocket endpoints for gameplay.
 * All game logic is handled by GameEngine (shared with P2P mode).
 */
fun main() {
    val config = ServerConfig.fromEnv()
    val lobbyManager = LobbyManager(config)

    val protocol = if (config.tlsEnabled) "wss" else "ws"
    println("Starting DongADeuce Server on port ${config.port} ($protocol)...")
    println("Max games: ${config.maxGames}, Max players per game: ${config.maxPlayersPerGame}")

    // Generate or load TLS certificate if enabled
    val tlsConfig = if (config.tlsEnabled) {
        val keystorePath = System.getenv("TLS_KEYSTORE_PATH") ?: "./server.jks"
        val certInfo = generateOrLoadCertificate(keystorePath = keystorePath)
        println("TLS enabled. Certificate fingerprint:")
        println("  ${certInfo.fingerprint}")
        certInfo.toServerTlsConfig()
    } else null

    // Periodic cleanup of idle rooms
    val cleanupScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    cleanupScope.launch {
        while (isActive) {
            delay(5 * 60 * 1000) // Every 5 minutes
            lobbyManager.cleanupIdleRooms()
        }
    }

    val serverModule: Application.() -> Unit = {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(30)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                prettyPrint = true
            })
        }

        routing {
            get("/api/health") {
                call.respond(HealthResponse(
                    status = "ok",
                    activeGames = lobbyManager.getRoomCount()
                ))
            }

            get("/api/games") {
                val games = lobbyManager.listOpenGames().map { info ->
                    GameResponse(
                        code = info.code,
                        playerCount = info.playerCount,
                        createdAt = info.createdAt
                    )
                }
                call.respond(GamesListResponse(games = games))
            }

            post("/api/games") {
                val room = lobbyManager.createGame()
                if (room == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("Server is full, cannot create more games"))
                    return@post
                }
                call.respond(HttpStatusCode.Created, GameCreatedResponse(code = room.code))
            }

            delete("/api/games/{code}") {
                val code = call.parameters["code"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing game code"))
                    return@delete
                }
                lobbyManager.removeGame(code)
                call.respond(HttpStatusCode.OK, mapOf("status" to "removed"))
            }

            webSocket("/game/{code}") {
                val code = call.parameters["code"]
                if (code == null) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing game code"))
                    return@webSocket
                }

                val room = lobbyManager.getRoom(code)
                if (room == null) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Game not found: $code"))
                    return@webSocket
                }

                room.handleConnection(this)

                if (room.isEmpty()) {
                    lobbyManager.removeGame(code)
                }
            }
        }
    }

    if (tlsConfig != null) {
        val keystoreFile = java.io.File(tlsConfig.keystorePath)
        val keyStore = KeyStore.getInstance("JKS")
        keystoreFile.inputStream().use { keyStore.load(it, tlsConfig.keystorePassword.toCharArray()) }

        val environment = applicationEngineEnvironment {
            module(serverModule)
            sslConnector(
                keyStore = keyStore,
                keyAlias = tlsConfig.keyAlias,
                keyStorePassword = { tlsConfig.keystorePassword.toCharArray() },
                privateKeyPassword = { tlsConfig.privateKeyPassword.toCharArray() }
            ) {
                port = config.port
            }
        }
        embeddedServer(Netty, environment).start(wait = true)
    } else {
        embeddedServer(Netty, port = config.port, module = serverModule).start(wait = true)
    }

    cleanupScope.cancel()
}

// ==================== Response Models ====================

@Serializable
data class HealthResponse(
    val status: String,
    val activeGames: Int
)

@Serializable
data class GameResponse(
    val code: String,
    val playerCount: Int,
    val createdAt: Long
)

@Serializable
data class GamesListResponse(
    val games: List<GameResponse>
)

@Serializable
data class GameCreatedResponse(
    val code: String
)

@Serializable
data class ErrorResponse(
    val error: String
)
