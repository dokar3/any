package any.base.log

object StdOutLogger : Logger {
    override fun d(tag: String, message: String) {
        println("[D]$tag: $message")
    }

    override fun i(tag: String, message: String) {
        println("[I]$tag: $message")
    }

    override fun w(tag: String, message: String) {
        println("[W]$tag: $message")
    }

    override fun e(tag: String, message: String) {
        println("[E]$tag: $message")
    }
}