package com.snaptask.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ImageCompressor {

    private const val MAX_IMAGE_DIMENSION = 1280
    private const val THUMBNAIL_DIMENSION = 360
    private const val WEBP_QUALITY = 78

    data class CompressedResult(
        val fullImagePath: String,
        val thumbnailPath: String
    )

    /**
     * Compresses and optimizes a raw photo file for low-end Android devices.
     * Guarantees max edge <= 1280px in ARGB_8888 and a 360px thumbnail.
     */
    suspend fun compressPhoto(context: Context, rawFile: File): CompressedResult = withContext(Dispatchers.IO) {
        try {
            val snapsDir = FileUtils.getSnapsDirectory(context)
            val thumbsDir = FileUtils.getThumbnailsDirectory(context)

            val timestamp = System.currentTimeMillis()
            val targetFullFile = File(snapsDir, "snap_$timestamp.webp")
            val targetThumbFile = File(thumbsDir, "thumb_$timestamp.webp")

            // 1. Read EXIF orientation
            val orientation = readExifOrientation(rawFile.absolutePath)

            // 2. Decode full image with inSampleSize to save RAM (ARGB_8888)
            val sampledBitmap = decodeSampledBitmap(rawFile.absolutePath, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                ?: throw IllegalArgumentException("Không thể giải mã file ảnh từ máy ảnh: ${rawFile.name}")

            val rotatedBitmap = rotateBitmapIfRequired(sampledBitmap, orientation)

            // 3. Clamp long edge to exactly 1280px if long edge > 1280 (preserving aspect ratio)
            val maxEdge = max(rotatedBitmap.width, rotatedBitmap.height)
            val finalFullBitmap = if (maxEdge > MAX_IMAGE_DIMENSION) {
                val scale = MAX_IMAGE_DIMENSION.toFloat() / maxEdge
                val targetWidth = (rotatedBitmap.width * scale).toInt()
                val targetHeight = (rotatedBitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(rotatedBitmap, targetWidth, targetHeight, true)
            } else {
                rotatedBitmap
            }

            // 4. Compress clamped full image to WebP
            saveBitmapAsWebp(finalFullBitmap, targetFullFile, WEBP_QUALITY)

            // 5. Create miniature 360px thumbnail for Widget & Grid view
            val thumbBitmap = Bitmap.createScaledBitmap(
                finalFullBitmap,
                THUMBNAIL_DIMENSION,
                (THUMBNAIL_DIMENSION * finalFullBitmap.height) / finalFullBitmap.width,
                true
            )
            saveBitmapAsWebp(thumbBitmap, targetThumbFile, 70)

            // Clean up Bitmaps to free native memory immediately
            if (finalFullBitmap != rotatedBitmap) {
                finalFullBitmap.recycle()
            }
            if (rotatedBitmap != sampledBitmap) {
                rotatedBitmap.recycle()
            }
            sampledBitmap.recycle()
            thumbBitmap.recycle()

            CompressedResult(
                fullImagePath = targetFullFile.absolutePath,
                thumbnailPath = targetThumbFile.absolutePath
            )
        } finally {
            // Raw camera file is always cleaned up even on corrupt/null decode
            rawFile.delete()
        }
    }

    private fun decodeSampledBitmap(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null
        }

        // Calculate inSampleSize (power of 2)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun readExifOrientation(path: String): Int {
        return try {
            val exif = ExifInterface(path)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun saveBitmapAsWebp(bitmap: Bitmap, targetFile: File, quality: Int) {
        FileOutputStream(targetFile).use { out ->
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }
}
