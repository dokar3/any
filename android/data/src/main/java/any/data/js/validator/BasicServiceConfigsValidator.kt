package any.data.js.validator

import any.base.result.ValidationResult
import any.base.util.isHttpUrl
import any.data.entity.ServiceConfig
import any.data.entity.value

object BasicServiceConfigsValidator : ServiceConfigsValidator {
    override suspend fun validate(
        configs: List<ServiceConfig>,
    ): List<ValidationResult> = configs.map(::validConfig)

    private fun validConfig(config: ServiceConfig): ValidationResult {
        if (config.required && config.value == null) {
            return ValidationResult.Fail("Cannot be empty")
        }

        when (config) {
            is ServiceConfig.Bool -> {}

            is ServiceConfig.Number -> {}

            is ServiceConfig.Option -> {
                if (config.required && config.value.isNullOrEmpty()) {
                    return ValidationResult.Fail("Cannot be empty")
                }
                if (config.value != null) {
                    val index = config.options.indexOfFirst { it.value == config.value }
                    if (index == -1) {
                        return ValidationResult.Fail("The value is not allowed")
                    }
                }
            }

            is ServiceConfig.Text -> {
                if (config.required && config.value.isNullOrEmpty()) {
                    return ValidationResult.Fail("Cannot be empty")
                }
            }

            is ServiceConfig.Url -> {
                if (config.required && config.value.isNullOrEmpty()) {
                    return ValidationResult.Fail("Cannot be empty")
                }
                if (config.value?.isHttpUrl() == false) {
                    return ValidationResult.Fail("Not a valid http url")
                }
            }

            is ServiceConfig.Cookies -> {
                if (config.required && config.value.isNullOrEmpty()) {
                    return ValidationResult.Fail("Cannot be empty")
                }
            }

            is ServiceConfig.CookiesUa -> {}
        }

        return ValidationResult.Pass
    }
}