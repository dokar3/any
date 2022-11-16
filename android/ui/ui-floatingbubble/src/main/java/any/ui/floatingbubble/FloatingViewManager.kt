package any.ui.floatingbubble

import java.lang.ref.WeakReference

object FloatingViewManager {
    private val views = mutableListOf<WeakReference<FloatingView>>()

    fun views(): List<FloatingView> {
        return views.mapNotNull { it.get() }
    }

    fun show(view: FloatingView) {
        dismissAll()
        views.add(WeakReference(view))
        view.show()
    }

    fun dismiss(view: FloatingView) {
        if (views.isEmpty()) return
        val itr = views.iterator()
        do {
            if (itr.next().get() == view) {
                if (!view.isDismissed()) {
                    view.dismiss()
                }
                itr.remove()
                return
            }
        } while (itr.hasNext());
    }

    fun dismissAll() {
        for (view in views) {
            view.get()?.dismiss()
        }
        views.clear()
    }
}