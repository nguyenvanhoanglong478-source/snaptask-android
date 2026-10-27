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
import java.io.InputStream
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
     * Generates a 720p/1080p WebP main image and a lightweight thumbnail for the Home Widget.
     */
    suspend fun compressPhoto(context: Context, rawFile: File): CompressedResult = withContext(Dispatchers.IO) {
        val snapsDir = FileUtils.getSnapsDirectory(context)
        val thumbsDir = FileUtils.getThumbnailsDirectory(context)

        val timestamp = System.currentTimeMillis()
        val targetFullFile = File(snapsDir, "snap_$timestamp.webp")
        val targetThumbFile = File(thumbsDir, "thumb_$timestamp.webp")

        // 1. Read EXIF orientation
        val orientation = readExifOrientation(rawFile.absolutePath)

        // 2. Decode full image with inSampleSize to save RAM
        val sampledBitmap = decodeSampledBitmap(rawFile.absolutePath, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
        val rotatedBitmap = rotateBitmapIfRequired(sampledBitmap, orientation)

        // 3. Compress full image to WebP
        saveBitmapAsWebp(rotatedBitmap, targetFullFile, WEBP_QUALITY)

        // 4. Create miniature thumbnail for Widget & Grid view
        val thumbBitmap = Bitmap.createScaledBitmap(
            rotatedBitmap,
            THUMBNAIL_DIMENSION,
            (THUMBNAIL_DIMENSION * rotatedBitmap.height) / rotatedBitmap.width,
            true
        )
        saveBitmapAsWebp(thumbBitmap, targetThumbFile, 70)

        // Clean up Bitmaps to free native memory immediately
        if (rotatedBitmap != sampledBitmap) {
            sampledBitmap.recycle()
        }
        rotatedBitmap.recycle()
        thumbBitmap.recycle()

        // Delete raw camera file
        rawFile.delete()

        CompressedResult(
            fullImagePath = targetFullFile.absolutePath,
            thumbnailPath = targetThumbFile.absolutePath
        )
    }

    private fun decodeSampledBitmap(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        // Calculate inSampleSize (power of 2)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565 // 16-bit color consumes 50% less RAM than ARGB_8888

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
