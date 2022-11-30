package any.base.result

sealed class ValidationResult {
    object Pass : ValidationResult()

    class Fail(val reason: String) : ValidationResult()

    fun failOrNull(): Fail? = this as? Fail
}