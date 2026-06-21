package io.github.monosz.hoverclock.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class InstanceSummaryFormatterTest {
    @Test
    fun fontSizeLabel_small() {
        assertEquals(FontSizeLabel.Small, InstanceSummaryFormatter.fontSizeLabel(16f))
        assertEquals(FontSizeLabel.Small, InstanceSummaryFormatter.fontSizeLabel(24f))
    }

    @Test
    fun fontSizeLabel_medium() {
        assertEquals(FontSizeLabel.Medium, InstanceSummaryFormatter.fontSizeLabel(25f))
        assertEquals(FontSizeLabel.Medium, InstanceSummaryFormatter.fontSizeLabel(32f))
        assertEquals(FontSizeLabel.Medium, InstanceSummaryFormatter.fontSizeLabel(40f))
    }

    @Test
    fun fontSizeLabel_large() {
        assertEquals(FontSizeLabel.Large, InstanceSummaryFormatter.fontSizeLabel(41f))
        assertEquals(FontSizeLabel.Large, InstanceSummaryFormatter.fontSizeLabel(64f))
    }

    @Test
    fun formatDuration_minutesAndSeconds() {
        assertEquals("5:00", InstanceSummaryFormatter.formatDuration(5 * 60 * 1000L))
        assertEquals("10:30", InstanceSummaryFormatter.formatDuration((10 * 60 + 30) * 1000L))
    }

    @Test
    fun formatDuration_withHours() {
        assertEquals("1:05:00", InstanceSummaryFormatter.formatDuration((65 * 60) * 1000L))
    }

    @Test
    fun buildSummary_joinsWithMiddleDot() {
        assertEquals("Medium · Standard", InstanceSummaryFormatter.buildSummary("Medium", "Standard"))
    }
}
