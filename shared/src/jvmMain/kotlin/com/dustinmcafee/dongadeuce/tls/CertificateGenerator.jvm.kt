package com.dustinmcafee.dongadeuce.tls

import io.ktor.network.tls.certificates.*
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Date

private const val RENEWAL_THRESHOLD_DAYS = 30L
private const val VALIDITY_DAYS = 3650L // 10 years

/**
 * Generates a self-signed certificate and stores it in a JKS keystore,
 * or loads an existing one. If the existing cert is expired or expiring
 * within 30 days, the keystore is regenerated.
 */
fun generateOrLoadCertificate(
    keystorePath: String,
    keystorePassword: String = "dongadeuce",
    keyAlias: String = "dongadeuce",
    privateKeyPassword: String = "dongadeuce"
): CertificateInfo {
    val keystoreFile = File(keystorePath)

    // Check if existing cert needs renewal
    if (keystoreFile.exists()) {
        val needsRenewal = try {
            val ks = KeyStore.getInstance("JKS")
            keystoreFile.inputStream().use { ks.load(it, keystorePassword.toCharArray()) }
            val cert = ks.getCertificate(keyAlias) as? X509Certificate
            cert == null || isCertExpiringSoon(cert)
        } catch (_: Exception) {
            true // Corrupted keystore — regenerate
        }

        if (needsRenewal) {
            keystoreFile.delete()
        }
    }

    if (!keystoreFile.exists()) {
        keystoreFile.parentFile?.mkdirs()
        val keyStore = buildKeyStore {
            certificate(keyAlias) {
                password = privateKeyPassword
                daysValid = VALIDITY_DAYS
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
 * Check if a certificate is expired or expiring within the renewal threshold.
 */
fun isCertExpiringSoon(cert: X509Certificate): Boolean {
    val thresholdMs = RENEWAL_THRESHOLD_DAYS * 24 * 60 * 60 * 1000
    val expiryDate = cert.notAfter
    return expiryDate.time - System.currentTimeMillis() < thresholdMs
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
