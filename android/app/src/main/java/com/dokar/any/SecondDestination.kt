package com.dokar.any

enum class SecondDestination(val value: String) {
    Search("search"),
    Download("download");

    companion object {
        fun fromValue(value: String): SecondDestination {
            return checkNotNull(values().find { it.value == value })
        }
    }
}