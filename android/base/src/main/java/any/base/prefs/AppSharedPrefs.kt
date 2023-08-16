@file:Suppress("DEPRECATION")

package any.base.prefs

import android.content.Context
import android.preference.PreferenceManager
import any.base.model.DarkMode
import any.base.model.FolderViewType
import any.base.model.PostFolderSelectionSorting
import any.base.model.PostSorting
import any.base.model.toEnabledState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.atomic.AtomicInteger

const val LAUNCH_SCREEN_FRESH = "fresh"

const val LAUNCH_SCREEN_FOLLOWING = "following"

const val LAUNCH_SCREEN_COLLECTIONS = "collection"

const val LAUNCH_SCREEN_DOWNLOADS = "downloads"

private const val DEFAULT_FONT_SCALE = 1f

private const val DEFAULT_LINE_SPACING_MUL = 1.8f

private val LOCK = Any()

private var store: PreferencesStore? = null

fun Context.preferencesStore(): PreferencesStore {
    return sharedPreferencesStore(applicationContext)
}

private fun sharedPreferencesStore(context: Context): PreferencesStore {
    return store ?: synchronized(LOCK) {
        store ?: PreferenceManager
            .getDefaultSharedPreferences(context)
            .let {
                SharedPreferencesStore(it)
            }
            .also {
                store = it
            }
    }
}

val PreferencesStore.currentService
    get() = prefValueOf("current_service") { key ->
        nullableStringValue(this, key)
    }

val PreferencesStore.launchScreen
    get() = prefValueOf("launch_screen") { key ->
        nullableStringValue(this, key)
    }

val PreferencesStore.primaryColor
    get() = prefValueOf("primary_color") { key ->
        intValue(this, key)
    }

val PreferencesStore.darkModePrimaryColor
    get() = prefValueOf("dark_mode_primary_color") { key ->
        intValue(this, key)
    }

private val PreferencesStore.darkModePrefValue
    get() = prefValueOf("dark_mode") { key ->
        stringValue(this, key, defaultValue = DarkMode.System.name)
    }

var PreferencesStore.darkMode: DarkMode
    get() {
        return try {
            DarkMode.valueOf(darkModePrefValue.value)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            DarkMode.System
        }
    }
    set(value) {
        darkModePrefValue.value = value.name
    }

fun PreferencesStore.darkModeFlow(): Flow<DarkMode> {
    return darkModePrefValue
        .asFlow()
        .map { darkMode }
}

/**
 * Used to update latest dark mode enabled state in flows are created by [darkModeEnabledFlow]
 */
internal object DarkModeEnabledFlowUpdater {
    private val tick = AtomicInteger(0)

    internal val flow by lazy { MutableStateFlow(tick.get()) }

    fun update() {
        flow.tryEmit(tick.incrementAndGet())
    }
}

fun PreferencesStore.darkModeEnabledFlow(
    context: Context,
    scope: CoroutineScope,
): StateFlow<Boolean> {
    val appContext = context.applicationContext
    return combine(
        flow = darkModeFlow(),
        flow2 = DarkModeEnabledFlowUpdater.flow,
        transform = { darkMode, _ -> darkMode }
    )
        .onStart { emit(darkMode) }
        .map { it.toEnabledState(appContext) }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Lazily,
            initialValue = darkMode.toEnabledState(context),
        )
}

val PreferencesStore.darkenedImages
    get() = prefValueOf("darkened_images") { key ->
        boolValue(this, key)
    }

val PreferencesStore.monochromeImages
    get() = prefValueOf("monochrome_images") { key ->
        boolValue(this, key)
    }

val PreferencesStore.transparentImages
    get() = prefValueOf("transparent_images") { key ->
        boolValue(this, key)
    }

val PreferencesStore.appPassword
    get() = prefValueOf("app_password") { key ->
        nullableStringValue(this, key)
    }

val PreferencesStore.isSecureScreenEnabled
    get() = prefValueOf("secure_screen_enabled") { key ->
        boolValue(this, key, defaultValue = true)
    }

val PreferencesStore.overrideServiceHeaderImage
    get() = prefValueOf("override_service_header_image") { key ->
        boolValue(this, key, defaultValue = false)
    }

val PreferencesStore.headerImage
    get() = prefValueOf("header_image") { key ->
        nullableStringValue(this, key)
    }

val PreferencesStore.showDevOptions
    get() = prefValueOf("show_dev_options") { key ->
        boolValue(this, key, defaultValue = false)
    }

private val PreferencesStore.postSortingPrefValue
    get() = prefValueOf("post_sorting") { key ->
        stringValue(this, key, defaultValue = PostSorting.ByAddTime.name)
    }

var PreferencesStore.postSorting: PostSorting
    get() {
        return try {
            PostSorting.valueOf(postSortingPrefValue.value)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            PostSorting.ByAddTime
        }
    }
    set(value) {
        postSortingPrefValue.value = value.name
    }

fun PreferencesStore.postSortingFlow(): Flow<PostSorting> {
    return postSortingPrefValue
        .asFlow()
        .map { postSorting }
}

private val PreferencesStore.postFolderSelectionSortingPrefValue
    get() = prefValueOf("post_folder_selection_sorting") { key ->
        stringValue(this, key, defaultValue = PostFolderSelectionSorting.ByTitle.name)
    }

var PreferencesStore.postFolderSelectionSorting: PostFolderSelectionSorting
    get() {
        return try {
            PostFolderSelectionSorting.valueOf(
                postFolderSelectionSortingPrefValue.value
            )
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            PostFolderSelectionSorting.ByTitle
        }
    }
    set(value) {
        postFolderSelectionSortingPrefValue.value = value.name
    }

private val PreferencesStore.defaultFolderViewTypePrefValue
    get() = prefValueOf("default_folder_view_type") { key ->
        stringValue(this, key, defaultValue = FolderViewType.List.name)
    }

var PreferencesStore.defaultFolderViewType: FolderViewType
    get() {
        return try {
            FolderViewType.valueOf(defaultFolderViewTypePrefValue.value)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            FolderViewType.List
        }
    }
    set(value) {
        defaultFolderViewTypePrefValue.value = value.name
    }

private val PreferencesStore.forcedFolderViewTypePrefValue
    get() = prefValueOf("forced_folder_view_type") { key ->
        nullableStringValue(this, key)
    }

var PreferencesStore.forcedFolderViewType: FolderViewType?
    get() {
        return forcedFolderViewTypePrefValue.value?.let {
            try {
                FolderViewType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                null
            }
        }
    }
    set(value) {
        forcedFolderViewTypePrefValue.value = value?.name
    }

fun PreferencesStore.forcedFolderViewTypeFlow(): Flow<FolderViewType?> {
    return forcedFolderViewTypePrefValue
        .asFlow()
        .map { forcedFolderViewType }
}

val PreferencesStore.postFontScale
    get() = prefValueOf("post_font_scale") { key ->
        floatValue(this, key, defaultValue = DEFAULT_FONT_SCALE)
    }

val PreferencesStore.postLineSpacingMultiplier
    get() = prefValueOf("post_line_spacing_mul") { key ->
        floatValue(this, key, defaultValue = DEFAULT_LINE_SPACING_MUL)
    }

val PreferencesStore.versionCodeIgnoresBuiltinServiceUpdates
    get() = prefValueOf("version_code_ignores_builtin_service_updates") { key ->
        longValue(this, key, defaultValue = 0L)
    }

val PreferencesStore.fixedTopBar
    get() = prefValueOf("fixed_top_bar") { key ->
        boolValue(this, key, defaultValue = false)
    }

val PreferencesStore.fixedBottomBar
    get() = prefValueOf("fixed_bottom_bar") { key ->
        boolValue(this, key, defaultValue = false)
    }

val PreferencesStore.maxImageCacheSize
    get() = prefValueOf("max_image_cache_size") { key ->
        longValue(this, key, defaultValue = -1L)
    }

val PreferencesStore.maxVideoCacheSize
    get() = prefValueOf("max_video_cache_size") { key ->
        longValue(this, key, defaultValue = -1L)
    }

@Suppress("unchecked_cast")
private fun <T> PreferencesStore.prefValueOf(
    key: String,
    initBlock: (key: String) -> PreferenceValue<T>
): PreferenceValue<T> {
    return values.getOrPut(key) {
        initBlock(key)
    } as PreferenceValue<T>
}
