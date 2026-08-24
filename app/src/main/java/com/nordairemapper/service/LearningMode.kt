package com.nordairemapper.service

/**
 * True while the Key setup screen is visible. Gates [RemapEngine] so learning
 * does not fire assigned actions (or consume keys) mid-setup.
 */
object LearningMode {
    @Volatile
    var active: Boolean = false
}
