package com.nordairemapper.service

/**
 * True while the Key setup screen is visible. Gates [RemapEngine] so learning
 * does not fire assigned actions (or consume keys) mid-setup.
 *
 * Token-based begin/end: two overlapping VM instances (navigation recreate)
 * can't have a stale end() from instance #1 disable the gate set by #2.
 */
object LearningMode {

    @Volatile
    private var owner: Any? = null

    val active: Boolean get() = owner != null

    fun begin(ownerToken: Any) {
        synchronized(this) { owner = ownerToken }
    }

    fun end(ownerToken: Any) {
        synchronized(this) {
            if (owner === ownerToken) owner = null
        }
    }
}
