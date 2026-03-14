package com.dustinmcafee.dongadeuce.server

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
import java.time.Duration

/**
 * DongADeuce Dedicated Game Server
 *
 * Provides REST API for game management and WebSocket endpoints for gameplay.
 * All game logic is handled by GameEngine (shared with P2P mode).
 */
fun main() {
    val config = ServerConfig.fromEnv()
    val lobbyManager = LobbyManager(config)

    println("Starting DongADeuce Server on port ${config.port}...")
    println("Max games: ${config.maxGames}, Max players per game: ${config.maxPlayersPerGame}")

    // Periodic cleanup of idle rooms
    val cleanupScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    cleanupScope.launch {
        while (isActive) {
            delay(5 * 60 * 1000) // Every 5 minutes
            lobbyManager.cleanupIdleRooms()
        }
    }

    embeddedServer(Netty, port = config.port) {
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
            // ==================== REST API ====================

            /**
             * GET /api/health - Health check
             */
            get("/api/health") {
                call.respond(HealthResponse(
                    status = "ok",
                    activeGames = lobbyManager.getRoomCount()
                ))
            }

            /**
             * GET /api/games - List open games
             */
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

            /**
             * POST /api/games - Create a new game room
             */
            post("/api/games") {
                val room = lobbyManager.createGame()
                if (room == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("Server is full, cannot create more games"))
                    return@post
                }
                call.respond(HttpStatusCode.Created, GameCreatedResponse(code = room.code))
            }

            /**
             * DELETE /api/games/{code} - Remove a game room
             */
            delete("/api/games/{code}") {
                val code = call.parameters["code"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing game code"))
                    return@delete
                }
                lobbyManager.removeGame(code)
                call.respond(HttpStatusCode.OK, mapOf("status" to "removed"))
            }

            // ==================== WebSocket Endpoints ====================

            /**
             * WS /game/{code} - Join a specific game room
             */
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

                // Clean up empty rooms after disconnect
                if (room.isEmpty()) {
                    lobbyManager.removeGame(code)
                }
            }
        }
    }.start(wait = true)

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
