package any.ui.common.video

import android.content.Context
import android.net.Uri
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import any.data.cache.ExoVideoCache
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.video.VideoSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

private const val DEFAULT_TICK_INTERVAL = 100L

@Composable
fun rememberVideoPlaybackState(
    uri: Uri,
    progressTickInterval: Long = DEFAULT_TICK_INTERVAL,
): VideoPlaybackState {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val state = remember(uri) {
        VideoPlaybackState(context, scope, progressTickInterval, uri)
    }

    SideEffect {
        state.tickInterval = progressTickInterval
    }

    DisposableEffect(state) {
        PlaybackStateManager.add(state)
        onDispose { PlaybackStateManager.remove(state) }
    }

    return state
}

private object PlaybackStateManager {
    private val states = mutableSetOf<VideoPlaybackState>()

    fun add(state: VideoPlaybackState) {
        states.add(state)
    }

    fun remove(state: VideoPlaybackState) {
        states.remove(state)
    }

    fun onPlay(state: VideoPlaybackState) {
        for (s in states) {
            if (s != state) {
                // Pause other players
                s.pause()
            }
        }
    }
}

@Stable
class VideoPlaybackState internal constructor(
    private val context: Context,
    private val tickScope: CoroutineScope,
    internal var tickInterval: Long,
    val uri: Uri,
) : Player.Listener {
    private var player: ExoPlayer? = null

    private var isReleased = false

    var isRenderedFirstFrame by mutableStateOf(false)
        private set

    var isPlayed by mutableStateOf(false)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var isBuffering by mutableStateOf(false)
        private set

    var error: PlaybackException? by mutableStateOf(null)
        private set

    var duration: Long by mutableStateOf(-0L)
        private set

    var progress: Float by mutableStateOf(-1f)
        private set

    var isMuted: Boolean
        get() = globalIsMuted
        set(value) {
            withPlayer { setMuted(value) }
            globalIsMuted = value
        }

    var videoSize: VideoSize by mutableStateOf(VideoSize.UNKNOWN)
        private set

    private var tickJob: Job? = null

    private val cacheDataSourceFactory by lazy {
        val cache = ExoVideoCache.get(context)
        val cacheSink = CacheDataSink.Factory().setCache(cache)
        val upstreamFactory = DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory())
        CacheDataSource.Factory()
            .setCache(cache)
            .setCacheWriteDataSinkFactory(cacheSink)
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        this.isPlaying = isPlaying
    }

    override fun onRenderedFirstFrame() {
        this.isRenderedFirstFrame = true
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        this.videoSize = videoSize
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        duration = player?.duration ?: 0L
        when (playbackState) {
            Player.STATE_BUFFERING -> {
                isBuffering = true
            }

            Player.STATE_ENDED -> {
                isBuffering = false
            }

            Player.STATE_IDLE -> {
                isBuffering = false
            }

            Player.STATE_READY -> {
                isBuffering = false
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        this.error = error
    }

    fun attachToView(view: TextureView) {
        withPlayer {
            setVideoTextureView(view)
        }
    }

    fun detachFromView(view: TextureView) {
        withPlayer {
            clearVideoTextureView(view)
        }
        release()
    }

    fun play() {
        error = null
        isPlayed = true
        PlaybackStateManager.onPlay(this)
        withPlayer {
            if (currentMediaItem == null) {
                val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
                setMediaSource(mediaSource)
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
            setMuted(globalIsMuted)
            play()
        }
        tickProgress()
    }

    fun pause() {
        withPlayer { pause() }
        tickJob?.cancel()
    }

    fun seek(progress: Float) {
        if (duration <= 0) return
        seek((duration * progress).toLong())
    }

    fun seek(position: Long) {
        withPlayer { seekTo(position) }
    }

    fun updateProgress() {
        if (player == null) return
        withPlayer {
            progress = if (duration > 0) {
                currentPosition.toFloat() / duration
            } else {
                -1f
            }
        }
    }

    private fun tickProgress() {
        tickJob = tickScope.launch {
            while (player != null) {
                updateProgress()
                delay(max(50L, tickInterval))
            }
        }
    }

    fun init() {
        if (player != null) {
            return
        }
        val player = ExoPlayer.Builder(context).build()
        player.addListener(this)
        this.player = player
    }

    fun release() {
        if (player == null || isReleased) return
        withPlayer {
            removeListener(this@VideoPlaybackState)
            release()
        }
        tickJob?.cancel()
        isPlayed = false
        isBuffering = false
        isPlaying = false
        player = null
        isReleased = true
    }

    private fun Player.setMuted(isMuted: Boolean) {
        volume = if (isMuted) 0f else 1f
    }

    private inline fun <T> withPlayer(block: ExoPlayer.() -> T): T {
        check(!isReleased) { "Player is released" }
        val player = checkNotNull(player) { "Player is not initialized" }
        return block(player)
    }

    companion object {
        private var globalIsMuted by mutableStateOf(false)
    }
}
