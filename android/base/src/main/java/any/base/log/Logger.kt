package any.base.log

interface Logger {
    fun d(tag: String, message: String)

    fun i(tag: String, message: String)

    fun w(tag: String, message: String)

    fun e(tag: String, message: String)

    companion object : Logger {
        var logger: Logger = AndroidLogger

        override fun d(tag: String, message: String) {
            logger.d(tag, message)
        }

        override fun i(tag: String, message: String) {
            logger.i(tag, message)
        }

        override fun w(tag: String, message: String) {
            logger.w(tag, message)
        }

        override fun e(tag: String, message: String) {
            logger.e(tag, message)
        }
    }
}