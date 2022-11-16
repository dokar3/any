package any.richtext.markdown

import any.richtext.ImageMark
import any.richtext.InlineCodeBlockMark
import any.richtext.LinkMark
import any.richtext.RichContent
import any.richtext.RichElement
import any.richtext.RichString
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

class DefaultMarkdownParser : MarkdownParser {
    private val parser = Parser.builder().build()

    override fun parse(text: String): RichContent {
        if (text.isEmpty()) {
            return RichContent.Empty
        }

        val contentBuilder = RichContent.Builder()

        val node = parser.parse(text)
        contentBuilder.appendBlockNode(node)

        return contentBuilder.build()
    }

    private fun RichContent.Builder.appendBlockNode(node: Node) {
        when (node) {
            is Document -> {
                node.visitChildren { appendBlockNode(it) }
            }

            is OrderedList -> {
                var order = node.startNumber
                node.visitChildren { child ->
                    if (child is ListItem) {
                        val contentBuilder = RichContent.Builder()
                        child.visitChildren { contentBuilder.appendBlockNode(it) }
                        addElement(RichElement.OrderedListItem(contentBuilder.build(), order++))
                    } else {
                        appendBlockNode(node)
                    }
                }
            }

            is BulletList -> {
                node.visitChildren { appendBlockNode(it) }
            }

            is Heading -> {
                val richString = RichString.Builder()
                node.visitChildren { richString.appendNode(it) }
                addElement(RichElement.Heading(richString.build(), node.level))
            }

            is Paragraph -> {
                val richString = RichString.Builder()
                node.visitChildren { richString.appendNode(it) }
                addElement(RichElement.Text(richString.build()))
            }

            is BlockQuote -> {
                val contentBuilder = RichContent.Builder()
                node.visitChildren { contentBuilder.appendBlockNode(it) }
                addElement(RichElement.BlockQuote(contentBuilder.build()))
            }

            is FencedCodeBlock -> {
                val code = node.literal.removeSuffix("\n")
                addElement(RichElement.CodeBlock(code, node.language))
            }

            is ListItem -> {
                val contentBuilder = RichContent.Builder()
                node.visitChildren { contentBuilder.appendBlockNode(it) }
                addElement(RichElement.UnorderedListItem(contentBuilder.build()))
            }

            is ThematicBreak -> {
                addElement(RichElement.HorizontalRule)
            }
        }
    }

    private fun RichString.Builder.appendNode(node: Node) {
        when (node) {
            is Text -> {
                append(node.literal)
            }

            is Paragraph -> {
                if (isNotEmpty()) {
                    append('\n')
                }
                node.visitChildren { appendNode(it) }
            }

            is Image -> {
                addMark(ImageMark(node.destination, length))
            }

            is Emphasis -> {
                val start = length
                node.visitChildren { appendNode(it) }
                val end = length
                applyItalic(start, end)
            }

            is StrongEmphasis -> {
                val start = length
                node.visitChildren { appendNode(it) }
                val end = length
                applyBold(start, end)
            }

            is HardLineBreak -> {
                append('\n')
            }

            is Link -> {
                val start = length
                node.visitChildren { appendNode(it) }
                val end = length
                addMark(LinkMark(node.destination, start, end))
            }

            is Code -> {
                val start = length
                append(node.literal)
                val end = length
                addMark(InlineCodeBlockMark(start, end))
            }
        }
    }

    private inline fun Node.visitChildren(block: (Node) -> Unit) {
        var node = firstChild
        while (node != null) {
            val next = node.next
            block(node)
            node = next
        }
    }

    private val FencedCodeBlock.language: String?
        get() {
            return if (!info.isNullOrEmpty()) {
                val spaceAt = info.indexOf(' ')
                if (spaceAt != -1) {
                    info.substring(0, spaceAt)
                } else {
                    info
                }
            } else {
                null
            }
        }
}