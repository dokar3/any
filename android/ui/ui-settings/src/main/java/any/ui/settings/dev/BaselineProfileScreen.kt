package any.ui.settings.dev

import any.base.R as BaseR
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.profileinstaller.ProfileVerifier
import androidx.profileinstaller.ProfileVerifier.CompilationStatus
import any.ui.common.theme.pass
import any.ui.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
internal fun BaselineProfileScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    var compilationStatus by remember { mutableStateOf<CompilationStatus?>(null) }
    var compilationStatusText by remember { mutableStateOf("?") }

    var showManualCompilation by remember { mutableStateOf(false) }
    var manualCompilationEnabled by remember { mutableStateOf(false) }

    var compileResult: CompileResult? by remember { mutableStateOf(null) }

    var reloadCompilationStatus by remember { mutableStateOf(0) }

    fun compileNow() {
        manualCompilationEnabled = false
        scope.launch(Dispatchers.Default) {
            compileResult = compileBaselineProfile(packageName = context.packageName)
            compilationStatus = getCompilationStatusSync(context)
            reloadCompilationStatus++
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.updateTitle(context.resources.getString(BaseR.string.baseline_profile))
        viewModel.setShowBackArrow(true)
    }

    LaunchedEffect(Unit) {
        ProfileVerifier.getCompilationStatusAsync().addListener(
            { compilationStatus = getCompilationStatusSync(context) },
            Executors.newSingleThreadExecutor(),
        )
    }

    LaunchedEffect(compilationStatus) {
        val status = compilationStatus ?: return@LaunchedEffect
        val code = status.profileInstallResultCode
        val codeText = profileInstallResultCodeToString(code)
        compilationStatusText = if (status.isCompiledWithProfile) {
            showManualCompilation = false
            manualCompilationEnabled = false
            "✔ $codeText"
        } else if (status.hasProfileEnqueuedForCompilation()) {
            showManualCompilation = true
            manualCompilationEnabled = true
            "⌛ $codeText"
        } else {
            showManualCompilation = false
            manualCompilationEnabled = false
            codeText
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(state = rememberScrollState()),
    ) {
        Text(text = "Compilation status: $compilationStatusText")

        if (showManualCompilation) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { compileNow() },
                enabled = manualCompilationEnabled,
            ) {
                Text("Compile now (root required)")
            }

            val compileRet = compileResult
            if (compileRet != null) {
                Text(
                    text = compileRet.output,
                    color = if (compileRet.isSuccess) {
                        MaterialTheme.colors.pass
                    } else {
                        MaterialTheme.colors.error
                    },
                )
            }
        }
    }
}

private fun compileBaselineProfile(packageName: String): CompileResult {
    return try {
        val cmd = "cmd package compile -f -m speed-profile $packageName"
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        process.waitFor()
        val error = process.errorStream.bufferedReader().readText().trim()
        if (process.exitValue() == 0 && error.isEmpty()) {
            val output = process.inputStream.bufferedReader().readText().trim()
            CompileResult(isSuccess = true, output = output)
        } else {
            CompileResult(isSuccess = false, output = error)
        }
    } catch (e: Exception) {
        CompileResult(isSuccess = false, output = e.message ?: "Unknown error")
    }
}

@Immutable
private data class CompileResult(
    val isSuccess: Boolean,
    val output: String,
)

private val writeProfileVerification by lazy {
    ProfileVerifier::class.java.getDeclaredMethod(
        "writeProfileVerification",
        Context::class.java,
        Boolean::class.java,
    ).also {
        it.isAccessible = true
    }
}

private fun getCompilationStatusSync(context: Context): CompilationStatus {
    return writeProfileVerification.invoke(null, context, true) as CompilationStatus
}

private fun profileInstallResultCodeToString(code: Int): String = when (code) {
    CompilationStatus.RESULT_CODE_NO_PROFILE -> {
        "NO_PROFILE"
    }

    CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE -> {
        "COMPILED_WITH_PROFILE"
    }

    CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING -> {
        "COMPILED_WITH_PROFILE_NON_MATCHING"
    }

    CompilationStatus.RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION -> {
        "PROFILE_ENQUEUED_FOR_COMPILATION"
    }

    CompilationStatus.RESULT_CODE_ERROR_CACHE_FILE_EXISTS_BUT_CANNOT_BE_READ -> {
        "ERROR_CACHE_FILE_EXISTS_BUT_CANNOT_BE_READ"
    }

    CompilationStatus.RESULT_CODE_ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE -> {
        "ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE"
    }

    CompilationStatus.RESULT_CODE_ERROR_PACKAGE_NAME_DOES_NOT_EXIST -> {
        "ERROR_PACKAGE_NAME_DOES_NOT_EXIST"
    }

    CompilationStatus.RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION -> {
        "ERROR_PACKAGE_NAME_DOES_NOT_EXIST"
    }

    else -> "UNKNOWN"
}