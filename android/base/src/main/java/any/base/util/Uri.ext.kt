package any.base.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


suspend fun Uri.writeToFile(
    context: Context,
    file: File,
) = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(this@writeToFile)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

suspend fun Uri.queryName(
    context: Context
): String? = withContext(Dispatchers.IO) {
    val cursor = context.contentResolver.query(this@queryName, null, null, null, null)
        ?: return@withContext null
    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    cursor.moveToFirst()
    val name = cursor.getString(nameIndex)
    cursor.close()
    name
}