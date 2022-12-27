package any.data.js.validator

import any.base.R
import any.base.Strings
import any.base.result.ValidationResult
import any.base.util.isHttpUrl
import any.data.entity.ServiceConfig

class BasicServiceConfigsValidator(private val strings: Strings) : ServiceConfigsValidator {
    override suspend fun validate(
        configs: List<ServiceConfig>,
    ): List<ValidationResult> = configs.map(::validConfig)

    private fun validConfig(config: ServiceConfig): ValidationResult {
        when (config) {
            is ServiceConfig.Bool -> {
                if (config.required && config.value == null) {
                    return ValidationResult.Fail(strings(R.string.cannot_be_empty))
                }
            }

            is ServiceConfig.Number -> {
                if (config.required && config.value == null) {
                    return ValidationResult.Fail(strings(R.string.cannot_be_empty))
                }
            }

            is ServiceConfig.Option -> {
                if (config.required && config.value.isNullOrEmpty()) {
                    return ValidationResult.Fail(strings(R.string.cannot_be_empty))
                }
                if (config.value != null) {
                    val index = config.options.indexOfFirst { it.value == config.value }
                    if (index == -1) {
                        return ValidationResult.Fail(strings(R.string.this_value_is_not_allowed))
                    }
                }
            }

            is ServiceConfig.Text -> {
                if (config.required && config.value.isNullOrEmpty()) {
                    return ValidationResult.Fail(strings(R.string.cannot_be_empty))
                }
            }

            is ServiceConfig.Url -> {
                if (config.required && config.value.isNullOrEmpty()) {
                    return ValidationResult.Fail(strings(R.string.cannot_be_empty))
                }
                if (config.value?.isHttpUrl() == false) {
                    return ValidationResult.Fail(strings(R.string.not_a_valid_http_url))
                }
            }

            is ServiceConfig.Cookies -> {
                if (config.required && config.value.isNullOrEmpty()) {
                    return ValidationResult.Fail(strings(R.string.no_cookies_obtained))
                }
            }

            is ServiceConfig.CookiesUa -> {
                if (config.required && config.value == null) {
                    return ValidationResult.Fail(strings(R.string.no_cookies_obtained))
                }
            }
        }

        return ValidationResult.Pass
    }
}