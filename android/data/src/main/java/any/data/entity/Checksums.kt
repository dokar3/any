package any.data.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Checksums(
    val md5: String,
    val sha1: String,
    val sha256: String,
    val sha512: String,
)
