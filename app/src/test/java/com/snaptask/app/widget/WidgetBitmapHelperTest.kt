package com.snaptask.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBitmapHelperTest {

    @Test
    fun `calculateSampleSize returns 1 for non-positive inputs`() {
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(0, 500, 450))
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(500, 0, 450))
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(500, 500, 0))
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(-100, 500, 450))
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(500, -100, 450))
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(500, 500, -10))
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(0, 0, 0))
    }

    @Test
    fun `calculateSampleSize returns 1 when dimensions fit within or equal maxSize`() {
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(200, 300, 450))
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(450, 450, 450))
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(450, 300, 450))
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(300, 450, 450))
        assertEquals(1, WidgetBitmapHelper.calculateSampleSize(1, 1, 1))
    }

    @Test
    fun `calculateSampleSize scales by powers of 2 for oversized portrait and landscape images`() {
        assertEquals(2, WidgetBitmapHelper.calculateSampleSize(451, 450, 450))
        assertEquals(4, WidgetBitmapHelper.calculateSampleSize(900, 1800, 450))
        assertEquals(8, WidgetBitmapHelper.calculateSampleSize(1920, 1080, 450))
        assertEquals(16, WidgetBitmapHelper.calculateSampleSize(4000, 3000, 450))
        assertEquals(16, WidgetBitmapHelper.calculateSampleSize(3000, 4000, 450))
        assertEquals(8, WidgetBitmapHelper.calculateSampleSize(100, 3600, 450))

        val maxSize = 420
        val sample = WidgetBitmapHelper.calculateSampleSize(3840, 2160, maxSize)
        assertTrue(sample > 1 && (sample and (sample - 1)) == 0)
        assertTrue(3840 / sample <= maxSize)
        assertTrue(2160 / sample <= maxSize)
    }
}
