package any.data.js.plugin

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

class JsoupDomPlugin : DomPlugin {
    private val elementMap: MutableMap<Int, Element> = mutableMapOf()

    override fun clear() {
        elementMap.clear()
    }

    override fun create(text: String, type: String): Int {
        val doc = when (type.lowercase()) {
            "xml" -> {
                Jsoup.parse(text, "", Parser.xmlParser())
            }
            else -> {
                Jsoup.parse(text)
            }
        }
        return putElement(doc)
    }

    override fun select(elementId: Int, cssQuery: String): Array<Int> {
        val curr = getElement(elementId)
        val elements = curr.select(cssQuery)
        return elements.map { putElement(it) }.toTypedArray()
    }

    override fun selectFirst(elementId: Int, cssQuery: String): Int {
        val curr = getElement(elementId)
        val element = curr.selectFirst(cssQuery)
        return if (element != null) {
            putElement(element)
        } else {
            DomPlugin.NO_ELEMENT
        }
    }

    override fun attr(elementId: Int, name: String): String? {
        val curr = getElement(elementId)
        return curr.attr(name)
    }

    override fun setAttr(elementId: Int, name: String, value: String) {
        getElement(elementId).attr(name, value)
    }

    override fun text(elementId: Int): String? {
        val curr = getElement(elementId)
        return curr.text()
    }

    override fun setText(elementId: Int, text: String) {
        getElement(elementId).text(text)
    }

    override fun html(elementId: Int): String {
        val curr = getElement(elementId)
        return curr.html()
    }

    override fun setHtml(elementId: Int, html: String) {
        getElement(elementId).html(html)
    }

    private fun putElement(element: Element): Int {
        val key = elementMap.size
        elementMap[key] = element
        return key
    }

    private fun getElement(id: Int): Element {
        val element = elementMap[id]
        requireNotNull(element) {
            "Target element not found, id: $id"
        }
        return element
    }
}