package io.github.monosz.hoverclock.model

data class TimeState(
    val mode: TimeMode,
    val formattedText: String,
    val isRunning: Boolean,
    val elapsedMillis: Long? = null,
    val remainingMillis: Long? = null,
)
