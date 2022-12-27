package any.base.result

sealed class ValidationResult {
    object Pass : ValidationResult() {
        override fun toString(): String {
            return "ValidationResult.Pass()"
        }
    }

    class Fail(val reason: String) : ValidationResult() {
        override fun toString(): String {
            return "ValidationResult.Fail('$reason')"
        }
    }

    fun failOrNull(): Fail? = this as? Fail
}