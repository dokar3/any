package any.data.json

import any.data.entity.ServiceConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import okio.buffer
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type

internal object DefaultJson : Json {
    private val moshi = Moshi.Builder()
        .add(
            PolymorphicJsonAdapterFactory.of(ServiceConfig::class.java, "type")
                .withSubtype(ServiceConfig.Bool::class.java, "boolean")
                .withSubtype(ServiceConfig.Number::class.java, "number")
                .withSubtype(ServiceConfig.Text::class.java, "text")
                .withSubtype(ServiceConfig.Url::class.java, "url")
                .withSubtype(ServiceConfig.Option::class.java, "option")
                .withSubtype(ServiceConfig.Cookies::class.java, "cookies")
                .withSubtype(ServiceConfig.CookiesUa::class.java, "cookies_ua")
        )
        .build()

    override fun <T> fromJson(json: String, type: Type): T? {
        return moshi.adapter<T>(type).fromJson(json)
    }

    override fun <T> fromJson(json: String, clz: Class<T>): T? {
        return moshi.adapter(clz).fromJson(json)
    }

    override fun <T> fromJson(json: InputStream, type: Type): T? {
        return json.source().buffer().use { source ->
            moshi.adapter<T>(type).fromJson(source)
        }
    }

    override fun <T> fromJson(json: InputStream, clz: Class<T>): T? {
        return json.source().buffer().use { source ->
            moshi.adapter(clz).fromJson(source)
        }
    }

    override fun <T : Any> toJson(src: T, clz: Class<T>): String {
        return moshi.adapter(clz).toJson(src)
    }

    override fun <T : Any> toJson(src: T, output: OutputStream, clz: Class<T>) {
        output.sink().buffer().use { sink ->
            moshi.adapter(clz).toJson(sink, src)
        }
    }
}