package com.dustinmcafee.dongadeuce.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Platform-specific IO dispatcher for file and network operations.
 */
expect val ioDispatcher: CoroutineDispatcher
