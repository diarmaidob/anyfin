package io.github.diarmaidob.anyfin.core.common

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ActionThrottlerTest {

    private lateinit var throttler: ActionThrottler

    @Before
    fun setUp() {
        throttler = ActionThrottler()
    }

    @Test
    fun `attempt executes block on first run`() {
        val block = mockk<() -> Unit>(relaxed = true)

        throttler.attempt(1.seconds, block)

        verify(exactly = 1) { block() }
    }

    @Test
    fun `attempt throttles execution if within duration window`() {
        val block = mockk<() -> Unit>(relaxed = true)

        throttler.attempt(10.seconds, block)
        throttler.attempt(10.seconds, block)

        verify(exactly = 1) { block() }
    }

    @Test
    fun `attempt executes block again after duration window passes`() {
        val block = mockk<() -> Unit>(relaxed = true)
        val window = 10.milliseconds

        throttler.attempt(window, block)

        Thread.sleep(window.inWholeMilliseconds + 5)

        throttler.attempt(window, block)

        verify(exactly = 2) { block() }
    }

    @Test
    fun `attemptSuspend returns result on first run`() = runTest {
        val expected = "Success"
        val block = mockk<suspend () -> String> {
            coEvery { this@mockk.invoke() } returns expected
        }

        val result = throttler.attemptSuspend(1.seconds, block)

        assertEquals(expected, result)
        coVerify(exactly = 1) { block() }
    }

    @Test
    fun `attemptSuspend returns null and does not execute block if within window`() = runTest {
        val block = mockk<suspend () -> String>(relaxed = true)

        throttler.attemptSuspend(10.seconds, block)
        val result = throttler.attemptSuspend(10.seconds, block)

        assertEquals(null, result)
        coVerify(exactly = 1) { block() }
    }

    @Test
    fun `attemptSuspend executes again after duration window`() = runTest {
        val block = mockk<suspend () -> String>(relaxed = true)
        val window = 10.milliseconds

        throttler.attemptSuspend(window, block)

        Thread.sleep(window.inWholeMilliseconds + 5)

        throttler.attemptSuspend(window, block)

        coVerify(exactly = 2) { block() }
    }

    @Test
    fun `force executes block and updates timestamp preventing immediate subsequent attempt`() {
        val forceBlock = mockk<() -> String>(relaxed = true)
        val attemptBlock = mockk<() -> Unit>(relaxed = true)

        throttler.force(forceBlock)

        verify(exactly = 1) { forceBlock() }

        throttler.attempt(10.seconds, attemptBlock)

        verify(exactly = 0) { attemptBlock() }
    }

    @Test
    fun `forceSuspend executes block and updates timestamp`() = runTest {
        val forceBlock = mockk<suspend () -> String>(relaxed = true)
        val attemptBlock = mockk<suspend () -> String>(relaxed = true)

        throttler.forceSuspend(forceBlock)

        coVerify(exactly = 1) { forceBlock() }

        val result = throttler.attemptSuspend(10.seconds, attemptBlock)

        assertEquals(null, result)
        coVerify(exactly = 0) { attemptBlock() }
    }

    @Test
    fun `attempt works with zero duration (only throttles if time diff is 0)`() {
        val block = mockk<() -> Unit>(relaxed = true)
        throttler.attempt(0.milliseconds, block)
        verify(exactly = 1) { block() }
    }
}