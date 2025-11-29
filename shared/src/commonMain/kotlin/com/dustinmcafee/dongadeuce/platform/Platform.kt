package com.dustinmcafee.dongadeuce.platform

/**
 * Generates a unique UUID string.
 */
expect fun generateUUID(): String

/**
 * Returns the current time in milliseconds since epoch.
 */
expect fun currentTimeMillis(): Long
