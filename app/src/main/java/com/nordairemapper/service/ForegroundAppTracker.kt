package com.nordairemapper.service

import javax.inject.Inject
import javax.inject.Singleton

/** Tracks the current foreground package for per-app remap exclusions. */
@Singleton
class ForegroundAppTracker @Inject constructor() {
    @Volatile
    var packageName: String? = null
        private set

    /**
     * Window-state events include SystemUI surfaces (shade/QS pulled over an
     * app would otherwise flip the tracker away from the real foreground app).
     * Approximation remains: keyboard windows etc. still update this value.
     */
    fun onWindowStateChanged(pkg: String?) {
        if (pkg == null || pkg in SYSTEM_UI_PACKAGES) return
        packageName = pkg
    }

    companion object {
        private val SYSTEM_UI_PACKAGES = setOf(
            "com.android.systemui",
            "android",
        )
    }
}
