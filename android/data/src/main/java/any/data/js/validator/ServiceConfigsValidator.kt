package any.data.js.validator

import any.base.result.ValidationResult
import any.data.entity.ServiceConfig

interface ServiceConfigsValidator {
    suspend fun validate(
        configs: List<ServiceConfig>,
    ): List<ValidationResult>
}