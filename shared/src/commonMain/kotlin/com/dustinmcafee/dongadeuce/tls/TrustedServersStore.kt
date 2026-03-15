package com.dustinmcafee.dongadeuce.tls

import com.dustinmcafee.dongadeuce.platform.FileHandle
import com.dustinmcafee.dongadeuce.platform.currentTimeMillis
import com.dustinmcafee.dongadeuce.platform.getAppDataDirectory
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TrustedServersData(
    val servers: List<TrustedServer> = emptyList()
)

/**
 * Manages the client's list of trusted server certificate fingerprints.
 * Uses the same FileHandle/getAppDataDirectory() pattern as UserSettings.
 */
class TrustedServersStore(
    private val dataDir: FileHandle = getAppDataDirectory()
) {
    private val file: FileHandle = dataDir.child("trusted_servers.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        dataDir.mkdirs()
    }

    fun load(): TrustedServersData {
        return try {
            if (file.exists()) {
                json.decodeFromString<TrustedServersData>(file.readText())
            } else {
                TrustedServersData()
            }
        } catch (e: Exception) {
            TrustedServersData()
        }
    }

    private fun save(data: TrustedServersData) {
        try {
            file.writeText(json.encodeToString(data))
        } catch (_: Exception) {}
    }

    fun getTrustedFingerprint(host: String, port: Int): String? {
        return load().servers.find { it.host == host && it.port == port }?.fingerprint
    }

    fun trustServer(host: String, port: Int, fingerprint: String, label: String = "") {
        val data = load()
        val filtered = data.servers.filter { !(it.host == host && it.port == port) }
        val updated = filtered + TrustedServer(
            host = host,
            port = port,
            fingerprint = fingerprint,
            trustedAt = currentTimeMillis(),
            label = label
        )
        save(TrustedServersData(updated))
    }

    fun removeServer(host: String, port: Int) {
        val data = load()
        save(TrustedServersData(data.servers.filter { !(it.host == host && it.port == port) }))
    }

    fun isServerTrusted(host: String, port: Int, fingerprint: String): Boolean {
        return getTrustedFingerprint(host, port) == fingerprint
    }
}
