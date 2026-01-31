package io.github.diarmaidob.anyfin.common.test

import io.github.diarmaidob.anyfin.core.common.ScreenStateDelegate
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTestBase {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setupBase() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this)
    }

    @After
    fun tearDownBase() {
        Dispatchers.resetMain()
    }

    protected fun mockScreenStateDelegate(delegate: ScreenStateDelegate<*>) {
        coEvery {
            delegate.load(any<Boolean>(), any<suspend () -> Unit>())
        } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
            Result.success(Unit)
        }

        every {
            delegate.performThrottledAction(any<() -> Unit>())
        } answers {
            firstArg<() -> Unit>().invoke()
        }
    }

    protected fun TestScope.startCollecting(flow: StateFlow<*>) {
        this.backgroundScope.launch { flow.collect() }
    }

    protected fun <T> TestScope.awaitValue(flow: StateFlow<T>): T {
        this.advanceUntilIdle()
        return flow.value
    }
}