package any.base.test

import any.base.AutoCleaner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoCleanerTest {
    @Test
    fun testZeroAliveTime() {
        var cleanCount = 0
        val cleaner = AutoCleaner<String>(
            dispatcher = UnconfinedTestDispatcher(),
            currTimeProvider = { System.currentTimeMillis() },
            onClean = { cleanCount++ },
            aliveTime = 0,
        )

        cleaner.enqueue("Hello")
        cleaner.enqueue("World")

        assertFalse(cleaner.contains("Hello"))
        assertFalse(cleaner.contains("World"))

        assertEquals(2, cleanCount)
    }

    @Test
    fun testAutoClean() {
        val dispatcher = UnconfinedTestDispatcher()
        var cleanCount = 0
        val cleaner = AutoCleaner<String>(
            dispatcher = dispatcher,
            currTimeProvider = { dispatcher.scheduler.currentTime },
            onClean = { cleanCount++ },
            aliveTime = 10_000,
        )

        cleaner.enqueue("Hello")
        cleaner.enqueue("World")

        assertTrue(cleaner.contains("Hello"))
        assertTrue(cleaner.contains("World"))

        dispatcher.scheduler.advanceTimeBy(5_000)

        // Enqueue 'World' again
        cleaner.enqueue("World")

        dispatcher.scheduler.advanceTimeBy(5_001)

        assertFalse(cleaner.contains("Hello"))
        // 'World' should not be cleared
        assertTrue(cleaner.contains("World"))

        assertEquals(1, cleanCount)
    }
}