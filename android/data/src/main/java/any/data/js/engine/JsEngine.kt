package any.data.js.engine

import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface JsEngine : AutoCloseable {
    val name: String

    val version: String

    fun <T : Any> set(name: String, type: Class<T>, instance: T)

    suspend fun evaluate(code: String)

    suspend fun <T : Any?> evaluate(code: String, type: KType): T?

    interface Factory {
        fun create(): JsEngine
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
suspend inline fun <reified T : Any?> JsEngine.evaluate(code: String): T? {
    return evaluate(code, typeOf<T>())
}