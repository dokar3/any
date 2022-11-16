package any.base.util

import java.security.MessageDigest

fun String.md5HexString(): String {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(toByteArray()).toHexString()
}

fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}