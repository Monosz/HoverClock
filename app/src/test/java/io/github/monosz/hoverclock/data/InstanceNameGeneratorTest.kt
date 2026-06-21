package io.github.monosz.hoverclock.data

import io.github.monosz.hoverclock.model.OverlayInstance
import io.github.monosz.hoverclock.model.TimeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class InstanceNameGeneratorTest {
    @Test
    fun nextName_startsAtOne_whenNoInstances() {
        val name = InstanceNameGenerator.nextName(TimeMode.Clock, emptyList())
        assertEquals("Clock 1", name)
    }

    @Test
    fun nextName_incrementsPerMode() {
        val existing =
            listOf(
                OverlayInstance("1", TimeMode.Stopwatch, "Stopwatch 1"),
                OverlayInstance("2", TimeMode.Clock, "Clock 1"),
                OverlayInstance("3", TimeMode.Stopwatch, "Kitchen"),
            )
        assertEquals("Stopwatch 2", InstanceNameGenerator.nextName(TimeMode.Stopwatch, existing))
        assertEquals("Clock 2", InstanceNameGenerator.nextName(TimeMode.Clock, existing))
        assertEquals("Timer 1", InstanceNameGenerator.nextName(TimeMode.Timer, existing))
    }

    @Test
    fun nextName_skipsGapsInNumbering() {
        val existing =
            listOf(
                OverlayInstance("1", TimeMode.Timer, "Timer 1"),
                OverlayInstance("2", TimeMode.Timer, "Timer 3"),
            )
        assertEquals("Timer 4", InstanceNameGenerator.nextName(TimeMode.Timer, existing))
    }
}
