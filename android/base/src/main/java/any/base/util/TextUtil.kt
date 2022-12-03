package any.base.util

import java.net.URLDecoder
import java.net.URLEncoder

fun String.isHttpUrl(): Boolean {
    return startsWith("http://") || startsWith("https://")
}

fun String.urlEncode(): String {
    return URLEncoder.encode(this, "utf-8")
}

fun String.urlDecode(): String {
    return URLDecoder.decode(this, "utf-8")
}

inline fun CharSequence.indexOfFirst(start: Int = 0, predicate: (Char) -> Boolean): Int {
    for (i in start..lastIndex) {
        if (predicate(this[i])) {
            return i
        }
    }
    return -1
}

inline fun <C, R> C.ifNullOrEmpty(defaultValue: () -> R): R where C : R, R : CharSequence? =
    if (isNullOrEmpty()) defaultValue() else this