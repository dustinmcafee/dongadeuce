# Optimizing File Decryption on QCOM ARM64 Android

If you're experiencing high RAM and CPU usage while decrypting files on your QCOM ARM64 Android device, the issue is likely your implementation, not the algorithm itself.

Your device has specialized hardware to handle this. By using the correct algorithm and a streaming process, you can achieve extremely fast, low-resource decryption.

---

## The Solution: Hardware-Accelerated AES + Streaming

The most efficient decryption scheme you can use is **AES** (Advanced Encryption Standard). Your QCOM ARM64 chip contains dedicated hardware instructions, part of the **ARMv8-A Cryptography Extensions (CE)**, specifically designed to accelerate AES operations.

This is the "hardware" you're looking for. It's even more efficient than general-purpose SIMD (NEON) because these instructions are built for one purpose: performing AES encryption and decryption at high speed.

---

## How to Use Hardware-Accelerated Decryption

The best part is that you don't need to do anything complex. Android's standard cryptography libraries are designed to use this hardware automatically.

### 1. The Best Algorithm: AES-GCM

For high performance and modern security, use **AES-GCM**. This is an "authenticated" cipher, meaning it encrypts and verifies the data in one pass, which is very efficient.

The standard transformation string to use in your Java/Kotlin code is:
`"AES/GCM/NoPadding"`

### 2. The Right Code: Use `javax.crypto` (The Default)

When you call `Cipher.getInstance("AES/GCM/NoPadding")`, you are *already* getting the hardware-accelerated version.

Here’s why:
1.  On modern Android, the default security provider is **Conscrypt**.
2.  Conscrypt is a high-performance library that wraps **BoringSSL** (Google's crypto library).
3.  BoringSSL is compiled to **automatically detect and use** the ARMv8-A Crypto Extensions (the AES hardware) whenever it runs on a chip that has them, like your QCOM ARM64.

**The most important rule:** **Do not** specify a provider, like `"BouncyCastle"`.

```kotlin
// 👍 DO THIS (Correct - Uses Hardware)
val cipher = Cipher.getInstance("AES/GCM/NoPadding")

// 🛑 DON'T DO THIS (Incorrect - Forces Slow Software)
val cipher = Cipher.getInstance("AES/GCM/NoPadding", "BouncyCastle")

Requesting the Bouncy Castle provider will force your app to use a pure-Java software implementation, which is much slower and will cause the high CPU/RAM usage you're seeing.

---

## What About SIMD (NEON)?

You were right to ask about SIMD! The dedicated ARM AES instructions actually use the 128-bit **NEON (SIMD) registers** to perform their operations. So, while they are specialized instructions, they are part of the same Advanced SIMD hardware unit.

General-purpose NEON instructions (not the special AES ones) are used to accelerate ciphers that *don't* have dedicated hardware, like **ChaCha20-Poly1305**. However, on your device, **hardware-accelerated AES-GCM will be significantly faster** than NEON-accelerated ChaCha20.

---

## Why Is My Decryption Taking So Much RAM/CPU?

Your problem is very likely **not** the *algorithm* itself, but how you are *implementing* it.

**The most common cause of high RAM/CPU usage is reading the entire encrypted file into a byte array in memory** and then decrypting it all at once. This is extremely inefficient.

### The Solution: Use Streaming

You **must** use a streaming approach. This decrypts the file in small, manageable chunks, keeping your RAM and CPU usage incredibly low, no matter how big the file is.

The easiest way is with `CipherInputStream`:

```kotlin
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// --- Example setup (you already have this) ---
val key: SecretKey = /* your_secret_key */
val iv: ByteArray = /* your_iv_for_gcm */
val inputFile = File(context.filesDir, "my_large_file.encrypted")
val outputFile = File(context.filesDir, "my_large_file.decrypted")

// 1. Get the hardware-accelerated cipher
val cipher = Cipher.getInstance("AES/GCM/NoPadding")
val gcmSpec = GCMParameterSpec(128, iv) // 128-bit auth tag
cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

// 2. Use CipherInputStream to stream the decryption
FileInputStream(inputFile).use { fileInput ->
    CipherInputStream(fileInput, cipher).use { cipherInput ->
        FileOutputStream(outputFile).use { fileOutput ->
            
            // 3. Use a small, fixed-size buffer
            // This is the *only* memory you'll use, regardless of file size
            val buffer = ByteArray(8192) // 8KB buffer
            var bytesRead: Int
            
            // 4. Read -> Decrypt -> Write in one loop
            // The decryption happens automatically as you read
            while (cipherInput.read(buffer).also { bytesRead = it } != -1) {
                fileOutput.write(buffer, 0, bytesRead)
            }
        }
    }
}

// Your file is now decrypted with minimal RAM/CPU usage.

By combining the **correct algorithm** (`AES/GCM/NoPadding`) with the **correct implementation** (`CipherInputStream`), you will fully utilize your QCOM's hardware and solve your performance problems.
