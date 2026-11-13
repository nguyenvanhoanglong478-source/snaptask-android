package com.snaptask.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCompressorTest {

    @Test
    fun `calculateInSampleSize returns 1 for non-positive inputs`() {
        assertEquals(1, ImageCompressor.calculateInSampleSize(0, 2000, 1280, 1280))
        assertEquals(1, ImageCompressor.calculateInSampleSize(2000, 0, 1280, 1280))
        assertEquals(1, ImageCompressor.calculateInSampleSize(2000, 2000, 0, 1280))
        assertEquals(1, ImageCompressor.calculateInSampleSize(2000, 2000, 1280, 0))
        assertEquals(1, ImageCompressor.calculateInSampleSize(-100, 2000, 1280, 1280))
        assertEquals(1, ImageCompressor.calculateInSampleSize(2000, 2000, -1, 1280))
    }

    @Test
    fun `calculateInSampleSize returns 1 when source fits target or half edge is below target`() {
        assertEquals(1, ImageCompressor.calculateInSampleSize(800, 600, 1280, 1280))
        assertEquals(1, ImageCompressor.calculateInSampleSize(1280, 1280, 1280, 1280))
        assertEquals(1, ImageCompressor.calculateInSampleSize(1280, 720, 1280, 1280))
        assertEquals(1, ImageCompressor.calculateInSampleSize(1920, 1080, 1280, 1280))
        assertEquals(1, ImageCompressor.calculateInSampleSize(2560, 1280, 1280, 1280))
        assertEquals(1, ImageCompressor.calculateInSampleSize(8000, 1000, 1280, 1280))
    }

    @Test
    fun `calculateInSampleSize scales by powers of 2 keeping sampled dimensions at or above target`() {
        assertEquals(2, ImageCompressor.calculateInSampleSize(2560, 2560, 1280, 1280))
        assertEquals(2, ImageCompressor.calculateInSampleSize(4032, 3024, 1280, 1280))
        assertEquals(2, ImageCompressor.calculateInSampleSize(3024, 4032, 1280, 1280))
        assertEquals(4, ImageCompressor.calculateInSampleSize(8000, 6000, 1280, 1280))
        assertEquals(4, ImageCompressor.calculateInSampleSize(4000, 3000, 500, 500))

        val sample = ImageCompressor.calculateInSampleSize(6000, 4500, 1280, 1280)
        assertTrue((sample and (sample - 1)) == 0)
        assertTrue(6000 / sample >= 1280)
        assertTrue(4500 / sample >= 1280)
    }
}
