package com.dustinmcafee.dongadeuce.platform

import com.dustinmcafee.dongadeuce.tls.ServerTlsConfig
import com.dustinmcafee.dongadeuce.tls.computeFingerprint
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * Android implementation wrapping CIO (plain) or Netty (TLS) server.
 */
private class AndroidServerWrapper(
    private val server: ApplicationEngine,
    override val certificateFingerprint: String? = null
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
    module: Application.() -> Unit,
    tlsConfig: ServerTlsConfig?
): ServerWrapper {
    if (tlsConfig != null) {
        // Use Netty for TLS — CIO's TLS implementation is unreliable for WebSockets
        val keystoreFile = File(tlsConfig.keystorePath)
        val keyStore = KeyStore.getInstance("PKCS12")
        keystoreFile.inputStream().use { keyStore.load(it, tlsConfig.keystorePassword.toCharArray()) }

        val cert = keyStore.getCertificate(tlsConfig.keyAlias) as X509Certificate
        val fingerprint = computeFingerprint(cert)

        val environment = applicationEngineEnvironment {
            this.module(module)
            sslConnector(
                keyStore = keyStore,
                keyAlias = tlsConfig.keyAlias,
                keyStorePassword = { tlsConfig.keystorePassword.toCharArray() },
                privateKeyPassword = { tlsConfig.privateKeyPassword.toCharArray() }
            ) {
                this.port = port
            }
        }

        val server = embeddedServer(Netty, environment)
        return AndroidServerWrapper(server, fingerprint)
    } else {
        // Use CIO for plain HTTP — lightweight, no TLS needed
        val server = embeddedServer(CIO, port = port, module = module)
        return AndroidServerWrapper(server)
    }
}
