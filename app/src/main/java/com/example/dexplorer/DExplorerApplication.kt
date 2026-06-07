package com.example.dexplorer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.example.dexplorer.core.util.FileLogger
import com.example.dexplorer.core.util.ThumbnailGenerator

@HiltAndroidApp
class DExplorerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FileLogger.init(this)
        ThumbnailGenerator.init(this)
    }
}