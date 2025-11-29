package com.dustinmcafee.dongadeuce.platform

import java.io.File

actual class FileOutputStream(private val stream: java.io.FileOutputStream) : AutoCloseable {
    actual fun write(bytes: ByteArray, offset: Int, length: Int) {
        stream.write(bytes, offset, length)
    }

    actual fun flush() {
        stream.flush()
    }

    actual override fun close() {
        stream.close()
    }
}

actual class FileInputStream(private val stream: java.io.FileInputStream) : AutoCloseable {
    actual fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return stream.read(buffer, offset, length)
    }

    actual override fun close() {
        stream.close()
    }
}

actual class FileHandle(private val file: File) {
    actual val path: String get() = file.absolutePath

    actual fun readText(): String = file.readText()

    actual fun writeText(content: String) {
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    actual fun readBytes(): ByteArray = file.readBytes()

    actual fun writeBytes(bytes: ByteArray) {
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    actual fun appendBytes(bytes: ByteArray) {
        file.parentFile?.mkdirs()
        file.appendBytes(bytes)
    }

    actual fun appendBytes(bytes: ByteArray, offset: Int, length: Int) {
        file.parentFile?.mkdirs()
        java.io.FileOutputStream(file, true).use { fos ->
            fos.write(bytes, offset, length)
        }
    }

    actual fun openOutputStream(append: Boolean): FileOutputStream {
        file.parentFile?.mkdirs()
        return FileOutputStream(java.io.FileOutputStream(file, append))
    }

    actual fun openInputStream(): FileInputStream {
        return FileInputStream(java.io.FileInputStream(file))
    }

    actual fun exists(): Boolean = file.exists()

    actual fun delete(): Boolean = file.delete()

    actual fun mkdirs(): Boolean = file.mkdirs()

    actual fun listFiles(): List<FileHandle> =
        file.listFiles()?.map { FileHandle(it) } ?: emptyList()

    actual fun isDirectory(): Boolean = file.isDirectory

    actual fun child(name: String): FileHandle = FileHandle(File(file, name))

    actual fun length(): Long = file.length()
}

actual fun getAppDataDirectory(): FileHandle {
    val os = System.getProperty("os.name").lowercase()
    val path = when {
        os.contains("win") -> "${System.getenv("APPDATA")}/CommanderMTG"
        os.contains("mac") -> "${System.getProperty("user.home")}/.commandermtg"
        else -> "${System.getProperty("user.home")}/.commandermtg"
    }
    return FileHandle(File(path).also { it.mkdirs() })
}

actual fun createFileHandle(path: String): FileHandle = FileHandle(File(path))
