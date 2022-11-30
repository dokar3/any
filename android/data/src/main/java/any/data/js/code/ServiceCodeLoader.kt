package any.data.js.code

import any.data.entity.Checksums

interface ServiceCodeLoader {
    suspend fun load(checksums: Checksums, url: String): String
}