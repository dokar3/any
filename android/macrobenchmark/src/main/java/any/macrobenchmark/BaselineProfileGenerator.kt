package any.macrobenchmark

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@RequiresApi(Build.VERSION_CODES.P)
class BaselineProfileGenerator {
    @get:Rule
    val baselineRule = BaselineProfileRule()

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val sampleDataManager = SampleDataManager(
        targetPackageName = TARGET_PACKAGE_NAME,
        context = context,
    )

    @Before
    fun setup() {
        sampleDataManager.useSampleData()
    }

    @After
    fun teardown() {
        sampleDataManager.clearSampleData()
    }

    @Test
    fun generate() = baselineRule.collect(
        packageName = TARGET_PACKAGE_NAME,
    ) {
        fun launch() {
            pressHome()
            startActivityAndWait {
                it.putExtra("extra.block_all_main_screen_nav", true)
            }
        }

        launch()

        @Throws(Exception::class)
        fun waitObject(
            selector: BySelector,
            failedCount: Int = 0,
        ): UiObject2 {
            return try {
                device.wait(Until.findObject(selector), 1_000)
            } catch (e: Exception) {
                launch()
                if (failedCount == 1) {
                    throw Exception("Wait object error, selector: ${selector}, error: $e")
                } else {
                    return waitObject(selector, failedCount + 1)
                }
            }
        }

        waitObject(By.text("Fresh")).click()
        sampleDataManager.sampleServices().forEach { service ->
            if (!device.hasObject(By.text(service.name))) {
                withRetry {
                    onBeforeRetry {
                        launch()
                    }
                    waitObject(By.res("serviceSelector")).click()
                    // Select target service
                    waitObject(By.text(service.name)).click()
                    device.waitForIdle()
                    scrollList(waitObject(By.res("freshPostList")))
                    device.waitForIdle()
                }
            }
        }

        withRetry {
            onBeforeRetry {
                launch()
            }
            waitObject(By.text("Following")).click()
            scrollList(waitObject(By.res("followingList")))
            device.waitForIdle()
        }

        withRetry {
            onBeforeRetry {
                launch()
            }
            waitObject(By.text("Collections")).click()
            scrollList(waitObject(By.res("collectionList")))
            device.waitForIdle()
        }

        withRetry {
            onBeforeRetry {
                launch()
            }
            waitObject(By.text("Downloads")).click()
            scrollList(waitObject(By.res("downloadList")))
            device.waitForIdle()
        }

        device.pressBack()
    }

    private fun scrollList(uiList: UiObject2) {
        uiList.setGestureMargin(uiList.visibleBounds.width() / 3)
        uiList.fling(Direction.DOWN)
    }

    private fun withRetry(maxRetries: Int = 5, block: RetryScope.() -> Unit) {
        val scope = RetryScopeImpl()
        for (i in 0..<maxRetries) {
            try {
                scope.block()
                return
            } catch (e: Exception) {
                if (i == maxRetries - 1) {
                    throw e
                } else {
                    scope.onBeforeRetryAction?.invoke()
                }
            }
        }
    }

    interface RetryScope {
        fun onBeforeRetry(action: () -> Unit)
    }

    private class RetryScopeImpl : RetryScope {
        var onBeforeRetryAction: (() -> Unit)? = null

        override fun onBeforeRetry(action: () -> Unit) {
            this.onBeforeRetryAction = action
        }
    }
}