package any.data.js.engine

object JsEngineFactories {
    private val factories = mutableMapOf<String, JsEngine.Factory>()

    init {
        factories[DuktapeJsEngine.NAME] = DuktapeJsEngine.Factory()
    }

    fun getAll(): Map<String, JsEngine.Factory> = factories

    fun getDefault(): JsEngine.Factory {
        return requireNotNull(factories[DuktapeJsEngine.NAME])
    }
}