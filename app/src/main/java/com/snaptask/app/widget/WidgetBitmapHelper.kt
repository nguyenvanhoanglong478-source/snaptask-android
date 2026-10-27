package com.snaptask.app.widget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object WidgetBitmapHelper {

    fun loadOptimizedBitmap(path: String?, maxSize: Int = 450): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null

        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            var sample = 1
            while (options.outWidth / sample > maxSize || options.outHeight / sample > maxSize) {
                sample *= 2
            }
            options.inSampleSize = sample
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            BitmapFactory.decodeFile(path, options)
        } catch (_: Throwable) {
            null
        }
    }
}
