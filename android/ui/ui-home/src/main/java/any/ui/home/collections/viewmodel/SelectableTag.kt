package any.ui.home.collections.viewmodel

data class SelectableTag(
    val name: String,
    val count: Int = 0,
    val isSelected: Boolean = false,
    val isAll: Boolean = false,
) {
    override fun toString(): String {
        return "$name-$isSelected"
    }
}