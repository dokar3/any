package any.data.js

import java.util.TreeMap

fun String.escape(): String {
    return replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\b", "\\b")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\'", "\\'")
        .replace("\"", "\\\"")
}

@JvmInline
value class JsObject(val code: String) {
    class Builder {
        private val fields = TreeMap<String, String>()

        fun addBoolField(name: String, value: Boolean?): Builder {
            checkFieldName(name)
            fields[name] = value.toString()
            return this
        }

        fun addNumberField(name: String, value: Number?): Builder {
            checkFieldName(name)
            fields[name] = value.toString()
            return this
        }

        fun addStringField(name: String, value: String?): Builder {
            checkFieldName(name)
            fields[name] = if (value != null) {
                "\"${value.escape()}\""
            } else {
                "null"
            }
            return this
        }

        fun addObjectField(name: String, value: JsObject?): Builder {
            checkFieldName(name)
            fields[name] = value?.code ?: ""
            return this
        }

        fun addArrayField(name: String, value: Array<Any?>?): Builder {
            checkFieldName(name)
            fields[name] = if (value != null) {
                buildString {
                    append('[')
                    for ((index, item) in value.withIndex()) {
                        append(
                            when (item) {
                                null -> "null"
                                is Boolean -> item.toString()
                                is Number -> item.toString()
                                is String -> "\"${item.escape()}\""
                                is JsObject -> item.code
                                else -> throw IllegalArgumentException(
                                    "Unsupported array element: [$index] = $item"
                                )
                            }
                        )
                        if (index != value.lastIndex) {
                            append(',')
                        }
                    }
                    append(']')
                }
            } else {
                "null"
            }
            return this
        }

        private fun checkFieldName(name: String) {
            require(name.isNotEmpty()) { "The field name cannot be empty" }
            require(!name.contains('"')) { "The field name must not contain (\"): $name" }
        }

        fun build(): JsObject {
            val code = buildString {
                append('{')
                for ((name, value) in fields) {
                    append('"')
                    append(name)
                    append('"')
                    append(':')
                    append(value)
                    append(',')
                }
                append('}')
            }
            return JsObject(code)
        }
    }

    companion object {
        val Null = JsObject("null")

        val Undefined = JsObject("undefined")
    }
}