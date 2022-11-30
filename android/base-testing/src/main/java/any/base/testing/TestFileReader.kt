package any.base.testing

import any.base.file.FileReader
import java.io.File
import java.io.InputStream

class TestFileReader : FileReader {
    override fun read(filepath: String): InputStream = File(filepath).inputStream()
}