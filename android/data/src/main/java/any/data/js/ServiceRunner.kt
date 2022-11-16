package any.data.js

import android.content.Context
import any.data.entity.ServiceManifest
import any.data.entity.ServiceResource
import any.data.js.code.ServiceCodeLoaderImpl
import any.data.js.engine.JsEngine
import any.data.js.engine.JsEngineFactories
import any.data.js.plugin.DefaultHttpPlugin
import any.data.js.plugin.DefaultLogPlugin
import any.data.js.plugin.DomPlugin
import any.data.js.plugin.HttpPlugin
import any.data.js.plugin.JsoupDomPlugin
import any.data.js.plugin.LogPlugin
import any.data.js.plugin.ServiceConfigsUpdater
import any.data.js.plugin.ServiceManifestUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import java.util.Locale

class ServiceRunner(
    private val codeLoader: ServiceCodeLoaderImpl,
    private val httpPlugin: HttpPlugin,
    private val domPlugin: DomPlugin,
    private val logPlugin: LogPlugin,
    private val jsEngineFactory: JsEngine.Factory,
) {
    suspend fun <R> run(
        service: ServiceManifest,
        manifestUpdater: ServiceManifestUpdater,
        configsUpdater: ServiceConfigsUpdater,
        globalServiceName: String = "service",
        block: suspend JsEngine.() -> R
    ): R = withContext(Dispatchers.Default) {
        val jsEngine = jsEngineFactory.create()
        val deferred = async {
            jsEngine.set("__ANY_HTTP_PLUGIN__", HttpPlugin::class.java, httpPlugin)
            jsEngine.set("__ANY_DOM_PLUGIN__", DomPlugin::class.java, domPlugin)
            jsEngine.set("__ANY_LOG_PLUGIN__", LogPlugin::class.java, logPlugin)
            jsEngine.set(
                "__ANY_MANIFEST_UPDATER__",
                ServiceManifestUpdater::class.java,
                manifestUpdater
            )
            jsEngine.set(
                "__ANY_CONFIGS_UPDATER__",
                ServiceConfigsUpdater::class.java,
                configsUpdater
            )

            // Env
            @Language("JS")
            val envSetup = """
                const __ANY_ENV__ = {
                    LANGUAGE: "${Locale.getDefault().toLanguageTag()}",
                };
            """.trimIndent()
            jsEngine.evaluate(envSetup)

            // Evaluate service code
            val main = codeLoader.load(
                checksums = service.mainChecksums,
                url = service.localFirstResourcePath(
                    type = ServiceResource.Type.Main,
                    fallback = { service.main }
                ),
            )
            jsEngine.evaluate(main)

            val manifest = service.toJsManifestObject()
            val configs = service.configs.toJsObject()
            // Create global service object
            jsEngine.evaluate("const $globalServiceName = createService(${manifest},${configs});")

            try {
                block(jsEngine)
            } catch (e: Exception) {
                logPlugin.error(e.message ?: "Unknown error occurred while executing js")
                throw e
            }
        }

        deferred.invokeOnCompletion {
            domPlugin.clear()
            jsEngine.close()
        }

        deferred.await()
    }

    companion object {
        @Volatile
        private var instance: ServiceRunner? = null

        fun getDefault(context: Context): ServiceRunner {
            return instance ?: synchronized(ServiceRunner::class) {
                instance ?: ServiceRunner(
                    codeLoader = ServiceCodeLoaderImpl(context),
                    httpPlugin = DefaultHttpPlugin(),
                    domPlugin = JsoupDomPlugin(),
                    logPlugin = DefaultLogPlugin,
                    jsEngineFactory = JsEngineFactories.getDefault(),
                ).also {
                    instance = it
                }
            }
        }
    }
}