package any.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun startup() = baselineRule.collectBaselineProfile(packageName = APP_PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()

        val freshTab = device.findObject(By.text("Fresh"))
        freshTab.click()
        device.waitForIdle()
        val freshList = device.findObject(By.desc("FreshPostList"))
        scrollList(freshList)
        device.waitForIdle()

        val followingTab = device.findObject(By.text("Following"))
        followingTab.click()
        device.waitForIdle()
        val followingList = device.findObject(By.desc("FollowingList"))
        scrollList(followingList)
        device.waitForIdle()

        val collectionTab = device.findObject(By.text("Collections"))
        collectionTab.click()
        device.waitForIdle()
        val collectionList = device.findObject(By.desc("CollectionList"))
        scrollList(collectionList)
        device.waitForIdle()

        device.pressBack()
        device.waitForIdle()
    }

    private fun scrollList(uiList: UiObject2) {
        uiList.setGestureMargin(uiList.visibleBounds.width() / 5)
        repeat(5) {
            uiList.scroll(Direction.DOWN, 0.1f, 3000)
        }
        repeat(3) {
            uiList.scroll(Direction.UP, 0.2f, 3000)
        }
    }
}