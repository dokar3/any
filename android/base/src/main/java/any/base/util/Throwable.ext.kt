package any.base.util

import any.base.R
import any.base.Strings
import java.net.UnknownHostException
import javax.net.ssl.SSLException

fun Throwable.messageForUser(strings: Strings): String {
    return when (this) {
        is UnknownHostException,
        is SSLException -> strings(R.string.network_error)

        else -> strings(R.string._unknown_error, message ?: "")
    }
}