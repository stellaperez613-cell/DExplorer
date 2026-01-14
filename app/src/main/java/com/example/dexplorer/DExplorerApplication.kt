package com.example.dexplorer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DExplorerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Application initialization
    }
}