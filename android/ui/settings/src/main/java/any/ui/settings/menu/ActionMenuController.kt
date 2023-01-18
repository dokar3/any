package any.ui.settings.menu

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.LinkedList

@Stable
internal class ActionMenuController {
    private val items = LinkedList<ActionMenuItem>()

    var currentItem by mutableStateOf<ActionMenuItem?>(null)
        private set

    fun pushItem(item: ActionMenuItem) {
        items.push(item)
        currentItem = item
    }

    fun popItem() {
        items.pop()
        currentItem = items.peek()
    }
}