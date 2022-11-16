package any.base.file

import java.io.IOException
import java.io.InputStream

interface FileReader {
    /**
     * Read the [InputStream] from given filepath.
     *
     * @throws IOException if failed to read file.
     */
    fun read(filepath: String): InputStream
}