package com.dustinmcafee.dongadeuce.server

/**
 * Configuration for the dedicated game server.
 */
data class ServerConfig(
    val port: Int = 9090,
    val maxGames: Int = 100,
    val maxPlayersPerGame: Int = 6,
    val gameCodeLength: Int = 6,
    val idleTimeoutMinutes: Long = 60
) {
    companion object {
        fun fromEnv(): ServerConfig {
            return ServerConfig(
                port = System.getenv("PORT")?.toIntOrNull() ?: 9090,
                maxGames = System.getenv("MAX_GAMES")?.toIntOrNull() ?: 100,
                maxPlayersPerGame = System.getenv("MAX_PLAYERS")?.toIntOrNull() ?: 6,
                idleTimeoutMinutes = System.getenv("IDLE_TIMEOUT_MINUTES")?.toLongOrNull() ?: 60
            )
        }
    }
}
