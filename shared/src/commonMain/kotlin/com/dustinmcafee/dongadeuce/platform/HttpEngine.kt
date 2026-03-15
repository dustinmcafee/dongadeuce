package com.dustinmcafee.dongadeuce.platform

import io.ktor.client.*
import io.ktor.client.engine.*

/**
 * Creates a platform-specific HTTP client engine.
 * - JVM: CIO engine
 * - Android: OkHttp engine
 */
expect fun createHttpClientEngine(): HttpClientEngine

/**
 * Creates a platform-specific HTTP client engine with custom TLS trust.
 * Used for TOFU connections to servers with self-signed certificates.
 *
 * @param trustedFingerprint SHA-256 fingerprint to validate, or null to accept any cert (for probing).
 */
expect fun createTlsHttpClientEngine(trustedFingerprint: String? = null): HttpClientEngine

/**
 * Probes a TLS server to retrieve its certificate's SHA-256 fingerprint.
 * Used during the TOFU flow before the user has accepted the cert.
 */
expect suspend fun probeCertificateFingerprint(host: String, port: Int): String?

/**
 * Downloads a large file using platform-specific streaming that bypasses Ktor's buffering.
 * This is necessary for very large files (500MB+) that would cause OOM with Ktor's internal buffers.
 *
 * @param url The URL to download from
 * @param outputFile The file to write to
 * @param expectedSize Expected file size in bytes (for progress calculation)
 * @param onProgress Callback for progress updates (downloadedBytes, totalBytes)
 */
expect suspend fun streamingDownload(
    url: String,
    outputFile: FileHandle,
    expectedSize: Long,
    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
)
