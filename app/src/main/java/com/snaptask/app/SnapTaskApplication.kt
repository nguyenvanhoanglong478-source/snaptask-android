package com.snaptask.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.snaptask.app.data.local.AppDatabase
import com.snaptask.app.data.repository.TaskRepository
import com.snaptask.app.data.repository.TaskRepositoryImpl
import com.snaptask.app.util.FileUtils

class SnapTaskApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var taskRepository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        taskRepository = TaskRepositoryImpl(this, database.taskDao())

        // Low-End RAM Tuning for Coil Image Loader:
        // Set maximum memory cache to 15% of total available RAM
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
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

        // Background cleanup of stale temp files
        FileUtils.clearOldTempFiles(this)
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
