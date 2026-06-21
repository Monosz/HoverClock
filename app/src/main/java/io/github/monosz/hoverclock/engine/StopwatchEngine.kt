package io.github.monosz.hoverclock.engine

import android.os.SystemClock
import io.github.monosz.hoverclock.model.TimeMode
import io.github.monosz.hoverclock.model.TimeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class StopwatchEngine(
    private val scope: CoroutineScope,
    private var showMilliseconds: Boolean = true,
    autoStart: Boolean = false,
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
) : TimeProvider {
    private val _state =
        MutableStateFlow(
            TimeState(
                mode = TimeMode.Stopwatch,
                formattedText = formatElapsed(0L),
                isRunning = false,
                elapsedMillis = 0L,
            ),
        )
    override val state: StateFlow<TimeState> = _state.asStateFlow()

    private var elapsedMillis = 0L
    private var startRealtime = 0L
    private var tickerJob: Job? = null

    init {
        if (autoStart) start()
    }

    fun updateSettings(showMilliseconds: Boolean) {
        this.showMilliseconds = showMilliseconds
        _state.update { it.copy(formattedText = formatElapsed(elapsedMillis)) }
    }

    override fun start() {
        if (_state.value.isRunning) return
        startRealtime = elapsedRealtime() - elapsedMillis
        _state.update { it.copy(isRunning = true) }
        startTicker()
    }

    override fun pause() {
        if (!_state.value.isRunning) return
        elapsedMillis = elapsedRealtime() - startRealtime
        tickerJob?.cancel()
        tickerJob = null
        _state.update {
            it.copy(
                isRunning = false,
                elapsedMillis = elapsedMillis,
                formattedText = formatElapsed(elapsedMillis),
            )
        }
    }

    override fun reset() {
        tickerJob?.cancel()
        tickerJob = null
        elapsedMillis = 0L
        startRealtime = 0L
        _state.value =
            TimeState(
                mode = TimeMode.Stopwatch,
                formattedText = formatElapsed(0L),
                isRunning = false,
                elapsedMillis = 0L,
            )
    }

    override fun release() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun toggle() {
        if (_state.value.isRunning) pause() else start()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob =
            scope.launch {
                val interval = if (showMilliseconds) 50L else 1_000L
                while (isActive) {
                    val current = elapsedRealtime() - startRealtime
                    _state.update {
                        it.copy(
                            elapsedMillis = current,
                            formattedText = formatElapsed(current),
                        )
                    }
                    delay(interval)
                }
            }
    }

    private fun formatElapsed(millis: Long): String {
        val totalSeconds = millis / 1_000
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return if (showMilliseconds) {
            val ms = (millis % 1_000) / 10
            if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, ms)
            } else {
                String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, ms)
            }
        } else {
            if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
    }
}
