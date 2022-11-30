package any.ui.common.widget

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager

@Composable
fun rememberAndroidViewPagerState(
    initialPage: Int = 0,
): AndroidViewPagerState {
    return rememberSaveable(
        inputs = emptyArray(),
        saver = Saver(
            save = { it.currentPage },
            restore = { savedCurrentPage ->
                AndroidViewPagerState(initialPage = savedCurrentPage)
            }
        )
    ) {
        AndroidViewPagerState(initialPage = initialPage)
    }
}

@ExperimentalComposeUiApi
@Composable
fun <E> AndroidViewPager(
    state: AndroidViewPagerState,
    items: List<E>,
    onCreateItem: (Context, Int, E) -> View,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewPager = remember {
        ViewPager(context).apply {
            adapter = SimpleViewPagerAdapter(
                items = items,
                onCreateItem = onCreateItem,
            )
        }
    }

    LaunchedEffect(viewPager) {
        state.attachViewPager(viewPager)
    }

    DisposableEffect(viewPager) {
        onDispose {
            state.detachViewPager(viewPager)
        }
    }

    AndroidView(
        factory = { viewPager },
        modifier = modifier,
        update = {},
    )
}

class AndroidViewPagerState(
    internal var initialPage: Int = 0,
) : ViewPager.OnPageChangeListener {

    private var viewPager: ViewPager? = null

    internal fun attachViewPager(viewPager: ViewPager) {
        this.viewPager = viewPager
        // Listen current page changes
        viewPager.addOnPageChangeListener(this)
        // Restore current page
        if (viewPager.adapter != null) {
            setCurrentPage(page = initialPage, animate = false)
        }
    }

    internal fun detachViewPager(viewPager: ViewPager) {
        viewPager.removeOnPageChangeListener(this)
        this.viewPager = null
    }

    var currentPage by mutableStateOf(0)
        private set

    fun setCurrentPage(page: Int, animate: Boolean = true) {
        requireViewPager().setCurrentItem(page, animate)
    }

    private fun requireViewPager(): ViewPager {
        return checkNotNull(viewPager) {
            "Make sure this state had set to AndroidViewPage()"
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        currentPage = position
    }

    override fun onPageScrollStateChanged(state: Int) {
    }
}

private class SimpleViewPagerAdapter<E>(
    private val items: List<E>,
    private val onCreateItem: (Context, Int, E) -> View,
) : PagerAdapter() {
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = onCreateItem(container.context, position, items[position])
        container.addView(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return view
    }
}