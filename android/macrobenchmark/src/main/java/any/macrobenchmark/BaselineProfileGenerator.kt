package any.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    private val sampleDataManager = AppSampleDataManager(
        toPackageName = TARGET_PACKAGE_NAME,
        context = context,
    )

    @Before
    fun setup() {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            sampleDataManager.useSampleData()
        }
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

        val waitObjectTimeout = 2_000L

        val freshTab = device.findObject(By.text("Fresh"))
        freshTab.click()
        device.waitForIdle()

        sampleDataManager.sampleServices().forEach { service ->
            if (!device.hasObject(By.text(service.name))) {
                val selector = device.findObject(By.desc("ServiceSelector"))
                selector.click()
                device.wait(Until.hasObject(By.text(service.name)), waitObjectTimeout)
                // Select target service
                val serviceItem = device.findObject(By.text(service.name))
                serviceItem.click()
            }
            device.wait(Until.hasObject(By.desc("FreshPostList")), waitObjectTimeout)
            val freshPostList = device.findObject(By.desc("FreshPostList"))
            scrollList(freshPostList)
            device.waitForIdle()
        }

        val followingTab = device.findObject(By.text("Following"))
        followingTab.click()
        device.wait(Until.hasObject(By.desc("FollowingList")), waitObjectTimeout)
        val followingList = device.findObject(By.desc("FollowingList"))
        scrollList(followingList)
        device.waitForIdle()

        val collectionTab = device.findObject(By.text("Collections"))
        collectionTab.click()
        device.wait(Until.hasObject(By.desc("CollectionList")), waitObjectTimeout)
        val collectionList = device.findObject(By.desc("CollectionList"))
        scrollList(collectionList)
        device.waitForIdle()

        val downloadTab = device.findObject(By.text("Downloads"))
        downloadTab.click()
        device.wait(Until.hasObject(By.desc("DownloadList")), waitObjectTimeout)
        val downloadList = device.findObject(By.desc("DownloadList"))
        scrollList(downloadList)
        device.waitForIdle()

        device.pressBack()
        device.waitForIdle()
    }

    private fun scrollList(uiList: UiObject2) {
        uiList.setGestureMargin(uiList.visibleBounds.width() / 5)
        repeat(3) {
            uiList.scroll(Direction.DOWN, 0.1f, 3000)
        }
        repeat(3) {
            uiList.scroll(Direction.UP, 0.2f, 3000)
        }
    }
}