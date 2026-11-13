package com.snaptask.app

import android.app.ActivityManager
import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.data.preferences.FolderPreferencesRepository
import com.snaptask.app.data.repository.TaskRepository
import com.snaptask.app.data.repository.TaskRepositoryImpl
import com.snaptask.app.util.FileUtils
import com.snaptask.app.widget.SnapTaskWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SnapTaskApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set

    lateinit var taskRepository: TaskRepository
        private set

    lateinit var folderPreferences: FolderPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        folderPreferences = FolderPreferencesRepository(this)
        taskRepository = TaskRepositoryImpl(
            context = this,
            taskDao = database.taskDao(),
            onTasksChanged = { SnapTaskWidgetUpdater.requestUpdate(this) },
            onTaskDeleted = { id -> folderPreferences.removeTaskRepeat(id) }
        )

        val isLowRamDevice = (getSystemService(ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true

        // Low-End RAM Tuning for Coil Image Loader:
        // Set maximum memory cache to 15% of total available RAM and prefer RGB_565
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // Cap disk cache at 50MB
                    .build()
            }
            .allowRgb565(true)
            .crossfade(!isLowRamDevice)
            .build()
        Coil.setImageLoader(imageLoader)

        // Background cleanup of leftover capture temp files and stale cache on IO thread
        applicationScope.launch {
            FileUtils.clearTempCache(this@SnapTaskApplication)
            FileUtils.clearOldTempFiles(this@SnapTaskApplication)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Aggressively free image cache when OS signals low memory
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            Coil.imageLoader(this).memoryCache?.clear()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Coil.imageLoader(this).memoryCache?.clear()
    }
}
