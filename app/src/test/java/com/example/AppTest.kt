package com.example

import com.example.data.lyrics.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTest {
    @Test
    fun testLrcParser() {
        val lrc = """
            [00:02.50]Hello world
            [00:05.10]Second line
        """.trimIndent()
        val lines = LrcParser.parse(lrc)
        assertEquals(2, lines.size)
        assertEquals(2500L, lines[0].timestampMs)
        assertEquals("Hello world", lines[0].text)
    }
}
