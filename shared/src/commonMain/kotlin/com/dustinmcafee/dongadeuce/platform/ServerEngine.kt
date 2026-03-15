package com.dustinmcafee.dongadeuce.platform

import com.dustinmcafee.dongadeuce.tls.ServerTlsConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*

/**
 * Wrapper for platform-specific embedded server.
 */
interface ServerWrapper {
    fun start(wait: Boolean = false)
    fun stop(gracePeriodMillis: Long, timeoutMillis: Long)
    val certificateFingerprint: String?
}

/**
 * Creates a platform-specific embedded server.
 * - JVM: Netty engine
 * - Android: CIO engine
 *
 * @param tlsConfig If non-null, the server binds with TLS using the given keystore.
 */
expect fun createServer(
    port: Int,
    module: Application.() -> Unit,
    tlsConfig: ServerTlsConfig? = null
): ServerWrapper
