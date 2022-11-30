package any.data.js.plugin

interface ProgressPlugin {
    /**
     * Update loading progress
     *
     * @param progress Current progress, range: [0.0, 1.0].
     * @param message Optional message to describe the current progress.
     */
    fun update(progress: Double, message: String?)

    object Noop : ProgressPlugin {
        override fun update(progress: Double, message: String?) {}
    }
}