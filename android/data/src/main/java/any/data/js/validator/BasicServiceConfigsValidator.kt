package any.data.js.validator

import any.base.result.ValidationResult
import any.data.entity.ServiceConfig
import any.data.entity.ServiceConfigType
import any.data.entity.ServiceConfigValue

object BasicServiceConfigsValidator : ServiceConfigsValidator {
    override suspend fun validate(
        configs: List<ServiceConfig>,
    ): List<ValidationResult> {
        return configs.map { config ->
            val stringValue = config.value?.stringValue
            when {
                stringValue.isNullOrEmpty() && config.required -> {
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
                        (config.value as? ServiceConfigValue.Double)?.value == null -> {
                    ValidationResult.Fail("Invalid value, only 'true' and 'false' are allowed")
                }

                config.type == ServiceConfigType.Number &&
                        (config.value as? ServiceConfigValue.Double)?.value == null -> {
                    ValidationResult.Fail("Not a valid number")
                }

                config.type == ServiceConfigType.Url && stringValue != null -> {
                    if (stringValue.startsWith("http://") || stringValue.startsWith("https://")) {
                        ValidationResult.Pass
                    } else {
                        ValidationResult.Fail("Not a valid url")
                    }
                }

                config.type == ServiceConfigType.Option && stringValue != null -> {
                    val options = config.options
                    if (!options.isNullOrEmpty()) {
                        for (option in options) {
                            if (option.value == stringValue) {
                                return@map ValidationResult.Pass
                            }
                        }
                        ValidationResult.Fail("Unsupported option value: $stringValue")
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