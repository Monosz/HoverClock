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

class TimerEngine(
    private val scope: CoroutineScope,
    private var durationMillis: Long,
    private var showMilliseconds: Boolean = false,
    private val onFinished: () -> Unit = {},
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
) : TimeProvider {
    private val _state =
        MutableStateFlow(
            TimeState(
                mode = TimeMode.Timer,
                formattedText = formatRemaining(durationMillis),
                isRunning = false,
                remainingMillis = durationMillis,
            ),
        )
    override val state: StateFlow<TimeState> = _state.asStateFlow()

    private var remainingMillis = durationMillis
    private var endRealtime = 0L
    private var tickerJob: Job? = null
    private var finished = false

    fun updateSettings(
        durationMillis: Long,
        showMilliseconds: Boolean,
    ) {
        if (!_state.value.isRunning) {
            this.durationMillis = durationMillis
            this.showMilliseconds = showMilliseconds
            remainingMillis = durationMillis
            finished = false
            _state.value =
                TimeState(
                    mode = TimeMode.Timer,
                    formattedText = formatRemaining(remainingMillis),
                    isRunning = false,
                    remainingMillis = remainingMillis,
                )
        } else {
            this.showMilliseconds = showMilliseconds
        }
    }

    override fun start() {
        if (_state.value.isRunning || finished) return
        endRealtime = elapsedRealtime() + remainingMillis
        _state.update { it.copy(isRunning = true) }
        startTicker()
    }

    override fun pause() {
        if (!_state.value.isRunning) return
        remainingMillis = (endRealtime - elapsedRealtime()).coerceAtLeast(0L)
        tickerJob?.cancel()
        tickerJob = null
        _state.update {
            it.copy(
                isRunning = false,
                remainingMillis = remainingMillis,
                formattedText = formatRemaining(remainingMillis),
            )
        }
    }

    override fun reset() {
        tickerJob?.cancel()
        tickerJob = null
        finished = false
        remainingMillis = durationMillis
        _state.value =
            TimeState(
                mode = TimeMode.Timer,
                formattedText = formatRemaining(remainingMillis),
                isRunning = false,
                remainingMillis = remainingMillis,
            )
    }

    override fun release() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun toggle() {
        if (finished) {
            reset()
            start()
        } else if (_state.value.isRunning) {
            pause()
        } else {
            start()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob =
            scope.launch {
                val interval = if (showMilliseconds) 50L else 1_000L
                while (isActive) {
                    val left = (endRealtime - elapsedRealtime()).coerceAtLeast(0L)
                    remainingMillis = left
                    if (left <= 0L) {
                        finish()
                        break
                    }
                    _state.update {
                        it.copy(
                            remainingMillis = left,
                            formattedText = formatRemaining(left),
                        )
                    }
                    delay(interval)
                }
            }
    }

    private fun finish() {
        tickerJob?.cancel()
        tickerJob = null
        finished = true
        remainingMillis = 0L
        _state.value =
            TimeState(
                mode = TimeMode.Timer,
                formattedText = formatRemaining(0L),
                isRunning = false,
                remainingMillis = 0L,
            )
        onFinished()
    }

    private fun formatRemaining(millis: Long): String {
        val totalSeconds = (millis + 999) / 1_000
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
