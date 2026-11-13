package com.snaptask.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ImageCompressor {

    private const val MAX_IMAGE_DIMENSION = 1280
    private const val WEBP_QUALITY = 78

    /**
     * Compresses and optimizes a raw photo file for low-end Android devices.
     * Guarantees max edge <= 1280px in ARGB_8888.
     */
    suspend fun compressPhoto(context: Context, rawFile: File): String = withContext(Dispatchers.IO) {
        try {
            val orientation = readExifOrientation(rawFile.absolutePath)
            processAndSaveBitmap(
                context = context,
                orientation = orientation,
                errorMessage = "Không thể giải mã file ảnh từ máy ảnh: ${rawFile.name}"
            ) {
                decodeSampledBitmap(rawFile.absolutePath, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
            }
        } finally {
            // Raw camera file is always cleaned up even on corrupt/null decode
            rawFile.delete()
        }
    }

    /**
     * Compresses and optimizes a content/gallery URI without copying the raw file to cache.
     * Guarantees max edge <= 1280px in ARGB_8888 and safe stream/bitmap cleanup.
     */
    suspend fun compressUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val orientation = readExifOrientation(context, uri)
        processAndSaveBitmap(
            context = context,
            orientation = orientation,
            errorMessage = "Không thể giải mã ảnh đã chọn"
        ) {
            decodeSampledBitmap(context, uri, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
        }
    }

    private fun processAndSaveBitmap(
        context: Context,
        orientation: Int,
        errorMessage: String,
        decodeSampled: () -> Bitmap?
    ): String {
        val snapsDir = FileUtils.getSnapsDirectory(context)
        val timestamp = System.currentTimeMillis()
        var targetFullFile = File(snapsDir, "snap_$timestamp.webp")
        if (targetFullFile.exists()) {
            targetFullFile = File(snapsDir, "snap_${timestamp}_${System.nanoTime()}.webp")
        }

        var sampledBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null
        var finalFullBitmap: Bitmap? = null

        return try {
            // 1. Decode sampled image to save RAM (ARGB_8888)
            sampledBitmap = decodeSampled() ?: throw IllegalArgumentException(errorMessage)

            // 2. Clamp long edge to <= 1280px before rotation to minimize RAM peak
            val maxEdge = max(sampledBitmap.width, sampledBitmap.height)
            scaledBitmap = if (maxEdge > MAX_IMAGE_DIMENSION) {
                val scale = MAX_IMAGE_DIMENSION.toFloat() / maxEdge
                val targetWidth = (sampledBitmap.width * scale).toInt().coerceAtLeast(1)
                val targetHeight = (sampledBitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true)
            } else {
                sampledBitmap
            }

            // 3. Rotate/flip clamped image according to EXIF
            finalFullBitmap = rotateBitmapIfRequired(scaledBitmap, orientation)

            // 4. Compress full image to WebP
            saveBitmapAsWebp(finalFullBitmap, targetFullFile, WEBP_QUALITY)

            targetFullFile.absolutePath
        } catch (t: Throwable) {
            targetFullFile.delete()
            throw t
        } finally {
            // Clean up Bitmaps to free native memory immediately without double-recycling
            val bitmapsToRecycle = LinkedHashSet<Bitmap?>()
            bitmapsToRecycle.add(sampledBitmap)
            bitmapsToRecycle.add(scaledBitmap)
            bitmapsToRecycle.add(finalFullBitmap)
            for (bmp in bitmapsToRecycle) {
                if (bmp != null && !bmp.isRecycled) {
                    bmp.recycle()
                }
            }
        }
    }

    /**
     * Pure Kotlin power-of-2 downsampling calculation, unit-testable on JVM.
     */
    fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        if (srcWidth <= 0 || srcHeight <= 0 || reqWidth <= 0 || reqHeight <= 0) {
            return 1
        }
        var inSampleSize = 1
        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
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

    private fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return null
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        return calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight)
    }

    private fun readExifOrientation(path: String): Int {
        return try {
            val exif = ExifInterface(path)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun saveBitmapAsWebp(bitmap: Bitmap, targetFile: File, quality: Int) {
        val success = FileOutputStream(targetFile).use { out ->
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            val compressed = bitmap.compress(format, quality, out)
            out.flush()
            compressed
        }
        if (!success) {
            targetFile.delete()
            throw IllegalStateException("Không thể lưu ảnh nén WebP: ${targetFile.name}")
        }
    }
}
