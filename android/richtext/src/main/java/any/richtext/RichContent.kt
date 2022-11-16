package any.richtext

import androidx.compose.runtime.Immutable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun RichContent?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this == null || this.isEmpty()
}

@JvmInline
@Immutable
value class RichContent(val elements: List<RichElement>) {
    private val string: RichString get() = elements.joinToRichString("\n")

    fun isEmpty(): Boolean {
        return elements.isEmpty()
    }

    fun toRichString(): RichString {
        return string
    }

    override fun toString(): String {
        return string.toString()
    }

    class Builder {
        private val elements = mutableListOf<RichElement>()

        fun addElement(element: RichElement) {
            when (element) {
                is RichElement.Text -> {
                    val (texts, images) = element.text.splitByMarkType<ImageMark>()
                    for (i in 0 until (texts.size + images.size)) {
                        val idx = i / 2
                        if (i % 2 == 0) {
                            val text = texts[idx]
                            if (text.isNotEmpty()) {
                                elements.add(RichElement.Text(text))
                            }
                        } else {
                            elements.add(RichElement.Image(images[idx].url))
                        }
                    }
                }

                else -> {
                    elements.add(element)
                }
            }
        }

        fun build(): RichContent {
            return RichContent(elements = elements)
        }
    }

    companion object {
        val Empty = RichContent(elements = emptyList())
    }
}
