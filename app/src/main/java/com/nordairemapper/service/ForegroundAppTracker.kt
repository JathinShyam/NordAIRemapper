package com.nordairemapper.service

import javax.inject.Inject
import javax.inject.Singleton

/** Tracks the current foreground package for per-app remap exclusions. */
@Singleton
class ForegroundAppTracker @Inject constructor() {
    @Volatile
    var packageName: String? = null
}
