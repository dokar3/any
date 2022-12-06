package any.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
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

        val waitObjectTimeout = 5_000L

        device.findObject(By.text("Fresh")).click()
        device.waitForIdle()

        sampleDataManager.sampleServices().forEach { service ->
            if (!device.hasObject(By.text(service.name))) {
                device.findObject(By.res("serviceSelector")).click()
                device.wait(Until.hasObject(By.text(service.name)), waitObjectTimeout)
                // Select target service
                device.findObject(By.text(service.name)).click()
            }
            device.wait(Until.hasObject(By.res("freshPostList")), waitObjectTimeout)
            scrollList(device.findObject(By.res("freshPostList")))

            device.waitForIdle()
        }

        device.findObject(By.text("Following")).click()
        device.wait(Until.hasObject(By.res("followingList")), waitObjectTimeout)
        scrollList(device.findObject(By.res("followingList")))

        device.waitForIdle()

        device.findObject(By.text("Collections")).click()
        device.wait(Until.hasObject(By.res("collectionList")), waitObjectTimeout)
        scrollList(device.findObject(By.res("collectionList")))

        device.waitForIdle()

        device.findObject(By.text("Downloads")).click()
        device.wait(Until.hasObject(By.res("downloadList")), waitObjectTimeout)
        scrollList(device.findObject(By.res("downloadList")))
        device.waitForIdle()

        device.pressBack()
        device.waitForIdle()
    }

    private fun scrollList(uiList: UiObject2) {
        uiList.setGestureMargin(uiList.visibleBounds.width() / 3)
        uiList.fling(Direction.DOWN)
        uiList.fling(Direction.UP)
    }
}