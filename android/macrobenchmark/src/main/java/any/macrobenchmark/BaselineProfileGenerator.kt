package any.macrobenchmark

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
    fun generate() = baselineRule.collectBaselineProfile(
        packageName = TARGET_PACKAGE_NAME,
    ) {
        pressHome()
        startActivityAndWait()

        fun waitObject(selector: BySelector): UiObject2 {
            return device.wait(Until.findObject(selector), 3_000)
        }

        waitObject(By.text("Fresh")).click()
        sampleDataManager.sampleServices().forEach { service ->
            if (!device.hasObject(By.text(service.name))) {
                waitObject(By.res("serviceSelector")).click()
                // Select target service
                waitObject(By.text(service.name)).click()
                device.waitForIdle()
            }
            scrollList(waitObject(By.res("freshPostList")))
            device.waitForIdle()
        }

        waitObject(By.text("Following")).click()
        scrollList(waitObject(By.res("followingList")))
        device.waitForIdle()

        waitObject(By.text("Collections")).click()
        scrollList(waitObject(By.res("collectionList")))
        device.waitForIdle()

        waitObject(By.text("Downloads")).click()
        scrollList(waitObject(By.res("downloadList")))
        device.waitForIdle()

        device.pressBack()
    }

    private fun scrollList(uiList: UiObject2) {
        uiList.setGestureMargin(uiList.visibleBounds.width() / 3)
        uiList.fling(Direction.DOWN)
        uiList.fling(Direction.UP)
    }
}