package any.base.util

import any.base.R
import any.base.Strings
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

fun Throwable.userFriendlyMessage(strings: Strings): String {
    when (this) {
        is ConnectException,
        is UnknownHostException,
        is SocketException,
        is SSLException -> return strings(R.string.network_error)

        is InterruptedIOException -> {
            if (message == "timeout") {
                return strings(R.string.network_error_timeout)
            }
        }
    }
    return message ?: ""
}