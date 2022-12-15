package any.data.js.validator

import any.base.result.ValidationResult
import any.data.entity.ServiceConfig
import any.data.entity.ServiceConfigType
import any.data.entity.ServiceConfigValue
import any.data.entity.isNullOrEmpty

object BasicServiceConfigsValidator : ServiceConfigsValidator {
    override suspend fun validate(
        configs: List<ServiceConfig>,
    ): List<ValidationResult> = configs.map(::validConfig)

    private fun validConfig(config: ServiceConfig): ValidationResult {
        val value = config.value
        return when {
            value.isNullOrEmpty() && config.required -> {
                when (config.type) {
                    ServiceConfigType.Cookies,
                    ServiceConfigType.CookiesAndUserAgent -> {
                        ValidationResult.Fail("No cookies found, please try again")
                    }

                    else -> {
                        ValidationResult.Fail("Cannot be empty")
                    }
                }
            }

            config.type == ServiceConfigType.Bool &&
                    (config.value as? ServiceConfigValue.Double)?.inner == null -> {
                ValidationResult.Fail("Invalid value, only 'true' and 'false' are allowed")
            }

            config.type == ServiceConfigType.Number &&
                    (config.value as? ServiceConfigValue.Double)?.inner == null -> {
                ValidationResult.Fail("Not a valid number")
            }

            config.type == ServiceConfigType.Url && value is ServiceConfigValue.String -> {
                if (value.inner.startsWith("http://") || value.inner.startsWith("https://")) {
                    ValidationResult.Pass
                } else {
                    ValidationResult.Fail("Not a valid url")
                }
            }

            config.type == ServiceConfigType.Option && value is ServiceConfigValue.String -> {
                val options = config.options
                if (!options.isNullOrEmpty()) {
                    for (option in options) {
                        if (option.value == value.inner) {
                            return ValidationResult.Pass
                        }
                    }
                    ValidationResult.Fail("Unsupported option value: ${value.inner}")
                } else {
                    ValidationResult.Fail("Invalid app, no options are provided")
                }
            }

            else -> {
                ValidationResult.Pass
            }
        }
    }
}