package com.dustinmcafee.dongadeuce.platform

import com.dustinmcafee.dongadeuce.tls.computeFingerprint
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.*

actual fun createHttpClientEngine(): HttpClientEngine = CIO.create()

actual fun createTlsHttpClientEngine(trustedFingerprint: String?): HttpClientEngine {
    return CIO.create {
        https {
            trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    if (trustedFingerprint == null) return // Accept any (for TOFU probe)
                    val serverCert = chain?.firstOrNull()
                        ?: throw SSLException("No server certificate")
                    val serverFingerprint = computeFingerprint(serverCert)
                    if (serverFingerprint != trustedFingerprint) {
                        throw SSLException(
                            "Certificate fingerprint mismatch. Expected: $trustedFingerprint, Got: $serverFingerprint"
                        )
                    }
                }
            }
        }
    }
}

actual suspend fun probeCertificateFingerprint(host: String, port: Int): String? {
    return withContext(Dispatchers.IO) {
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            }), null)
            val socket = sslContext.socketFactory.createSocket(host, port) as SSLSocket
            socket.startHandshake()
            val cert = socket.session.peerCertificates[0] as X509Certificate
            val fp = computeFingerprint(cert)
            socket.close()
            fp
        } catch (e: Exception) {
            null
        }
    }
}

actual suspend fun streamingDownload(
    url: String,
    outputFile: FileHandle,
    expectedSize: Long,
    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
) {
    withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 60_000
        connection.readTimeout = 900_000 // 15 minutes for large files

        try {
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error: ${connection.responseCode} ${connection.responseMessage}")
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: expectedSize

            connection.inputStream.use { inputStream ->
                outputFile.openOutputStream(append = false).use { outputStream ->
                    val buffer = ByteArray(64 * 1024) // 64KB buffer
                    var downloadedBytes = 0L
                    var lastProgressTime = System.currentTimeMillis()

                    while (true) {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break

                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Report progress every 500ms
                        val now = System.currentTimeMillis()
                        if (now - lastProgressTime >= 500) {
                            onProgress(downloadedBytes, totalBytes)
                            lastProgressTime = now
                        }
                    }
                    outputStream.flush()
                    onProgress(downloadedBytes, totalBytes) // Final progress
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}

// Extension to use AutoCloseable with our FileOutputStream
private inline fun <T : AutoCloseable, R> T.use(block: (T) -> R): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        try {
            close()
        } catch (closeException: Throwable) {
            exception?.addSuppressed(closeException)
        }
    }
}
