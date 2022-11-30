package any.base.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import kotlin.system.exitProcess


object CrashHandler {
    private const val LOG_FILE_EXT = ".log"

    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

    private lateinit var logDir: File

    @SuppressLint("SimpleDateFormat")
    private val logFileDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    fun init(context: Context) {
        logDir = File(context.filesDir, "crashes")
        if (!logDir.exists()) {
            logDir.mkdir()
        }

        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

        val uiHandler = Handler(Looper.getMainLooper())

        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            uiHandler.post {
                Toast.makeText(
                    context.applicationContext,
                    "App crashed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            writeCrashLog(e)

            val defHandler = defaultExceptionHandler
            if (defHandler != null) {
                defHandler.uncaughtException(thread, e)
            } else {
                exitProcess(1)
            }
        }
    }

    private fun writeCrashLog(e: Throwable) {
        val now = System.currentTimeMillis()
        val logFile = File(logDir, logFileDateFormat.format(now) + LOG_FILE_EXT)
        logFile.createNewFile()
        logFile.bufferedWriter()
            .use { it.write(e.stackTraceToString()) }
    }

    fun crashLogFiles(): List<File> {
        if (!CrashHandler::logDir.isInitialized) {
            throw IllegalStateException("Call init() first!")
        }
        return logDir
            .listFiles { file: File ->
                file.isFile && file.name.endsWith(LOG_FILE_EXT)
            }
            ?.toList() ?: emptyList()
    }
}