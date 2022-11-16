package any.data.js.validator

import any.base.result.ValidationResult
import any.data.entity.ServiceConfig
import any.data.entity.ServiceConfigType

object BasicServiceConfigsValidator : ServiceConfigsValidator {
    override suspend fun validate(
        configs: List<ServiceConfig>,
    ): List<ValidationResult> {
        return configs.map { config ->
            val value = config.value?.text
            when {
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
                config.type == ServiceConfigType.Bool && config.value?.boolOrNull() == null -> {
                    ValidationResult.Fail("Invalid value, only 'true' and 'false' are allowed")
                }
                config.type == ServiceConfigType.Number && config.value?.doubleOrNull() == null -> {
                    ValidationResult.Fail("Not a valid number")
                }
                config.type == ServiceConfigType.Url && value != null -> {
                    if (value.startsWith("http://") || value.startsWith("https://")) {
                        ValidationResult.Pass
                    } else {
                        ValidationResult.Fail("Not a valid url")
                    }
                }
                config.type == ServiceConfigType.Option && value != null -> {
                    val options = config.options
                    if (!options.isNullOrEmpty()) {
                        for (option in options) {
                            if (option.value == value) {
                                return@map ValidationResult.Pass
                            }
                        }
                        ValidationResult.Fail("Unsupported option value: $value")
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
}