package any.ui.common

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import java.lang.reflect.Field

@OptIn(ExperimentalMaterialApi::class)
private val animationScopeField: Field by lazy {
    PullRefreshState::class.java.getDeclaredField("animationScope").also {
        it.isAccessible = true
    }
}

@OptIn(ExperimentalMaterialApi::class)
private val PullRefreshState.animationScope: CoroutineScope
    get() = animationScopeField.get(this) as CoroutineScope

@OptIn(ExperimentalMaterialApi::class)
suspend fun PullRefreshState.awaitAnimations() {
    animationScope.coroutineContext.job.children.toList().joinAll()
}
