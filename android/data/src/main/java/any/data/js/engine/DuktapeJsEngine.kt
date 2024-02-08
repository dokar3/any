package any.data.js.engine

import com.squareup.duktape.Duktape

class DuktapeJsEngine private constructor() : JsEngine {
    private val duktape = Duktape.create()

    override val name = NAME

    override val version = "unknown"

    override fun <T : Any> set(name: String, type: Class<T>, instance: T) {
        duktape.set(name, type, instance)
    }

    override suspend fun evaluate(code: String) {
        duktape.evaluate(code)
    }

    override suspend fun <T : Any?> evaluate(code: String, type: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return duktape.evaluate(code) as T
    }

    override fun close() {
        duktape.close()
    }

    class Factory : JsEngine.Factory {
        override fun create(): JsEngine {
            return DuktapeJsEngine()
        }
    }

    companion object {
        const val NAME = "duktape"
    }
}