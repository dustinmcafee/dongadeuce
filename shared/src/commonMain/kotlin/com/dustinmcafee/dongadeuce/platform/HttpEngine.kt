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
