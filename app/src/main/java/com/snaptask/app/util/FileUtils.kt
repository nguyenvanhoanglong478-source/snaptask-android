package com.snaptask.app.util

import android.content.Context
import java.io.File

object FileUtils {

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
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
            // Ignore failure on cleanup
        }
    }

    fun clearOldTempFiles(context: Context) {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (System.currentTimeMillis() - file.lastModified() > 86400000) { // 24 hours
                    file.delete()
                }
            }
            val legacyThumbs = File(context.filesDir, "thumbnails")
            if (legacyThumbs.exists()) {
                legacyThumbs.deleteRecursively()
            }
        } catch (_: Exception) {
        }
    }
}
