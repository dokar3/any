package any.data.js.engine

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.evaluate

class QuickJsEngine : JsEngine {
    private val quickJs = QuickJs.create()

    override val name: String = "QuickJS"

    override val version: String = quickJs.version

    init {
        quickJs.maxStackSize = 5 * 1024 * 1024L
    }

    override fun <T : Any> set(name: String, type: Class<T>, instance: T) {
        quickJs.define(name = name, type = type, instance = instance)
    }

    override suspend fun evaluate(code: String) {
        quickJs.evaluate<Any>(code)
    }

    override suspend fun <T : Any?> evaluate(code: String, type: Class<T>): T? {
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