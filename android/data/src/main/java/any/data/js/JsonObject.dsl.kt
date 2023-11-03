package any.data.js

/**
 * Build a [JsonObject].
 *
 * The example Kotlin code:
 * ```kotlin
 * json {
 *     "message" eq "Some text"
 *     "year" eq 1990
 *     "enabled" eq false
 *     "name" eq null
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
 *   "inner": {
 *     "value": 1.0,
 *   },
 * }
 * ```
 */
fun json(block: JsonScope.() -> Unit): JsonObject {
    val scope = JsonScopeImpl()
    block(scope)
    return scope.buildObject()
}

interface JsonScope {
    infix fun String.eq(value: Boolean?)

    infix fun String.eq(value: Number?)

    infix fun String.eq(value: String?)

    infix fun String.eq(value: JsonObject?)

    infix fun String.eq(value: Array<Any?>?)

    infix fun String.eq(value: List<Any?>?)

    infix fun String.eq(nullValue: Nothing?)
}

private class JsonScopeImpl : JsonScope {
    private val builder = JsonObject.Builder()

    override fun String.eq(value: Boolean?) {
        builder.addBoolField(this, value)
    }

    override fun String.eq(value: Number?) {
        builder.addNumberField(this, value)
    }

    override fun String.eq(value: String?) {
        builder.addStringField(this, value)
    }

    override fun String.eq(value: JsonObject?) {
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

    fun buildObject(): JsonObject {
        return builder.build()
    }
}