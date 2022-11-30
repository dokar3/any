package any.data.js.plugin

interface DomPlugin {
    /**
     * Clear elements cache
     */
    fun clear()

    /**
     * Create a document element.
     *
     * @param text Text.
     * @param type Parser type, 'html' or 'xml'. Defaults to 'html'.
     */
    fun create(text: String, type: String): Int

    /**
     * Select all matching children.
     *
     * @param elementId Element id returned by [create], [select] or [selectFirst].
     * @param cssQuery Css selector.
     */
    fun select(elementId: Int, cssQuery: String): Array<Int>

    /**
     * Select first matching child.
     *
     * @param elementId Element id returned by [create], [select] or [selectFirst].
     * @param cssQuery Css selector.
     */
    fun selectFirst(elementId: Int, cssQuery: String): Int

    /**
     * Get element attribute.
     *
     * @param elementId Element id returned by [create], [select] or [selectFirst].
     * @param name Attribute name.
     */
    fun attr(elementId: Int, name: String): String?

    /**
     * Set element attribute.
     *
     * @param elementId Element id returned by [create], [select] or [selectFirst].
     * @param name Attribute name.
     * @param value Attribute value
     */
    fun setAttr(elementId: Int, name: String, value: String)

    /**
     * Get element text.
     *
     * @param elementId Element id returned by [create], [select] or [selectFirst].
     */
    fun text(elementId: Int): String?

    /**
     * Set element text.
     *
     * @param elementId Element id returned by [create], [select] or [selectFirst].
     * @param text Element text
     */
    fun setText(elementId: Int, text: String)

    /**
     * Get element's inner html text.
     *
     * @param elementId Element id returned by [create], [select] or [selectFirst].
     */
    fun html(elementId: Int): String

    /**
     * Set element's inner html text.
     *
     * @param elementId Element id returned by [create], [select] or [selectFirst].
     * @param html Html content
     */
    fun setHtml(elementId: Int, html: String)

    companion object {
        const val NO_ELEMENT = -1
    }
}