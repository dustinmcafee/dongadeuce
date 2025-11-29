package com.dustinmcafee.dongadeuce.platform

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*

/**
 * Android implementation wrapping CIO server.
 */
private class AndroidServerWrapper(
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
    val server = embeddedServer(CIO, port = port, module = module)
    return AndroidServerWrapper(server)
}
