package any.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is an example startup benchmark.
 *
 * It navigates to the device's home screen, and launches the default activity.
 *
 * Before running this benchmark:
 * 1) switch your app's active build variant in the Studio (affects Studio runs only)
 * 2) add `<profileable android:shell="true" />` to your app's manifest, within the `<application>` tag
 *
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun startup() = baselineRule.collectBaselineProfile(packageName = "com.dokar.any") {
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