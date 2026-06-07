package com.example.dexplorer.data.repository

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import com.example.dexplorer.data.model.StorageVolumeInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageVolumeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

    private val _volumes = MutableStateFlow<List<StorageVolumeInfo>>(emptyList())
    val volumes: StateFlow<List<StorageVolumeInfo>> = _volumes.asStateFlow()

    init {
        refresh()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+: StorageVolumeCallback is the proper way to observe volume state changes.
            // It fires reliably for USB/SD connect, disconnect, and eject from the notification.
            storageManager.registerStorageVolumeCallback(
                context.mainExecutor,
                object : StorageManager.StorageVolumeCallback() {
                    override fun onStateChanged(volume: StorageVolume) { refresh() }
                }
            )
        } else {
            // Pre-API 29 fallback: broadcast receiver for media events.
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addAction(Intent.ACTION_MEDIA_EJECT)
                addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
                addDataScheme("file")
            }
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) { refresh() }
            }, filter)
        }
    }

    fun refresh() {
        _volumes.value = storageManager.storageVolumes
            .filter { it.isRemovable }
            .mapNotNull { buildVolumeInfo(it) }
            .filter { it.isMounted }
    }

    fun getStorageVolume(path: String): StorageVolume? =
        storageManager.storageVolumes.find { getPath(it) == path }

    /** Returns all currently-mounted volumes on the same physical disk as [volume].
     *  Uses the Linux major device number embedded in IDs like "public:179,33". */
    fun getVolumesOnSameDisk(volume: StorageVolumeInfo): List<StorageVolumeInfo> {
        val major = diskMajor(volume.id) ?: return listOf(volume)
        return _volumes.value.filter { diskMajor(it.id) == major }
    }

    /** Attempts a programmatic unmount via hidden StorageManager API.
     *  Tries volume ID first, then path, so it works across API levels. */
    fun tryUnmount(volume: StorageVolumeInfo): Boolean {
        for (arg in listOf(volume.id, volume.path)) {
            try {
                @Suppress("DiscouragedPrivateApi")
                StorageManager::class.java
                    .getDeclaredMethod("unmount", String::class.java)
                    .apply { isAccessible = true }
                    .invoke(storageManager, arg)
                return true
            } catch (_: Exception) { }
        }
        return false
    }

    private fun diskMajor(id: String): String? =
        id.substringAfter(":", "").substringBefore(",", "").takeIf { it.isNotEmpty() }

    private fun buildVolumeInfo(sv: StorageVolume): StorageVolumeInfo? {
        val path = getPath(sv) ?: return null
        val state = getState(sv)
        val mounted = state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
        val (total, free) = if (mounted) getSpaceInfo(path) else 0L to 0L
        return StorageVolumeInfo(
            id = getId(sv) ?: path,
            path = path,
            name = sv.getDescription(context),
            totalBytes = total,
            freeBytes = free,
            state = state,
            isRemovable = sv.isRemovable
        )
    }

    private fun getPath(sv: StorageVolume): String? = try {
        val apiPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) sv.directory?.absolutePath else null
        apiPath ?: run {
            @Suppress("DiscouragedPrivateApi")
            StorageVolume::class.java.getDeclaredMethod("getPath")
                .apply { isAccessible = true }.invoke(sv) as? String
        }
    } catch (_: Exception) { null }

    private fun getState(sv: StorageVolume): String = try {
        @Suppress("DiscouragedPrivateApi")
        (StorageVolume::class.java.getDeclaredMethod("getState")
            .apply { isAccessible = true }.invoke(sv) as? String) ?: Environment.MEDIA_UNKNOWN
    } catch (_: Exception) { Environment.MEDIA_UNKNOWN }

    private fun getId(sv: StorageVolume): String? = try {
        @Suppress("DiscouragedPrivateApi")
        StorageVolume::class.java.getDeclaredMethod("getId")
            .apply { isAccessible = true }.invoke(sv) as? String
    } catch (_: Exception) { null }

    private fun getSpaceInfo(path: String): Pair<Long, Long> = try {
        StatFs(path).let { it.blockCountLong * it.blockSizeLong to it.availableBlocksLong * it.blockSizeLong }
    } catch (_: Exception) { 0L to 0L }
}
