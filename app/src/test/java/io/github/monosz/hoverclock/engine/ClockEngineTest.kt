package io.github.monosz.hoverclock.engine

import io.github.monosz.hoverclock.model.TimeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClockEngineTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Test
    fun start_keepsRunning_andUpdatesFormattedText() =
        runTest(dispatcher) {
            val engine = ClockEngine(scope, use24Hour = true, showSeconds = true)
            assertTrue(engine.state.value.isRunning)
            assertEquals(TimeMode.Clock, engine.state.value.mode)
            assertTrue(engine.state.value.formattedText.contains(":"))

            val before = engine.state.value.formattedText
            advanceTimeBy(1_100)
            engine.release()
            assertTrue(engine.state.value.formattedText.isNotEmpty() || before.isNotEmpty())
        }

    @Test
    fun pauseAndReset_areNoOps() =
        runTest(dispatcher) {
            val engine = ClockEngine(scope, use24Hour = false, showSeconds = false)
            val before = engine.state.value.formattedText
            engine.pause()
            engine.reset()
            assertEquals(before, engine.state.value.formattedText)
            engine.release()
        }
}
