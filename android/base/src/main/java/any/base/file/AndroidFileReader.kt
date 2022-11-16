package any.base.file

import android.content.Context
import any.base.util.FileUtil
import java.io.File
import java.io.InputStream

class AndroidFileReader(context: Context) : FileReader {
    private val appContext = context.applicationContext

    override fun read(filepath: String): InputStream {
        return if (FileUtil.isAssetsFile(filepath)) {
            FileUtil.readAssetsFile(appContext, filepath)
        } else {
            File(filepath).inputStream()
        }
    }
}