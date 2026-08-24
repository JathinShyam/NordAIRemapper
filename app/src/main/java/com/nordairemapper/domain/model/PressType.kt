package com.nordairemapper.domain.model

enum class PressType(val key: String, val label: String) {
    SINGLE("single", "Single Press"),
    DOUBLE("double", "Double Press"),
    LONG("long", "Long Press");

    companion object {
        /** Malformed nav args fall back to SINGLE instead of crashing. */
        fun fromKey(key: String): PressType = entries.firstOrNull { it.key == key } ?: SINGLE
    }
}
