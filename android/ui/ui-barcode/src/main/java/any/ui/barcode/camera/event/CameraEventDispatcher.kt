package any.ui.barcode.camera.event

internal class CameraEventDispatcher : CameraEventListener {
    private val listeners = mutableListOf<CameraEventListener>()

    fun addListener(listener: CameraEventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CameraEventListener) {
        listeners.remove(listener)
    }

    override fun onEvent(event: CameraEvent) {
        val listeners = listeners.toList()
        for (listener in listeners) {
            listener.onEvent(event)
        }
    }
}