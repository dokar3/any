package any.ui.barcode.camera.scope

interface StartCameraScope {
    fun onOpened(action: CameraOpenedScope.() -> Unit)

    fun onDisconnect(action: () -> Unit)

    fun onError(action: (errorCode: Int) -> Unit)
}
