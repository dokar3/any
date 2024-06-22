package any.data.js.engine

import any.data.entity.Comment
import any.data.entity.JsFetchResult
import any.data.entity.JsPagedResult
import any.data.entity.JsPost
import any.data.entity.JsUser
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.conveter.JsonClassConverter
import com.dokar.quickjs.evaluate
import kotlinx.coroutines.Dispatchers
import kotlin.reflect.KType

class QuickJsEngine : JsEngine {
    private val quickJs = QuickJs.create(Dispatchers.IO)

    override val name: String = "QuickJS"

    override val version: String = quickJs.version

    init {
        quickJs.maxStackSize = 5 * 1024 * 1024L
        quickJs.addTypeConverters(
            JsonClassConverter<JsFetchResult<JsPost>>(),
            JsonClassConverter<JsFetchResult<JsUser>>(),
            JsonClassConverter<JsPagedResult<List<JsPost>>>(),
            JsonClassConverter<JsPagedResult<List<Comment>>>(),
        )
    }

    override fun <T : Any> set(name: String, type: Class<T>, instance: T) {
        quickJs.define(name = name, type = type, instance = instance)
    }

    override suspend fun evaluate(code: String) {
        quickJs.evaluate<Any>(code)
    }

    override suspend fun <T : Any?> evaluate(code: String, type: KType): T? {
        return quickJs.evaluate(code, type)
    }

    override fun close() {
        quickJs.close()
    }

    class Factory : JsEngine.Factory {
        override fun create(): JsEngine {
            return QuickJsEngine()
        }
    }
}