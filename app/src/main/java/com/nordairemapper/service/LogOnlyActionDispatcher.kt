package com.nordairemapper.service

import android.util.Log
import com.nordairemapper.domain.model.RemapAction
import javax.inject.Inject
import javax.inject.Singleton

/** Placeholder dispatcher until the real action executors land in Phase 5. */
@Singleton
class LogOnlyActionDispatcher @Inject constructor() : ActionDispatcher {
    override suspend fun execute(action: RemapAction) {
        Log.i("ActionDispatcher", "Would execute: $action")
    }
}
