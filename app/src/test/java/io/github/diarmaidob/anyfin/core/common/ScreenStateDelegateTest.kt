package io.github.diarmaidob.anyfin.core.common

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenStateDelegateTest {

    @MockK
    lateinit var actionThrottler: ActionThrottler

    @MockK
    lateinit var refreshThrottler: ActionThrottler

    private lateinit var delegate: ScreenStateDelegate<TestEvent>

    sealed class TestEvent {
        data object Dummy : TestEvent()
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        delegate = ScreenStateDelegate(actionThrottler, refreshThrottler)
    }

    @Test
    fun `initial state is correct`() = runTest {
        val stateFlow = delegate.bind(backgroundScope)
        backgroundScope.launch { stateFlow.collect() }
        runCurrent()

        val state = stateFlow.value

        assertEquals(false, state.isPullRefreshing)
        assertEquals(false, state.isSyncing)
        assertEquals(false, state.isRawSyncing)
        assertEquals(false, state.hasInitialized)
        assertEquals(null, state.error)
        assertEquals(0, state.events.size)
    }

    @Test
    fun `sendEvent adds event to state`() = runTest {
        val stateFlow = delegate.bind(backgroundScope)
        backgroundScope.launch { stateFlow.collect() }
        runCurrent()

        delegate.sendEvent(TestEvent.Dummy)
        runCurrent() // Ensure combine emits the new value

        val state = stateFlow.value
        assertEquals(1, state.events.size)
        assertEquals(TestEvent.Dummy, state.events.first().content)
    }

    @Test
    fun `consumeEvent removes event from state`() = runTest {
        val stateFlow = delegate.bind(backgroundScope)
        backgroundScope.launch { stateFlow.collect() }
        runCurrent()

        delegate.sendEvent(TestEvent.Dummy)
        runCurrent() // Ensure event is added

        val eventId = stateFlow.value.events.first().id

        delegate.consumeEvent(eventId)
        runCurrent() // Ensure removal propagates

        assertEquals(0, stateFlow.value.events.size)
    }

    @Test
    fun `performThrottledAction delegates to actionThrottler`() {
        val action = mockk<() -> Unit>(relaxed = true)
        every { actionThrottler.attempt(any(), any()) } just Runs

        delegate.performThrottledAction(action)

        verify { actionThrottler.attempt(150.milliseconds, action) }
    }

    @Test
    fun `load with manual refresh updates isPullRefreshing`() = runTest {
        coEvery { refreshThrottler.forceSuspend<Unit>(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        val stateFlow = delegate.bind(backgroundScope)
        backgroundScope.launch { stateFlow.collect() }
        runCurrent()

        backgroundScope.launch {
            delegate.load(isManualRefresh = true) {
                delay(1000)
            }
        }
        advanceTimeBy(100)
        runCurrent() // Propagate state updates

        assertEquals(true, stateFlow.value.isPullRefreshing)
        assertEquals(false, stateFlow.value.isRawSyncing)
        assertEquals(null, stateFlow.value.error)

        advanceTimeBy(1000)
        advanceTimeBy(50)
        runCurrent()

        assertEquals(false, stateFlow.value.isPullRefreshing)
        assertEquals(true, stateFlow.value.hasInitialized)
    }

    @Test
    fun `load without manual refresh updates isRawSyncing`() = runTest {
        coEvery { refreshThrottler.attemptSuspend<Unit>(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }

        val stateFlow = delegate.bind(backgroundScope)
        backgroundScope.launch { stateFlow.collect() }
        runCurrent()

        backgroundScope.launch {
            delegate.load(isManualRefresh = false) {
                delay(1000)
            }
        }
        advanceTimeBy(100)
        runCurrent()

        assertEquals(false, stateFlow.value.isPullRefreshing)
        assertEquals(true, stateFlow.value.isRawSyncing)

        advanceTimeBy(1000)
        advanceTimeBy(50)
        runCurrent()

        assertEquals(false, stateFlow.value.isRawSyncing)
        assertEquals(true, stateFlow.value.hasInitialized)
    }

    @Test
    fun `isSyncing is debounced by 500ms when raw syncing`() = runTest {
        coEvery { refreshThrottler.attemptSuspend<Unit>(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }

        val stateFlow = delegate.bind(backgroundScope)
        backgroundScope.launch { stateFlow.collect() }
        runCurrent()

        backgroundScope.launch {
            delegate.load(isManualRefresh = false) {
                delay(1000)
            }
        }

        advanceTimeBy(100)
        runCurrent()
        assertEquals(true, stateFlow.value.isRawSyncing)
        assertEquals(false, stateFlow.value.isSyncing)

        advanceTimeBy(401)
        runCurrent()
        assertEquals(true, stateFlow.value.isSyncing)

        advanceTimeBy(500)
        advanceTimeBy(50)
        runCurrent()

        assertEquals(false, stateFlow.value.isRawSyncing)
        assertEquals(false, stateFlow.value.isSyncing)
    }

    @Test
    fun `load captures exception and updates error state`() = runTest {
        val expectedError = RuntimeException("Boom")
        coEvery { refreshThrottler.attemptSuspend<Unit>(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }

        val stateFlow = delegate.bind(backgroundScope)
        backgroundScope.launch { stateFlow.collect() }
        runCurrent()

        val result = delegate.load(isManualRefresh = false) {
            throw expectedError
        }
        runCurrent()

        advanceTimeBy(50)
        runCurrent()

        assertEquals(true, result!!.isFailure)
        assertEquals(expectedError, stateFlow.value.error)
        assertEquals(false, stateFlow.value.isRawSyncing)
        assertEquals(true, stateFlow.value.hasInitialized)
    }

    @Test
    fun `load waits 50ms in finally block before resetting flags`() = runTest {
        coEvery { refreshThrottler.forceSuspend<Unit>(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        val stateFlow = delegate.bind(backgroundScope)
        backgroundScope.launch { stateFlow.collect() }
        runCurrent()

        backgroundScope.launch {
            delegate.load(isManualRefresh = true) { }
        }

        advanceTimeBy(10)
        runCurrent()

        assertEquals(true, stateFlow.value.isPullRefreshing)

        advanceTimeBy(50)
        runCurrent()

        assertEquals(false, stateFlow.value.isPullRefreshing)
    }

    @Test
    fun `load uses correct throttler method based on isManualRefresh`() = runTest {
        coEvery { refreshThrottler.forceSuspend<Unit>(any()) } returns Unit
        coEvery { refreshThrottler.attemptSuspend<Unit>(any(), any()) } returns Unit

        delegate.load(isManualRefresh = true) {}
        coVerify { refreshThrottler.forceSuspend<Unit>(any()) }

        delegate.load(isManualRefresh = false) {}
        coVerify { refreshThrottler.attemptSuspend<Unit>(5.minutes, any()) }
    }

    @Test
    fun `load returns success result when block succeeds`() = runTest {
        coEvery { refreshThrottler.attemptSuspend<String>(any(), any()) } returns "Success"

        val result = delegate.load(isManualRefresh = false) { "Success" }

        assertNotEquals(null, result)
        assertEquals(true, result!!.isSuccess)
        assertEquals("Success", result.getOrNull())
    }
}