package any.ui.jslogger

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import any.ui.floatingbubble.FloatingView
import any.ui.floatingbubble.FloatingViewManager
import java.lang.ref.WeakReference

class FloatingLoggerService : Service() {
    private val binder = IFloatingServiceImpl(this)

    private val floatingView = FloatingView(context = this)

    override fun onCreate() {
        super.onCreate()
        floatingView.setContent(
            arrowColor = { floatingContentArrowColor() },
            bubble = { Bubble() },
            expandedContent = { FloatingLoggerScreen() },
        )
        floatingView.setOnDismissListener {
            unbindAll()
            stopSelf()
        }
        FloatingViewManager.show(floatingView)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        FloatingViewManager.dismiss(floatingView)
    }

    private fun dismiss() {
        floatingView.dismiss()
    }

    private class IFloatingServiceImpl(
        service: FloatingLoggerService
    ) : IFoatingLoggerSerivce.Stub() {
        private val serviceRef = WeakReference(service)

        override fun dismiss() {
            serviceRef.get()?.dismiss()
        }
    }

    companion object {
        private var sService: IFoatingLoggerSerivce? = null

        private val boundContexts = mutableListOf<WeakReference<Context>>()

        private val connection = object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?
            ) {
                sService = service as IFoatingLoggerSerivce
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                sService = null
            }
        }

        fun show(context: Context) {
            val intent = Intent(context, FloatingLoggerService::class.java)
            context.bindService(intent, connection, BIND_AUTO_CREATE)
            boundContexts.add(WeakReference(context))
        }

        fun dismiss() {
            sService?.dismiss()
        }

        fun unbind(context: Context) {
            boundContexts.forEach {
                if (it.get() == context) {
                    context.unbindService(connection)
                    return
                }
            }
        }

        fun unbindAll() {
            sService = null
            boundContexts.forEach {
                try {
                    it.get()?.unbindService(connection)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            boundContexts.clear()
        }
    }
}