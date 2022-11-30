package any.richtext

import androidx.compose.ui.text.AnnotatedString

fun interface FromStringAnnotation {
    fun fromStringAnnotation(annotation: AnnotatedString.Range<String>): RichStringMark?
}

sealed class RichStringMark(
    /** inclusive */
    val start: Int,
    /** exclusive */
    val end: Int,
) {
    open val additionalText: String = ""

    init {
        require(end > start) { "Mark end ($end) must larger than start ($start)" }
    }

    abstract fun toStringAnnotation(): AnnotatedString.Range<String>

    companion object {
        private val factories = mutableMapOf<Class<out RichStringMark>, FromStringAnnotation>()

        init {
            factories[ImageMark::class.java] = FromStringAnnotation {
                ImageMark.fromStringAnnotation(it)
            }
            factories[LinkMark::class.java] = FromStringAnnotation {
                LinkMark.fromStringAnnotation(it)
            }
            factories[InlineCodeBlockMark::class.java] = FromStringAnnotation {
                InlineCodeBlockMark.fromStringAnnotation(it)
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : RichStringMark> fromStringAnnotation(
            clazz: Class<out T>,
            annotation: AnnotatedString.Range<String>
        ): T? {
            val factory = factories[clazz]
            if (factory != null) {
                return factory.fromStringAnnotation(annotation) as T?
            }
            for ((clz, f) in factories.entries) {
                if (clazz.isAssignableFrom(clz)) {
                    val mark = f.fromStringAnnotation(annotation)
                    if (mark != null) {
                        return mark as T
                    }
                }
            }
            return null
        }
    }
}

class ImageMark(
    val url: String,
    index: Int
) : RichStringMark(index, index + ADDITIONAL_TEXT.length) {
    override val additionalText: String = ADDITIONAL_TEXT

    override fun toStringAnnotation(): AnnotatedString.Range<String> {
        return AnnotatedString.Range(item = url, start, end, TAG)
    }

    companion object : FromStringAnnotation {
        private const val TAG = "RICH_STRING_MARK_IMAGE"

        private const val ADDITIONAL_TEXT = " "

        override fun fromStringAnnotation(
            annotation: AnnotatedString.Range<String>,
        ): RichStringMark? {
            if (annotation.tag != TAG) {
                return null
            }
            return ImageMark(annotation.item, annotation.start)
        }
    }
}

class LinkMark(val url: String, start: Int, end: Int) : RichStringMark(start, end) {
    override fun toStringAnnotation(): AnnotatedString.Range<String> {
        return AnnotatedString.Range(item = url, start, end, TAG)
    }

    companion object : FromStringAnnotation {
        private const val TAG = "RICH_STRING_MARK_LINK"

        override fun fromStringAnnotation(
            annotation: AnnotatedString.Range<String>,
        ): RichStringMark? {
            if (annotation.tag != TAG) {
                return null
            }
            return LinkMark(annotation.item, annotation.start, annotation.end)
        }
    }
}

class InlineCodeBlockMark(start: Int, end: Int) : RichStringMark(start, end) {
    override fun toStringAnnotation(): AnnotatedString.Range<String> {
        return AnnotatedString.Range(item = "", start, end, TAG)
    }

    companion object : FromStringAnnotation {
        private const val TAG = "RICH_STRING_MARK_INLINE_CODE_BLOCK"

        override fun fromStringAnnotation(
            annotation: AnnotatedString.Range<String>
        ): RichStringMark? {
            return if (annotation.tag == TAG) {
                InlineCodeBlockMark(annotation.start, annotation.end)
            } else {
                null
            }
        }
    }
}