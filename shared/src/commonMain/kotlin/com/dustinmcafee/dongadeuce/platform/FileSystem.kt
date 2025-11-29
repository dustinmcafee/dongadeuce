package com.dustinmcafee.dongadeuce.platform

/**
 * Platform-agnostic output stream for writing files.
 */
expect class FileOutputStream : AutoCloseable {
    fun write(bytes: ByteArray, offset: Int, length: Int)
    fun flush()
    override fun close()
}

/**
 * Platform-agnostic input stream for reading files.
 */
expect class FileInputStream : AutoCloseable {
    fun read(buffer: ByteArray, offset: Int, length: Int): Int
    override fun close()
}

/**
 * Platform-agnostic file handle for reading and writing files.
 */
expect class FileHandle {
    val path: String
    fun readText(): String
    fun writeText(content: String)
    fun readBytes(): ByteArray
    fun writeBytes(bytes: ByteArray)
    fun appendBytes(bytes: ByteArray)
    fun appendBytes(bytes: ByteArray, offset: Int, length: Int)
    fun openOutputStream(append: Boolean = false): FileOutputStream
    fun openInputStream(): FileInputStream
    fun exists(): Boolean
    fun delete(): Boolean
    fun mkdirs(): Boolean
    fun listFiles(): List<FileHandle>
    fun isDirectory(): Boolean
    fun child(name: String): FileHandle
    fun length(): Long
}

/**
 * Returns the application data directory for storing persistent data.
 * - Windows: %APPDATA%/CommanderMTG
 * - macOS/Linux: ~/.commandermtg
 * - Android: Context.filesDir
 */
expect fun getAppDataDirectory(): FileHandle

/**
 * Creates a FileHandle from a path string.
 */
expect fun createFileHandle(path: String): FileHandle
