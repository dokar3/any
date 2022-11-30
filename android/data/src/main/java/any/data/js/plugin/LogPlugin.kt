package any.data.js.plugin

interface LogPlugin {
    /**
     * Print log message.
     *
     * @param message Message to print.
     */
    fun log(message: String)

    /**
     * Print log message.
     *
     * @param tag Log tag.
     * @param message Message to print.
     */
    fun logWithTag(tag: String, message: String)

    /**
     * Print info message.
     *
     * @param message Message to print.
     */
    fun info(message: String)

    /**
     * Print info message.
     *
     * @param tag Log tag.
     * @param message Message to print.
     */
    fun infoWithTag(tag: String, message: String)


    /**
     * Print warning message.
     *
     * @param message Message to print.
     */
    fun warn(message: String)

    /**
     * Print warning message.
     *
     * @param tag Log tag.
     * @param message Message to print.
     */
    fun warnWithTag(tag: String, message: String)


    /**
     * Print error message.
     *
     * @param message Message to print.
     */
    fun error(message: String)

    /**
     * Print error message.
     *
     * @param tag Log tag.
     * @param message Message to print.
     */
    fun errorWithTag(tag: String, message: String)
}
