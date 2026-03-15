package com.dustinmcafee.dongadeuce.tls

import kotlinx.serialization.Serializable

/**
 * Server-side TLS configuration passed to createServer().
 */
data class ServerTlsConfig(
    val keystorePath: String,
    val keystorePassword: String,
    val keyAlias: String = "dongadeuce",
    val privateKeyPassword: String
)

/**
 * Result of generating or loading a self-signed certificate.
 */
data class CertificateInfo(
    val keystorePath: String,
    val keystorePassword: String,
    val keyAlias: String,
    val privateKeyPassword: String,
    val fingerprint: String
) {
    fun toServerTlsConfig() = ServerTlsConfig(
        keystorePath = keystorePath,
        keystorePassword = keystorePassword,
        keyAlias = keyAlias,
        privateKeyPassword = privateKeyPassword
    )
}

/**
 * A trusted server entry stored by the client.
 */
@Serializable
data class TrustedServer(
    val host: String,
    val port: Int,
    val fingerprint: String,
    val trustedAt: Long,
    val label: String = ""
)

/**
 * Client-side trust decision returned by TOFU callback.
 */
enum class TrustDecision {
    ACCEPT,
    REJECT
}

/**
 * Callback for TOFU verification. The UI layer implements this to show a dialog.
 */
typealias TofuVerifier = suspend (host: String, port: Int, fingerprint: String) -> TrustDecision
