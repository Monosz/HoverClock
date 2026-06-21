package io.github.monosz.hoverclock.data

import io.github.monosz.hoverclock.model.OverlayInstance
import io.github.monosz.hoverclock.model.TimeMode

object InstanceNameGenerator {
    fun nextName(
        mode: TimeMode,
        existingInstances: List<OverlayInstance>,
    ): String {
        val prefix = modeLabel(mode)
        val usedNumbers =
            existingInstances
                .filter { it.mode == mode }
                .mapNotNull { instance -> trailingNumber(prefix, instance.name) }
        val next = (usedNumbers.maxOrNull() ?: 0) + 1
        return "$prefix $next"
    }

    private fun modeLabel(mode: TimeMode): String =
        when (mode) {
            TimeMode.Clock -> "Clock"
            TimeMode.Stopwatch -> "Stopwatch"
            TimeMode.Timer -> "Timer"
        }

    private fun trailingNumber(
        prefix: String,
        name: String,
    ): Int? {
        val pattern = Regex("^${Regex.escape(prefix)} (\\d+)$")
        return pattern.matchEntire(name)?.groupValues?.get(1)?.toIntOrNull()
    }
}
