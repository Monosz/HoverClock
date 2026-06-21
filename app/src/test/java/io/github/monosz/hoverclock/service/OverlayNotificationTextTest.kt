package io.github.monosz.hoverclock.service

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayNotificationTextTest {
    @Test
    fun summaryLine_joinsNames() {
        val result = OverlayNotificationText.summaryLine(listOf("Clock 1", "Stopwatch 2"))
        assertEquals("Clock 1, Stopwatch 2", result)
    }

    @Test
    fun summaryLine_emptyList() {
        assertEquals("", OverlayNotificationText.summaryLine(emptyList()))
    }

    @Test
    fun summaryLine_truncatesLongText() {
        val names = List(10) { "Very Long Instance Name $it" }
        val result = OverlayNotificationText.summaryLine(names, maxLength = 40)
        assertEquals(40, result.length)
        assertEquals("…", result.takeLast(1))
    }
}

class OverlaySpawnOffsetTest {
    @Test
    fun offsetPx_staggersBySessionIndex() {
        val density = 2f
        val first = OverlaySpawnOffset.offsetPx(0, density)
        val second = OverlaySpawnOffset.offsetPx(1, density)

        assertEquals(0, first.first)
        assertEquals(400, first.second)
        assertEquals(96, second.first)
        assertEquals(496, second.second)
    }
}
