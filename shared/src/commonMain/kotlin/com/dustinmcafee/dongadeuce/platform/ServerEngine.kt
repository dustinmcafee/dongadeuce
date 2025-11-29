package com.dustinmcafee.dongadeuce.platform

import io.ktor.server.application.*
import io.ktor.server.engine.*

/**
 * Wrapper for platform-specific embedded server.
 */
interface ServerWrapper {
    fun start(wait: Boolean = false)
    fun stop(gracePeriodMillis: Long, timeoutMillis: Long)
}

/**
 * Creates a platform-specific embedded server.
 * - JVM: Netty engine
 * - Android: CIO engine
 */
expect fun createServer(
    port: Int,
    module: Application.() -> Unit
): ServerWrapper
