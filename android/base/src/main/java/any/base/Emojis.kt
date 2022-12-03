package any.base

sealed class Emojis(val symbols: List<String>) {
    object Negative : Emojis(
        listOf(
            "\uD83D\uDE41",
            "\uD83D\uDE15",
            "\uD83D\uDE33",
            "\uD83D\uDE44"
        )
    )
}