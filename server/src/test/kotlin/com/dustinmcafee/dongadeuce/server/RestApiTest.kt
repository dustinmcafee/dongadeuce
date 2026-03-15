package com.dustinmcafee.dongadeuce.server

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json as installJson
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerCN
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Tests for the dedicated server REST API endpoints.
 */
class RestApiTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private fun ApplicationTestBuilder.configureApp(lobbyManager: LobbyManager) {
        application {
            install(ServerCN) {
                installJson(json)
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
                        call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("Server is full"))
                        return@post
                    }
                    call.respond(HttpStatusCode.Created, GameCreatedResponse(code = room.code))
                }
                delete("/api/games/{code}") {
                    val code = call.parameters["code"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing code"))
                        return@delete
                    }
                    lobbyManager.removeGame(code)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }

    @Test
    fun `health endpoint returns ok`() = testApplication {
        val config = ServerConfig(maxGames = 10)
        val lobbyManager = LobbyManager(config)
        configureApp(lobbyManager)

        val response = client.get("/api/health")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("\"ok\""))
        assertTrue(body.contains("\"activeGames\""))
    }

    @Test
    fun `health endpoint shows active game count`() = testApplication {
        val config = ServerConfig(maxGames = 10)
        val lobbyManager = LobbyManager(config)
        runBlocking {
            lobbyManager.createGame()
            lobbyManager.createGame()
        }
        configureApp(lobbyManager)

        val response = client.get("/api/health")
        val body = response.bodyAsText()
        assertTrue(body.contains("2"))
    }

    @Test
    fun `create game returns 201 with code`() = testApplication {
        val config = ServerConfig(maxGames = 10)
        val lobbyManager = LobbyManager(config)
        configureApp(lobbyManager)

        val response = client.post("/api/games")
        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("\"code\""))
        runBlocking { assertEquals(1, lobbyManager.getRoomCount()) }
    }

    @Test
    fun `create game returns 503 when full`() = testApplication {
        val config = ServerConfig(maxGames = 1)
        val lobbyManager = LobbyManager(config)
        runBlocking { lobbyManager.createGame() } // fill it
        configureApp(lobbyManager)

        val response = client.post("/api/games")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    @Test
    fun `list games returns empty initially`() = testApplication {
        val config = ServerConfig(maxGames = 10)
        val lobbyManager = LobbyManager(config)
        configureApp(lobbyManager)

        val response = client.get("/api/games")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("\"games\""))
    }

    @Test
    fun `list games returns created games`() = testApplication {
        val config = ServerConfig(maxGames = 10)
        val lobbyManager = LobbyManager(config)
        val room = runBlocking { lobbyManager.createGame()!! }
        configureApp(lobbyManager)

        val response = client.get("/api/games")
        val body = response.bodyAsText()
        assertTrue(body.contains(room.code))
    }

    @Test
    fun `delete game removes room`() = testApplication {
        val config = ServerConfig(maxGames = 10)
        val lobbyManager = LobbyManager(config)
        val room = runBlocking { lobbyManager.createGame()!! }
        configureApp(lobbyManager)

        runBlocking { assertEquals(1, lobbyManager.getRoomCount()) }
        val response = client.delete("/api/games/${room.code}")
        assertEquals(HttpStatusCode.OK, response.status)
        runBlocking { assertEquals(0, lobbyManager.getRoomCount()) }
    }
}
