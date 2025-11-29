package com.dustinmcafee.dongadeuce.platform

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

/**
 * JVM implementation wrapping Netty server.
 */
private class JvmServerWrapper(
    private val server: ApplicationEngine
) : ServerWrapper {
    override fun start(wait: Boolean) {
        server.start(wait)
    }

    override fun stop(gracePeriodMillis: Long, timeoutMillis: Long) {
        server.stop(gracePeriodMillis, timeoutMillis)
    }
}

actual fun createServer(
    port: Int,
    module: Application.() -> Unit
): ServerWrapper {
    val server = embeddedServer(Netty, port = port, module = module)
    return JvmServerWrapper(server)
}
