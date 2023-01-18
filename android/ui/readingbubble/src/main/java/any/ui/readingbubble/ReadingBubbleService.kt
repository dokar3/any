package any.ui.readingbubble

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteCallbackList
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import any.ui.floatingbubble.FloatingView
import any.ui.floatingbubble.FloatingViewManager
import any.ui.readingbubble.entity.ReadingPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ReadingBubbleService : Service() {
    private val binder = IReadingBubbleServiceImpl(this)

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val viewModel = ReadingBubbleViewModel(viewModelScope = coroutineScope)

    private val floatingView = FloatingView(this)

    private val navigateListeners = RemoteCallbackList<INavigateToPostListener>()

    override fun onCreate() {
        super.onCreate()
        setupFloatingView()
        setupAutoDismiss()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        navigateListeners.kill()
        FloatingViewManager.dismiss(floatingView)
    }

    private fun setupFloatingView() {
        floatingView.setOnDismissListener {
            viewModel.clearPosts()
            unbindAll()
            stopSelf()
        }
        floatingView.setContent(
            bubble = {
                val postCount = viewModel.uiState.collectAsState().value.posts.size
                Bubble(postCount = postCount)
            },
            expandedContent = {
                val uiState by viewModel.uiState.collectAsState()
                ReadingBubbleContent(
                    uiState =uiState,
                    onPostClick = {
                        floatingView.hideContentView(
                            restoreFabPosition = true,
                            onAnimationEnd = { navigateToPost(it) },
                        )
                    },
                    onRemovePost = viewModel::removePost,
                    onClearPosts = viewModel::clearPosts,
                )
            }
        )
        FloatingViewManager.show(floatingView)
    }

    private fun setupAutoDismiss() {
        coroutineScope.launch {
            var autoDismissEnabled = false
            viewModel.uiState.collect {
                if (it.posts.isEmpty()) {
                    if (autoDismissEnabled) {
                        delay(125)
                        if (!floatingView.isContentViewVisible()) {
                            withContext(Dispatchers.Main) {
                                floatingView.dismiss()
                            }
                        }
                    }
                } else {
                    autoDismissEnabled = true
                }
            }
        }
    }

    private fun navigateToPost(post: ReadingPost) {
        navigateListeners.beginBroadcast()
        for (i in 0 until navigateListeners.registeredCallbackCount) {
            navigateListeners.getBroadcastItem(i).onNavigate(post)
        }
        navigateListeners.finishBroadcast()

    }

    private fun addPost(post: ReadingPost) {
        viewModel.addPost(post)
    }

    private fun removePost(post: ReadingPost) {
        viewModel.removePost(post)
    }

    private fun clearPosts() {
        viewModel.clearPosts()
    }

    private fun dismiss() {
        floatingView.dismiss()
    }

    private fun addNavigateListener(listener: INavigateToPostListener) {
        navigateListeners.register(listener)
    }

    private fun removeNavigateListener(listener: INavigateToPostListener) {
        navigateListeners.unregister(listener)
    }

    private class IReadingBubbleServiceImpl(
        service: ReadingBubbleService
    ) : IReadingBubbleService.Stub() {
        private val weakRef = WeakReference(service)

        override fun addPost(post: ReadingPost) {
            weakRef.get()?.addPost(post)
        }

        override fun removePost(post: ReadingPost) {
            weakRef.get()?.removePost(post)
        }

        override fun clearPosts() {
            weakRef.get()?.clearPosts()
        }

        override fun dismiss() {
            weakRef.get()?.dismiss()
        }

        override fun addNavigateListener(listener: INavigateToPostListener) {
            weakRef.get()?.addNavigateListener(listener)
        }

        override fun removeNavigateListener(listener: INavigateToPostListener) {
            weakRef.get()?.removeNavigateListener(listener)
        }
    }

    companion object {
        private var sService: IReadingBubbleService? = null

        private val navListeners = WeakHashMap<Context, NavigateToPostListener>()

        private val globalNavListener = object : INavigateToPostListener.Stub() {
            override fun onNavigate(post: ReadingPost) {
                navListeners.values.forEach { it.onNavigate(post) }
            }
        }

        private var connectLatch = CountDownLatch(1)

        private val boundContexts = mutableListOf<WeakReference<Context>>()

        private val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                service as IReadingBubbleService
                sService = service
                service.addNavigateListener(globalNavListener)
                connectLatch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                sService?.removeNavigateListener(globalNavListener)
                sService = null
                connectLatch.countDown()
            }
        }

        fun addPost(context: Context, post: ReadingPost) {
            withService(context) { it.addPost(post) }
        }

        fun removePost(post: ReadingPost) {
            sService?.removePost(post)
        }

        fun addNavigateListener(context: Context, listener: NavigateToPostListener) {
            navListeners[context] = listener
        }

        fun removeNavListener(context: Context) {
            navListeners.remove(context)
        }

        private fun withService(
            context: Context,
            onAvailable: (IReadingBubbleService) -> Unit,
        ) {
            val service = sService
            if (service != null) {
                onAvailable(service)
                return
            }

            connectLatch = CountDownLatch(1)
            val intent = Intent(context, ReadingBubbleService::class.java)
            context.bindService(intent, connection, BIND_AUTO_CREATE)

            @Suppress("DEPRECATION")
            android.os.AsyncTask.THREAD_POOL_EXECUTOR.execute {
                if (connectLatch.await(10, TimeUnit.SECONDS)) {
                    boundContexts.add(WeakReference(context))
                    onAvailable(checkNotNull(sService))
                } else {
                    throw IllegalStateException("Cannot connect to the ReadingBubbleService")
                }
            }
        }

        fun dismiss() {
            sService?.dismiss()
        }

        fun unbind(context: Context) {
            boundContexts.forEach {
                if (it.get() == context) {
                    context.unbindService(connection)
                    return
                }
            }
        }

        fun unbindAll() {
            sService = null
            boundContexts.forEach {
                try {
                    it.get()?.unbindService(connection)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            boundContexts.clear()
        }
    }
}