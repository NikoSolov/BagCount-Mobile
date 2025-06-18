package com.nikosolov.bagscount

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

object ScanUtils {
    /**
     * Проверяет, превышает ли длительность видео максимальную длину (в секундах).
     */
    fun isVideoTooLong(durationMs: Long, maxLengthSec: Int): Boolean {
        return (durationMs / 1000) > maxLengthSec
    }

    /**
     * Генерирует корректное имя файла на основе raw-пути и mime-типа.
     * Если raw содержит расширение, возвращает raw, иначе добавляет расширение из mime.
     * Для неподдерживаемых image MIME-типов (не начинающихся с "image/") по умолчанию добавляет ".jpg",
     * для видео MIME-типов — ".mp4".
     */
    fun extractFilename(rawSegment: String?, mime: String, isVideo: Boolean): String {
        val raw = rawSegment
            ?.substringAfterLast('/')
            ?.substringBefore('?')
            ?: "upload"
        if ("." in raw) return raw

        val ext = when {
            isVideo -> "mp4"
            mime.startsWith("image/") -> mime.substringAfterLast('/').takeIf { it.isNotBlank() }
            else -> null
        } ?: if (isVideo) "mp4" else "jpg"

        return "${raw}.${ext}"
    }
}





class ScanUtilsTest {
    @Test
    fun `isVideoTooLong returns false when equal to max`() {
        assertFalse(ScanUtils.isVideoTooLong(durationMs = 60000, maxLengthSec = 60))
    }

    @Test
    fun `isVideoTooLong returns true when one second over`() {
        assertTrue(ScanUtils.isVideoTooLong(durationMs = 61000, maxLengthSec = 60))
    }

    @Test
    fun `isVideoTooLong returns false when just under max`() {
        assertFalse(ScanUtils.isVideoTooLong(durationMs = 59999, maxLengthSec = 60))
    }

    @Test
    fun `isVideoTooLong handles zero and negative duration`() {
        assertFalse(ScanUtils.isVideoTooLong(durationMs = 0, maxLengthSec = 60))
        assertFalse(ScanUtils.isVideoTooLong(durationMs = -1000, maxLengthSec = 60))
    }

    @Test
    fun `extractFilename uses raw with extension`() {
        val raw = "file.name.png?token=123"
        val filename = ScanUtils.extractFilename(rawSegment = raw, mime = "image/png", isVideo = false)
        assertEquals("file.name.png", filename)
    }

    @Test
    fun `extractFilename adds extension from mime for image`() {
        val filename = ScanUtils.extractFilename(rawSegment = "file", mime = "image/jpeg", isVideo = false)
        assertEquals("file.jpeg", filename)
    }

    @Test
    fun `extractFilename adds default mp4 for video when mime missing`() {
        val filename = ScanUtils.extractFilename(rawSegment = "videoFile", mime = "", isVideo = true)
        assertEquals("videoFile.mp4", filename)
    }

    @Test
    fun `extractFilename uses upload when raw null`() {
        val filename = ScanUtils.extractFilename(rawSegment = null, mime = "image/png", isVideo = false)
        assertEquals("upload.png", filename)
    }

    @Test
    fun `extractFilename falls back to jpg for image when mime invalid`() {
        val filename = ScanUtils.extractFilename(rawSegment = "pic", mime = "application/octet-stream", isVideo = false)
        assertEquals("pic.jpg", filename)
    }
}


