package com.nordairemapper.domain.model

enum class PressType(val key: String, val label: String) {
    SINGLE("single", "Single Press"),
    DOUBLE("double", "Double Press"),
    LONG("long", "Long Press");

    companion object {
        fun fromKey(key: String): PressType = entries.first { it.key == key }
    }
}
