package io.github.monosz.hoverclock.ui.navigation

import io.github.monosz.hoverclock.model.TimeMode

enum class MainTab {
    Clock,
    Stopwatch,
    Timer,
    ;

    val timeMode: TimeMode
        get() =
            when (this) {
                Clock -> TimeMode.Clock
                Stopwatch -> TimeMode.Stopwatch
                Timer -> TimeMode.Timer
            }

    companion object {
        fun fromTimeMode(mode: TimeMode): MainTab =
            when (mode) {
                TimeMode.Clock -> Clock
                TimeMode.Stopwatch -> Stopwatch
                TimeMode.Timer -> Timer
            }

        fun fromName(name: String?): MainTab = entries.firstOrNull { it.name == name } ?: Clock
    }
}
