package io.github.monosz.hoverclock.engine

import io.github.monosz.hoverclock.model.TimeState
import kotlinx.coroutines.flow.StateFlow

interface TimeProvider {
    val state: StateFlow<TimeState>

    fun start()

    fun pause()

    fun reset()

    fun release()
}
