package com.dustinmcafee.dongadeuce

import android.app.Application
import com.dustinmcafee.dongadeuce.platform.AndroidFileSystem

/**
 * Application class for initializing platform-specific components.
 */
class DongAdeuceApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize the Android file system with the app's files directory
        AndroidFileSystem.initialize(filesDir)
    }
}
