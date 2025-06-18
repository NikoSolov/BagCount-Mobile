package com.nikosolov.bagscount

import org.junit.Assert.assertEquals
import org.junit.Test

object SettingsHelper {
    private const val DEFAULT_IP = "127.0.0.1"
    private const val DEFAULT_MAX_LENGTH = 60

    fun formatIp(ip: String?): String {
        return if (ip.isNullOrBlank() || ip == DEFAULT_IP) {
            "Не задано"
        } else {
            ip
        }
    }

    fun formatMaxLength(length: Int?): String {
        val len = length ?: DEFAULT_MAX_LENGTH
        return "${len} сек."
    }
}

class SettingsHelperTest {
    @Test
    fun `formatIp returns not set when null`() {
        assertEquals("Не задано", SettingsHelper.formatIp(null))
    }

    @Test
    fun `formatIp returns not set when blank`() {
        assertEquals("Не задано", SettingsHelper.formatIp(""))
    }

    @Test
    fun `formatIp returns not set when default IP`() {
        assertEquals("Не задано", SettingsHelper.formatIp("127.0.0.1"))
    }

    @Test
    fun `formatIp returns custom IP`() {
        assertEquals("192.168.0.5", SettingsHelper.formatIp("192.168.0.5"))
    }

    @Test
    fun `formatMaxLength returns default when null`() {
        assertEquals("60 сек.", SettingsHelper.formatMaxLength(null))
    }

    @Test
    fun `formatMaxLength returns default when zero`() {
        assertEquals("0 сек.", SettingsHelper.formatMaxLength(0))
    }

    @Test
    fun `formatMaxLength returns custom length`() {
        assertEquals("120 сек.", SettingsHelper.formatMaxLength(120))
    }
}