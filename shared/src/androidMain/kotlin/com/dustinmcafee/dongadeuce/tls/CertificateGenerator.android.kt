package com.dustinmcafee.dongadeuce.tls

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.X509Certificate
import java.util.*

private const val RENEWAL_THRESHOLD_DAYS = 30L
private const val VALIDITY_DAYS = 3650L
private const val KEYSTORE_TYPE = "PKCS12"

/**
 * Generates a self-signed certificate and stores it in a PKCS12 keystore,
 * or loads an existing one. If the existing cert is expired or expiring
 * within 30 days, the keystore is regenerated.
 *
 * Uses Bouncy Castle for cert generation (Android doesn't support JKS or sun.security.x509).
 */
fun generateOrLoadCertificate(
    keystorePath: String,
    keystorePassword: String = "dongadeuce",
    keyAlias: String = "dongadeuce",
    privateKeyPassword: String = "dongadeuce"
): CertificateInfo {
    val keystoreFile = File(keystorePath)

    if (keystoreFile.exists()) {
        val needsRenewal = try {
            val ks = KeyStore.getInstance(KEYSTORE_TYPE)
            keystoreFile.inputStream().use { ks.load(it, keystorePassword.toCharArray()) }
            val cert = ks.getCertificate(keyAlias) as? X509Certificate
            cert == null || isCertExpiringSoon(cert)
        } catch (_: Exception) {
            true
        }

        if (needsRenewal) {
            keystoreFile.delete()
        }
    }

    if (!keystoreFile.exists()) {
        keystoreFile.parentFile?.mkdirs()
        generatePkcs12Keystore(keystoreFile, keystorePassword, keyAlias, privateKeyPassword)
    }

    val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
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
 * Generate a self-signed RSA cert using Bouncy Castle and save as PKCS12.
 */
private fun generatePkcs12Keystore(
    outputFile: File,
    keystorePassword: String,
    keyAlias: String,
    privateKeyPassword: String
) {
    // Generate RSA 2048 key pair
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048, SecureRandom())
    val keyPair = keyPairGenerator.generateKeyPair()

    // Build self-signed X.509 certificate
    val issuer = X500Name("CN=DongADeuce Server, O=DongADeuce")
    val serial = BigInteger(128, SecureRandom())
    val notBefore = Date()
    val notAfter = Date(notBefore.time + VALIDITY_DAYS * 24 * 60 * 60 * 1000)

    val certBuilder = JcaX509v3CertificateBuilder(
        issuer, serial, notBefore, notAfter, issuer, keyPair.public
    )

    // Add Subject Alternative Names (localhost + wildcard IP)
    val sans = GeneralNames(arrayOf(
        GeneralName(GeneralName.dNSName, "localhost"),
        GeneralName(GeneralName.iPAddress, "0.0.0.0")
    ))
    certBuilder.addExtension(Extension.subjectAlternativeName, false, sans)
    certBuilder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))

    // Sign with SHA256withRSA
    val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
    val certHolder = certBuilder.build(signer)
    val cert = JcaX509CertificateConverter().getCertificate(certHolder)

    // Store in PKCS12 keystore
    val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
    keyStore.load(null, null)
    keyStore.setKeyEntry(
        keyAlias,
        keyPair.private,
        privateKeyPassword.toCharArray(),
        arrayOf(cert)
    )

    FileOutputStream(outputFile).use { fos ->
        keyStore.store(fos, keystorePassword.toCharArray())
    }
}

fun isCertExpiringSoon(cert: X509Certificate): Boolean {
    val thresholdMs = RENEWAL_THRESHOLD_DAYS * 24 * 60 * 60 * 1000
    return cert.notAfter.time - System.currentTimeMillis() < thresholdMs
}

fun computeFingerprint(cert: X509Certificate): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(cert.encoded)
    return bytes.joinToString(":") { "%02X".format(it) }
}
