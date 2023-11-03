package any.data.js

/**
 * Build a [JsObject].
 *
 * The example Kotlin code:
 * ```kotlin
 * buildJsObject {
 *     "message" eq "Some text"
 *     "year" eq 1990
 *     "enabled" eq false
 *     "name" eq null
 *     "builtin" eq JsObject.Undefined
 *     "inner" eq buildJsObject {
 *         "value" eq 1.0
 *     }
 * }
 * ```
 * Equals to:
 * ```javascript
 * {
 *   "message": "Some text",
 *   "year": 1990,
 *   "enabled": false,
 *   "name": null,
 *   "builtin": undefined,
 *   "inner": {
 *     "value": 1.0,
 *   },
 * }
 * ```
 */
fun buildJsObject(block: JsObjectScope.() -> Unit): JsObject {
    val scope = JsObjectScopeImpl()
    block(scope)
    return scope.buildObject()
}

interface JsObjectScope {
    infix fun String.eq(value: Boolean?)

    infix fun String.eq(value: Number?)

    infix fun String.eq(value: String?)

    infix fun String.eq(value: JsObject?)

    infix fun String.eq(value: Array<Any?>?)

    infix fun String.eq(value: List<Any?>?)

    infix fun String.eq(nullValue: Nothing?)
}

private class JsObjectScopeImpl : JsObjectScope {
    private val builder = JsObject.Builder()

    override fun String.eq(value: Boolean?) {
        builder.addBoolField(this, value)
    }

    override fun String.eq(value: Number?) {
        builder.addNumberField(this, value)
    }

    override fun String.eq(value: String?) {
        builder.addStringField(this, value)
    }

    override fun String.eq(value: JsObject?) {
        builder.addObjectField(this, value)
    }

    override fun String.eq(value: Array<Any?>?) {
        builder.addArrayField(this, value)
    }

    override fun String.eq(value: List<Any?>?) {
        builder.addArrayField(this, value?.toTypedArray())
    }

    override fun String.eq(nullValue: Nothing?) {
        builder.addObjectField(this, null)
    }

    fun buildObject(): JsObject {
        return builder.build()
    }
}