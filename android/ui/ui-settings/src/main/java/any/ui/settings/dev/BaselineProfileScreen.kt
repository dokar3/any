package any.ui.settings.dev

import any.base.R as BaseR
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.profileinstaller.ProfileVerifier
import any.ui.settings.viewmodel.SettingsViewModel
import java.util.concurrent.Executors

@Composable
internal fun BaselineProfileScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var compilationStatus by remember { mutableStateOf("?") }

    LaunchedEffect(viewModel) {
        viewModel.updateTitle(context.resources.getString(BaseR.string.baseline_profile))
        viewModel.setShowBackArrow(true)
    }

    LaunchedEffect(Unit) {
        ProfileVerifier.getCompilationStatusAsync().let {
            it.addListener(
                {
                    val status = it.get()
                    val code = status.profileInstallResultCode
                    val codeText = profileInstallResultCodeToString(code)
                    compilationStatus = if (status.isCompiledWithProfile) {
                        "✔ $codeText"
                    } else if (status.hasProfileEnqueuedForCompilation()) {
                        "⌛ $codeText"
                    } else {
                        codeText
                    }
                },
                Executors.newSingleThreadExecutor(),
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = "Compilation status: $compilationStatus")
    }
}

private fun profileInstallResultCodeToString(code: Int): String = when (code) {
    ProfileVerifier.CompilationStatus.RESULT_CODE_NO_PROFILE -> {
        "NO_PROFILE"
    }

    ProfileVerifier.CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE -> {
        "COMPILED_WITH_PROFILE"
    }

    ProfileVerifier.CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING -> {
        "COMPILED_WITH_PROFILE_NON_MATCHING"
    }

    ProfileVerifier.CompilationStatus.RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION -> {
        "PROFILE_ENQUEUED_FOR_COMPILATION"
    }

    ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_CACHE_FILE_EXISTS_BUT_CANNOT_BE_READ -> {
        "ERROR_CACHE_FILE_EXISTS_BUT_CANNOT_BE_READ"
    }

    ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE -> {
        "ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE"
    }

    ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_PACKAGE_NAME_DOES_NOT_EXIST -> {
        "ERROR_PACKAGE_NAME_DOES_NOT_EXIST"
    }

    ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION -> {
        "ERROR_PACKAGE_NAME_DOES_NOT_EXIST"
    }

    else -> "UNKNOWN"
}