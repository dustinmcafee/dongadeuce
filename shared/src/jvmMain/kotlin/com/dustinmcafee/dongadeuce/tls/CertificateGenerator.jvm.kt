package com.dustinmcafee.dongadeuce.tls

import io.ktor.network.tls.certificates.*
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate

/**
 * Generates a self-signed certificate and stores it in a JKS keystore,
 * or loads an existing one. Uses Ktor's certificate utilities.
 */
fun generateOrLoadCertificate(
    keystorePath: String,
    keystorePassword: String = "dongadeuce",
    keyAlias: String = "dongadeuce",
    privateKeyPassword: String = "dongadeuce"
): CertificateInfo {
    val keystoreFile = File(keystorePath)

    if (!keystoreFile.exists()) {
        keystoreFile.parentFile?.mkdirs()
        val keyStore = buildKeyStore {
            certificate(keyAlias) {
                password = privateKeyPassword
                domains = listOf("localhost", "0.0.0.0")
            }
        }
        keyStore.saveToFile(keystoreFile, keystorePassword)
    }

    val keyStore = KeyStore.getInstance("JKS")
    keystoreFile.inputStream().use { keyStore.load(it, keystorePassword.toCharArray()) }
    val cert = keyStore.getCertificate(keyAlias) as X509Certificate
    val fingerprint = computeFingerprint(cert)

    return CertificateInfo(
        keystorePath = keystorePath,
        keystorePassword = keystorePassword,
        keyAlias = keyAlias,
        privateKeyPassword = privateKeyPassword,
        fingerprint = fingerprint
    )
}

/**
 * Compute SHA-256 fingerprint of a certificate.
 * Returns colon-separated hex string like "AB:CD:EF:..."
 */
fun computeFingerprint(cert: X509Certificate): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(cert.encoded)
    return bytes.joinToString(":") { "%02X".format(it) }
}
