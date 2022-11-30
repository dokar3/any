package com.dokar.any

enum class ShortcutsDestination(val value: String) {
    Search("search"),
    Download("download");

    companion object {
        fun fromValue(value: String): ShortcutsDestination {
            return checkNotNull(values().find { it.value == value })
        }
    }
}