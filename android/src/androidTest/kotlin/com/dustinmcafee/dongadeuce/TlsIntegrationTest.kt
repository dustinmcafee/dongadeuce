package com.dustinmcafee.dongadeuce

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.network.*
import com.dustinmcafee.dongadeuce.platform.probeCertificateFingerprint
import com.dustinmcafee.dongadeuce.tls.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Android instrumented tests for TLS: cert generation, PKCS12 keystore,
 * Netty TLS server, OkHttp TLS client, TOFU flow.
 */
@RunWith(AndroidJUnit4::class)
class TlsIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createTestDeck(): Deck {
        return Deck(
            name = "Test Deck",
            commander = Card(name = "Test Commander", type = "Legendary Creature", power = "4", toughness = "4"),
            cards = (1..99).map { i ->
                if (i <= 35) Card(name = "Land $i", type = "Basic Land")
                else Card(name = "Creature $i", type = "Creature", power = "2", toughness = "2")
            }
        )
    }

    private fun findFreePort(): Int {
        val socket = java.net.ServerSocket(0)
        val port = socket.localPort
        socket.close()
        return port
    }

    // ==================== Certificate Generation ====================

    @Test
    fun certGeneration_createsPkcs12Keystore() {
        val keystorePath = File(context.filesDir, "test_cert.p12").absolutePath
        try {
            val certInfo = generateOrLoadCertificate(keystorePath = keystorePath)

            // Keystore file exists
            assertTrue("Keystore file should exist", File(keystorePath).exists())

            // Fingerprint is SHA-256 format (colon-separated hex)
            assertTrue("Fingerprint should be SHA-256 format",
                certInfo.fingerprint.matches(Regex("([0-9A-F]{2}:){31}[0-9A-F]{2}")))

            // Can load as PKCS12
            val ks = java.security.KeyStore.getInstance("PKCS12")
            File(keystorePath).inputStream().use {
                ks.load(it, "dongadeuce".toCharArray())
            }
            assertNotNull("Should have key entry", ks.getKey("dongadeuce", "dongadeuce".toCharArray()))
            assertNotNull("Should have cert", ks.getCertificate("dongadeuce"))
        } finally {
            File(keystorePath).delete()
        }
    }

    @Test
    fun certGeneration_loadExistingKeystore() {
        val keystorePath = File(context.filesDir, "test_cert_reload.p12").absolutePath
        try {
            val first = generateOrLoadCertificate(keystorePath = keystorePath)
            val second = generateOrLoadCertificate(keystorePath = keystorePath)

            // Same fingerprint on reload
            assertEquals("Fingerprint should be stable", first.fingerprint, second.fingerprint)
        } finally {
            File(keystorePath).delete()
        }
    }

    // ==================== Trusted Servers Store ====================

    @Test
    fun trustedServersStore_trustAndLookup() {
        val store = TrustedServersStore()
        val fingerprint = "AA:BB:CC:DD:EE:FF"

        assertNull(store.getTrustedFingerprint("testhost", 9090))

        store.trustServer("testhost", 9090, fingerprint)
        assertEquals(fingerprint, store.getTrustedFingerprint("testhost", 9090))

        assertTrue(store.isServerTrusted("testhost", 9090, fingerprint))
        assertFalse(store.isServerTrusted("testhost", 9090, "XX:YY:ZZ"))

        store.removeServer("testhost", 9090)
        assertNull(store.getTrustedFingerprint("testhost", 9090))
    }

    private fun getDeviceLanIp(): String {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return "127.0.0.1"
        val linkProperties = cm.getLinkProperties(network) ?: return "127.0.0.1"
        return linkProperties.linkAddresses
            .map { it.address }
            .firstOrNull { !it.isLoopbackAddress && it.address.size == 4 }
            ?.hostAddress ?: "127.0.0.1"
    }

    // ==================== TLS Server + Client ====================

    @Test
    fun tlsServer_startsAndAcceptsTlsConnection() = runBlocking {
        val port = findFreePort()
        val keystorePath = File(context.filesDir, "test_server.p12").absolutePath

        try {
            // Generate cert
            val certInfo = generateOrLoadCertificate(keystorePath = keystorePath)
            val tlsConfig = certInfo.toServerTlsConfig()

            // Start server with TLS (uses Netty on Android)
            val server = GameServer(port = port, maxPlayers = 4, tlsConfig = tlsConfig)
            server.start()
            delay(2000) // Netty startup

            // Probe fingerprint
            val probeFingerprint = probeCertificateFingerprint("localhost", port)
            assertNotNull("Should get fingerprint from probe", probeFingerprint)
            assertEquals("Probe fingerprint should match cert", certInfo.fingerprint, probeFingerprint)

            // Connect client with TLS + auto-accept TOFU
            val client = GameClient()
            val deck = createTestDeck()
            val store = TrustedServersStore()

            val tofuVerifier: TofuVerifier = { _, _, _ -> TrustDecision.ACCEPT }

            val job = launch {
                client.connect(
                    "localhost", port, "TlsTestPlayer", deck,
                    useTls = true,
                    tofuVerifier = tofuVerifier,
                    trustedServersStore = store
                )
            }

            // Wait for connection
            withTimeout(10000) {
                client.connectionState.first { it is ConnectionState.Connected }
            }

            // Verify connected
            assertTrue(client.connectionState.value is ConnectionState.Connected)

            // Verify fingerprint was saved
            assertEquals(certInfo.fingerprint, store.getTrustedFingerprint("localhost", port))

            client.disconnect()
            job.cancel()
            server.stop()
        } finally {
            File(keystorePath).delete()
        }
    }

    @Test
    fun tlsServer_connectsViaLanIp() = runBlocking {
        val port = findFreePort()
        val keystorePath = File(context.filesDir, "test_server_lan.p12").absolutePath
        val lanIp = getDeviceLanIp()

        try {
            val certInfo = generateOrLoadCertificate(keystorePath = keystorePath)
            val server = GameServer(port = port, maxPlayers = 4, tlsConfig = certInfo.toServerTlsConfig())
            server.start()
            delay(2000)

            // Probe via LAN IP (not localhost)
            val probeFingerprint = probeCertificateFingerprint(lanIp, port)
            assertNotNull("Should get fingerprint via LAN IP $lanIp", probeFingerprint)
            assertEquals("Fingerprint via LAN IP should match", certInfo.fingerprint, probeFingerprint)

            // Connect via LAN IP
            val client = GameClient()
            val deck = createTestDeck()
            val store = TrustedServersStore()
            val tofuVerifier: TofuVerifier = { _, _, _ -> TrustDecision.ACCEPT }

            val job = launch {
                client.connect(
                    lanIp, port, "LanIpTestPlayer", deck,
                    useTls = true,
                    tofuVerifier = tofuVerifier,
                    trustedServersStore = store
                )
            }

            withTimeout(10000) {
                client.connectionState.first { it is ConnectionState.Connected }
            }

            assertTrue("Should be connected via LAN IP", client.connectionState.value is ConnectionState.Connected)

            client.disconnect()
            job.cancel()
            server.stop()
        } finally {
            File(keystorePath).delete()
        }
    }

    @Test
    fun tlsServer_rejectsPlainWsConnection() = runBlocking {
        val port = findFreePort()
        val keystorePath = File(context.filesDir, "test_server_reject.p12").absolutePath

        try {
            val certInfo = generateOrLoadCertificate(keystorePath = keystorePath)
            val server = GameServer(port = port, maxPlayers = 4, tlsConfig = certInfo.toServerTlsConfig())
            server.start()
            delay(2000)

            // Try connecting without TLS — should fail
            val client = GameClient()
            val deck = createTestDeck()

            val job = launch {
                client.connect("localhost", port, "PlainTestPlayer", deck, useTls = false)
            }

            // Should get an error, not a successful connection
            withTimeout(5000) {
                client.connectionState.first { it is ConnectionState.Error }
            }

            assertTrue(client.connectionState.value is ConnectionState.Error)

            client.disconnect()
            job.cancel()
            server.stop()
        } finally {
            File(keystorePath).delete()
        }
    }

    @Test
    fun tlsClient_rejectsUntrustedCert() = runBlocking {
        val port = findFreePort()
        val keystorePath = File(context.filesDir, "test_server_untrust.p12").absolutePath

        try {
            val certInfo = generateOrLoadCertificate(keystorePath = keystorePath)
            val server = GameServer(port = port, maxPlayers = 4, tlsConfig = certInfo.toServerTlsConfig())
            server.start()
            delay(2000)

            val client = GameClient()
            val deck = createTestDeck()

            // TOFU verifier that rejects
            val tofuVerifier: TofuVerifier = { _, _, _ -> TrustDecision.REJECT }

            val job = launch {
                client.connect(
                    "localhost", port, "RejectTestPlayer", deck,
                    useTls = true,
                    tofuVerifier = tofuVerifier,
                    trustedServersStore = TrustedServersStore()
                )
            }

            withTimeout(10000) {
                client.connectionState.first { it is ConnectionState.Error }
            }

            val error = client.connectionState.value as ConnectionState.Error
            assertTrue("Error should mention rejection", error.message.contains("rejected", ignoreCase = true))

            client.disconnect()
            job.cancel()
            server.stop()
        } finally {
            File(keystorePath).delete()
        }
    }

    @Test
    fun tlsClient_skipsTofuForTrustedServer() = runBlocking {
        val port = findFreePort()
        val keystorePath = File(context.filesDir, "test_server_trusted.p12").absolutePath

        try {
            val certInfo = generateOrLoadCertificate(keystorePath = keystorePath)
            val server = GameServer(port = port, maxPlayers = 4, tlsConfig = certInfo.toServerTlsConfig())
            server.start()
            delay(2000)

            val store = TrustedServersStore()
            // Pre-trust the server
            store.trustServer("localhost", port, certInfo.fingerprint)

            var tofuCalled = false
            val tofuVerifier: TofuVerifier = { _, _, _ ->
                tofuCalled = true
                TrustDecision.ACCEPT
            }

            val client = GameClient()
            val deck = createTestDeck()

            val job = launch {
                client.connect(
                    "localhost", port, "TrustedTestPlayer", deck,
                    useTls = true,
                    tofuVerifier = tofuVerifier,
                    trustedServersStore = store
                )
            }

            withTimeout(10000) {
                client.connectionState.first { it is ConnectionState.Connected }
            }

            // TOFU verifier should NOT have been called
            assertFalse("TOFU should be skipped for trusted server", tofuCalled)

            client.disconnect()
            job.cancel()
            server.stop()
        } finally {
            File(keystorePath).delete()
        }
    }
}
