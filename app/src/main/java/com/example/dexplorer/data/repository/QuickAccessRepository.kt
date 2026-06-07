package com.example.dexplorer.data.repository

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuickAccessRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("quick_access", Context.MODE_PRIVATE)
    }

    val internalStoragePath: String =
        Environment.getExternalStorageDirectory().absolutePath

    private val defaultPaths = listOf(
        internalStoragePath,
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath,
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath,
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath,
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath,
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath,
    )

    fun getPinnedPaths(): List<String> {
        val stored = prefs.getString(KEY_ORDER, null)
        return if (stored == null) {
            savePaths(defaultPaths)
            defaultPaths
        } else {
            stored.split(SEP).filter { it.isNotEmpty() }
        }
    }

    fun addPath(path: String) {
        val current = getPinnedPaths().toMutableList()
        if (path !in current) {
            current.add(path)
            savePaths(current)
        }
    }

    fun removePath(path: String) {
        if (isProtected(path)) return
        val current = getPinnedPaths().toMutableList()
        current.remove(path)
        savePaths(current)
    }

    fun isProtected(path: String): Boolean = path == internalStoragePath

    private fun savePaths(paths: List<String>) {
        prefs.edit().putString(KEY_ORDER, paths.joinToString(SEP)).apply()
    }

    companion object {
        private const val KEY_ORDER = "pinned_order"
        private const val SEP = "|"
    }
}
