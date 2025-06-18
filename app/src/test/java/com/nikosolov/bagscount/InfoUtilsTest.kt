package com.nikosolov.bagscount

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

object InfoUtils {
    /**
     * Определяет, является ли файл видео на основании расширения имени,
     * обрезая query-параметры.
     */
    fun isVideoFile(filename: String): Boolean {
        // Убираем query-параметры и фрагменты
        val cleanName = filename.substringBefore('?').substringBefore('#').trim().lowercase()
        return cleanName.endsWith(".mp4")
    }/**
     * Форматирует текст статистики для заданного ярлыка и количества.
     */
    fun formatStat(label: String, count: Int): String {
        return "$label: $count"
    }
}

class InfoUtilsTest {
    @Test
    fun `isVideoFile detects mp4`() {
        assertTrue(InfoUtils.isVideoFile("movie.mp4"))
    }

    @Test
    fun `isVideoFile detects case insensitive`() {
        assertTrue(InfoUtils.isVideoFile("CLIP.MP4"))
    }

    @Test
    fun `isVideoFile returns false for image`() {
        assertFalse(InfoUtils.isVideoFile("photo.jpg"))
    }

    @Test
    fun `isVideoFile returns false for filenames containing mp4 but not as extension`() {
        assertFalse(InfoUtils.isVideoFile("my_mp4_backup.txt"))
    }

    @Test
    fun `isVideoFile handles filenames with query params`() {
        assertTrue(InfoUtils.isVideoFile("video.mp4?token=xyz"))
    }

    @Test
    fun `formatStat combines label and count`() {
        assertEquals("Bags: 5", InfoUtils.formatStat("Bags", 5))
    }

    @Test
    fun `formatStat handles zero and negative counts`() {
        assertEquals("Items: 0", InfoUtils.formatStat("Items", 0))
        assertEquals("Errors: -1", InfoUtils.formatStat("Errors", -1))
    }
}