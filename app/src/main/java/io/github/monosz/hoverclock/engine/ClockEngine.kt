package io.github.monosz.hoverclock.engine

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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClockEngine(
    private val scope: CoroutineScope,
    private var use24Hour: Boolean = false,
    private var showSeconds: Boolean = true,
) : TimeProvider {
    private val _state =
        MutableStateFlow(
            TimeState(
                mode = TimeMode.Clock,
                formattedText = formatNow(),
                isRunning = true,
            ),
        )
    override val state: StateFlow<TimeState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    init {
        start()
    }

    fun updateSettings(
        use24Hour: Boolean,
        showSeconds: Boolean,
    ) {
        this.use24Hour = use24Hour
        this.showSeconds = showSeconds
        _state.update { it.copy(formattedText = formatNow()) }
        restartTicker()
    }

    override fun start() {
        if (tickerJob?.isActive == true) return
        _state.update { it.copy(isRunning = true, formattedText = formatNow()) }
        restartTicker()
    }

    override fun pause() = Unit

    override fun reset() = Unit

    override fun release() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun restartTicker() {
        tickerJob?.cancel()
        val interval = if (showSeconds) 1_000L else 60_000L
        tickerJob =
            scope.launch {
                while (isActive) {
                    _state.update { it.copy(formattedText = formatNow()) }
                    delay(interval)
                }
            }
    }

    private fun formatNow(): String {
        val pattern =
            buildString {
                if (use24Hour) append("HH") else append("hh")
                append(":mm")
                if (showSeconds) append(":ss")
                if (!use24Hour) append(" a")
            }
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return LocalTime.now().format(formatter)
    }
}
