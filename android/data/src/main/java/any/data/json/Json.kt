package any.data.json

import com.squareup.moshi.Types
import com.squareup.moshi.rawType
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

interface Json {
    fun <T> fromJson(json: String, type: Type): T?

    fun <T> fromJson(json: String, clz: Class<T>): T?

    fun <T> fromJson(json: InputStream, type: Type): T?

    fun <T> fromJson(json: InputStream, clz: Class<T>): T?

    fun <T : Any> toJson(src: T, clz: Class<T>): String

    fun <T : Any> toJson(src: T, output: OutputStream, clz: Class<T>)

    companion object : Json {
        override fun <T> fromJson(json: String, type: Type): T? {
            return DefaultJson.fromJson(json, type)
        }

        override fun <T> fromJson(json: String, clz: Class<T>): T? {
            return DefaultJson.fromJson(json, clz)
        }

        override fun <T> fromJson(json: InputStream, type: Type): T? {
            return DefaultJson.fromJson(json, type)
        }

        override fun <T> fromJson(json: InputStream, clz: Class<T>): T? {
            return DefaultJson.fromJson(json, clz)
        }

        override fun <T : Any> toJson(src: T, clz: Class<T>): String {
            return DefaultJson.toJson(src, clz)
        }

        override fun <T : Any> toJson(src: T, output: OutputStream, clz: Class<T>) {
            return DefaultJson.toJson(src, output, clz)
        }

        inline fun <reified T> parameterizedType(): Type {
            val token = object : `$TypeToken`<T>() {}
            val tokenParameterizedType = (token::class.java.genericSuperclass as ParameterizedType)
                .actualTypeArguments[0] as ParameterizedType
            return Types.newParameterizedType(
                T::class.java.rawType,
                *tokenParameterizedType.actualTypeArguments
            )
        }

        @Suppress("unused", "ClassName")
        open class `$TypeToken`<T> protected constructor()
    }
}