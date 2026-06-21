package io.github.monosz.hoverclock.engine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Test
    fun countsDownToZero_andStops() =
        runTest(dispatcher) {
            var now = 0L
            var finished = false
            val engine =
                TimerEngine(
                    scope = scope,
                    durationMillis = 3_000L,
                    showMilliseconds = false,
                    onFinished = { finished = true },
                    elapsedRealtime = { now },
                )
            assertEquals("00:03", engine.state.value.formattedText)

            engine.start()
            assertTrue(engine.state.value.isRunning)
            now += 3_100
            advanceTimeBy(100)
            assertTrue(finished)
            assertEquals("00:00", engine.state.value.formattedText)
            assertFalse(engine.state.value.isRunning)
            engine.release()
        }

    @Test
    fun pause_preservesRemainingTime() =
        runTest(dispatcher) {
            var now = 0L
            val engine =
                TimerEngine(
                    scope = scope,
                    durationMillis = 10_000L,
                    showMilliseconds = false,
                    elapsedRealtime = { now },
                )
            engine.start()
            now += 2_100
            advanceTimeBy(100)
            engine.pause()
            val remaining = engine.state.value.remainingMillis ?: 0L
            assertTrue(remaining in 7_000L..8_500L)
            engine.release()
        }

    @Test
    fun reset_restoresFullDuration() =
        runTest(dispatcher) {
            var now = 0L
            val engine =
                TimerEngine(
                    scope = scope,
                    durationMillis = 5_000L,
                    showMilliseconds = false,
                    elapsedRealtime = { now },
                )
            engine.start()
            now += 2_000
            advanceTimeBy(100)
            engine.reset()
            assertEquals("00:05", engine.state.value.formattedText)
            assertFalse(engine.state.value.isRunning)
            engine.release()
        }
}
