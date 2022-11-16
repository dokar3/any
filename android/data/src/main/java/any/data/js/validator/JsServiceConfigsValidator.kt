package any.data.js.validator

import any.base.result.ValidationResult
import any.data.Json
import any.data.entity.ServiceConfig
import any.data.entity.ServiceManifest
import any.data.js.ServiceRunner
import any.data.js.plugin.MemoryServiceConfigsUpdater
import any.data.js.plugin.MemoryServiceManifestUpdater
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
        return serviceRunner.run(
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
            @Language("JS")
            val checkCode = """
                const feature = service.getFeature(ConfigFeature);
                typeof feature.validateConfigs === 'function'
            """.trimIndent()
            val hasValidator = evaluate(checkCode, Boolean::class.java) == true

            if (!hasValidator) {
                return@run configs.map { ValidationResult.Pass }
            }

            @Language("JS")
            val validateCode = """
                const feature = service.getFeature(ConfigFeature);
                const ret = feature.validateConfigs();
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
                val ret = evaluate(validateCode, String::class.java)
                if (ret.isNullOrEmpty()) {
                    return@run List(configs.size) { ValidationResult.Pass }
                }
                json.fromJson<List<ValidationFailure>>(
                    json = ret,
                    type = Json.parameterizedType<List<ValidationFailure>>(),
                )
            } catch (e: Exception) {
                e.printStackTrace()
                val err = "Validation error: ${e.message}"
                return@run List(configs.size) { ValidationResult.Fail(err) }
            }

            if (failures.isNullOrEmpty()) {
                return@run List(configs.size) { ValidationResult.Pass }
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
    }

    @JsonClass(generateAdapter = true)
    data class ValidationFailure(
        val key: String,
        val reason: String,
    )
}