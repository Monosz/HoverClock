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
class StopwatchEngineTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Test
    fun startPauseReset_cycle() =
        runTest(dispatcher) {
            var now = 0L
            val engine =
                StopwatchEngine(
                    scope = scope,
                    showMilliseconds = false,
                    autoStart = false,
                    elapsedRealtime = { now },
                )
            assertFalse(engine.state.value.isRunning)
            assertEquals("00:00", engine.state.value.formattedText)

            engine.start()
            assertTrue(engine.state.value.isRunning)
            now += 2_100
            advanceTimeBy(100)
            assertTrue((engine.state.value.elapsedMillis ?: 0L) >= 2_000L)

            engine.pause()
            assertFalse(engine.state.value.isRunning)

            engine.reset()
            assertEquals("00:00", engine.state.value.formattedText)
            assertFalse(engine.state.value.isRunning)
            engine.release()
        }

    @Test
    fun toggle_switchesRunningState() =
        runTest(dispatcher) {
            var now = 0L
            val engine =
                StopwatchEngine(
                    scope = scope,
                    showMilliseconds = false,
                    autoStart = false,
                    elapsedRealtime = { now },
                )
            engine.toggle()
            assertTrue(engine.state.value.isRunning)
            engine.toggle()
            assertFalse(engine.state.value.isRunning)
            engine.release()
        }
}
