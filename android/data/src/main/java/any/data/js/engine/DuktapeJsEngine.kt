package any.data.js.engine

import com.squareup.duktape.Duktape

class DuktapeJsEngine private constructor() : JsEngine {
    private val duktape = Duktape.create()

    override val name = NAME

    override val version = "unknown"

    override fun <T : Any> set(name: String, clz: Class<T>, instance: T) {
        duktape.set(name, clz, instance)
    }

    override fun evaluate(jsCode: String) {
        duktape.evaluate(jsCode)
    }

    override fun <T> evaluate(jsCode: String, clz: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return duktape.evaluate(jsCode) as? T
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