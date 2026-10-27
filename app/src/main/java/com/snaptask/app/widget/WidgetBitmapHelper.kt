package com.snaptask.app.widget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
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

    /**
     * Đồng bộ ảnh Before/After lên Widget màn hình chính bằng Canvas:
     * - Cắt ảnh chính theo tỉ lệ 1:1.
     * - Nếu Task hoàn thành và có cả Before & After: vẽ PiP thu nhỏ đè lên góc phải.
     * - Trả về duy nhất 1 Bitmap tổng hợp, bảo đảm nhẹ và chống lỗi RemoteViews Binder IPC.
     */
    fun createWidgetCompositeBitmap(
        mainImageUri: String?,
        miniPipImageUri: String? = null,
        isCompleted: Boolean = false,
        outputSize: Int = 420
    ): Bitmap? {
        val mainSrc = loadOptimizedBitmap(mainImageUri, outputSize) ?: return null

        return try {
            val result = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.RGB_565)
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

            // 1. Draw Main Image (Center Crop onto outputSize x outputSize)
            val srcRect = calculateCenterCropRect(mainSrc.width, mainSrc.height, outputSize, outputSize)
            val dstRect = Rect(0, 0, outputSize, outputSize)
            canvas.drawBitmap(mainSrc, srcRect, dstRect, paint)

            // 2. If Completed & has PiP: draw mini Before thumbnail at Top-Right
            if (isCompleted && !miniPipImageUri.isNullOrBlank()) {
                val pipSrc = loadOptimizedBitmap(miniPipImageUri, 150)
                if (pipSrc != null) {
                    val pipSize = (outputSize * 0.28f).toInt()
                    val margin = (outputSize * 0.04f)
                    val left = outputSize - margin - pipSize
                    val top = margin
                    val right = left + pipSize
                    val bottom = top + pipSize
                    val pipRectF = RectF(left, top, right, bottom)
                    val cornerRadius = 24f

                    // Draw shadow / dark background
                    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(pipRectF, cornerRadius, cornerRadius, shadowPaint)

                    // Clip and draw PiP Image
                    val clipPath = Path().apply {
                        addRoundRect(pipRectF, cornerRadius, cornerRadius, Path.Direction.CW)
                    }
                    canvas.save()
                    canvas.clipPath(clipPath)

                    val pipSrcRect = calculateCenterCropRect(pipSrc.width, pipSrc.height, pipSize, pipSize)
                    val pipDstRect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
                    canvas.drawBitmap(pipSrc, pipSrcRect, pipDstRect, paint)
                    canvas.restore()

                    // Draw White Border
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 5f
                    }
                    canvas.drawRoundRect(pipRectF, cornerRadius, cornerRadius, borderPaint)
                }
            }

            result
        } catch (_: Throwable) {
            mainSrc
        }
    }

    private fun calculateCenterCropRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
        val srcAspect = srcW.toFloat() / srcH.toFloat()
        val dstAspect = dstW.toFloat() / dstH.toFloat()

        return if (srcAspect > dstAspect) {
            val cropW = (srcH * dstAspect).toInt()
            val left = (srcW - cropW) / 2
            Rect(left, 0, left + cropW, srcH)
        } else {
            val cropH = (srcW / dstAspect).toInt()
            val top = (srcH - cropH) / 2
            Rect(0, top, srcW, top + cropH)
        }
    }
}
