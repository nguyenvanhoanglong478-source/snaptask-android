package com.snaptask.app.util

import android.content.Context
import java.io.File
import java.util.Locale

object FileUtils {

    private const val ONE_DAY_MILLIS = 86_400_000L
    private const val COIL_CACHE_DIR = "image_cache"
    private val CAMERA_TEMP_PREFIXES = listOf("raw_", "temp_", "snap_", "proof_")

    fun getSnapsDirectory(context: Context): File {
        val dir = File(context.filesDir, "snaps")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun deleteFile(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val normalizedPath = if (path.startsWith("file://")) {
                path.removePrefix("file://")
            } else {
                path
            }
            val file = File(normalizedPath)
            if (file.exists() && file.isFile) {
                file.delete()
            }
        } catch (_: Exception) {
            // Ignore failure on cleanup
        }
    }

    fun deleteTaskImages(vararg paths: String?) {
        paths.asSequence()
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { deleteFile(it) }
    }

    fun clearTempCache(context: Context) {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && isCameraTempFile(file.name)) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Ignore failure on cleanup
        }
    }

    fun clearOldTempFiles(context: Context) {
        try {
            val now = System.currentTimeMillis()
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name == COIL_CACHE_DIR) return@forEach
                if (file.isFile && now - file.lastModified() > ONE_DAY_MILLIS) {
                    file.delete()
                } else if (file.isDirectory) {
                    file.listFiles()?.forEach { child ->
                        if (child.isFile && now - child.lastModified() > ONE_DAY_MILLIS) {
                            child.delete()
                        }
                    }
                }
            }
            val legacyThumbs = File(context.filesDir, "thumbnails")
            if (legacyThumbs.exists()) {
                legacyThumbs.deleteRecursively()
            }
        } catch (_: Exception) {
        }
    }

    fun getStorageUsedBytes(context: Context): Long {
        return try {
            val snapsDir = getSnapsDirectory(context)
            snapsDir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        } catch (_: Exception) {
            0L
        }
    }

    fun getStorageUsedFormatted(context: Context): String {
        return formatStorageBytes(getStorageUsedBytes(context))
    }

    fun formatStorageBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = 1024L
        val mb = kb * 1024L
        return when {
            bytes < kb -> "$bytes B"
            bytes < mb -> "${bytes / kb} KB"
            else -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / mb.toDouble())
        }
    }

    private fun isCameraTempFile(fileName: String): Boolean {
        return CAMERA_TEMP_PREFIXES.any { prefix -> fileName.startsWith(prefix) }
    }
}
