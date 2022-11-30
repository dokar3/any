package any.data.json

import com.squareup.moshi.Moshi
import okio.buffer
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type

internal object DefaultJson : Json {
    private val moshi = Moshi.Builder()
        .add(ServiceConfigValueAdapter())
        .add(ServiceConfigTypeFallbackAdapter())
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