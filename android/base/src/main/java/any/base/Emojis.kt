package any.base

sealed class Emojis(
    val symbols: List<String>
) {
    object Positive : Emojis(
        listOf(
            "\uD83D\uDE01",
            "\uD83D\uDE04",
            "\uD83D\uDE03",
            "\uD83D\uDE42"
        )
    )

    object Negative : Emojis(
        listOf(
            "\uD83D\uDE41",
            "\uD83D\uDE15",
            "\uD83D\uDE33",
            "\uD83D\uDE44"
        )
    )

    object Horny : Emojis(
        listOf(
            "\uD83E\uDD75",
            "\uD83E\uDD24",
            "\uD83D\uDE0D",
            "\uD83D\uDE1B"
        )
    )

    object Funny : Emojis(
        listOf(
            "\uD83E\uDD21",
            "\uD83E\uDD13",
            "\uD83D\uDC79",
            "\uD83E\uDD23"
        )
    )
}