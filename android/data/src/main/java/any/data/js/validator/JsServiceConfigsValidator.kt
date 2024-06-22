package any.data.js.validator

import any.base.result.ValidationResult
import any.data.entity.ServiceConfig
import any.data.entity.ServiceManifest
import any.data.js.ServiceRunner
import any.data.js.engine.evaluate
import any.data.js.plugin.MemoryServiceConfigsUpdater
import any.data.js.plugin.MemoryServiceManifestUpdater
import any.data.json.Json
import any.data.json.fromJson
import com.squareup.moshi.JsonClass
import org.intellij.lang.annotations.Language

class JsServiceConfigsValidator(
    private val serviceRunner: ServiceRunner,
    private val service: ServiceManifest,
    private val json: Json = Json,
) : ServiceConfigsValidator {
    private var latest = service

    fun getUpdatedService(): ServiceManifest {
        return latest
    }

    override suspend fun validate(configs: List<ServiceConfig>): List<ValidationResult> {
        val updatedConfigs = (service.configs ?: emptyList())
            .associateBy { it.key }
            .toMutableMap()
        for (config in configs) {
            updatedConfigs[config.key] = config
        }
        val result = serviceRunner.runSafely(
            service = service.copy(configs = updatedConfigs.values.toList()),
            manifestUpdater = MemoryServiceManifestUpdater(
                latest = { latest },
                update = { latest = it },
            ),
            configsUpdater = MemoryServiceConfigsUpdater(
                latest = { latest },
                update = { latest = it },
            ),
        ) {
            val checkCode = """
                const feature = service.features.config;
                feature != null && typeof feature.validate === 'function'
            """.trimIndent()
            val hasValidator = evaluate<Boolean?>(checkCode) == true

            if (!hasValidator) {
                return@runSafely configs.map { ValidationResult.Pass }
            }

            @Language("JS")
            val validateCode = """
                const ret = service.features.config.validate();
                var failures = null;
                if (ret) {
                    if (Array.isArray(ret)) {
                        failures = ret;
                    } else {
                        failures = [ret];
                    }
                }
                JSON.stringify(failures)
            """.trimIndent()

            val failures = try {
                val ret = evaluate<String>(validateCode)
                if (ret.isNullOrEmpty()) {
                    return@runSafely List(configs.size) { ValidationResult.Pass }
                }
                json.fromJson<List<ValidationFailure>>(ret)
            } catch (e: Exception) {
                e.printStackTrace()
                val err = "Validation error: ${e.message}"
                return@runSafely List(configs.size) { ValidationResult.Fail(err) }
            }

            if (failures.isNullOrEmpty()) {
                return@runSafely List(configs.size) { ValidationResult.Pass }
            }

            val failureMap = failures.associateBy { it.key }

            configs.map {
                val failure = failureMap[it.key]
                if (failure != null) {
                    ValidationResult.Fail(failure.reason)
                } else {
                    ValidationResult.Pass
                }
            }
        }
        return if (result.isFailure) {
            val errorMessage = result.exceptionOrNull()?.message
                ?: "Error occurred when executing the js code"
            List(configs.size) { ValidationResult.Fail(errorMessage) }
        } else {
            result.getOrThrow()
        }
    }

    @JsonClass(generateAdapter = true)
    data class ValidationFailure(
        val key: String,
        val reason: String,
    )
}