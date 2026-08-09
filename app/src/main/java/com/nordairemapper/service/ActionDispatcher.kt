package com.nordairemapper.service

import com.nordairemapper.domain.model.RemapAction

/** Executes a resolved RemapAction. Implemented by RemapActionExecutor. */
interface ActionDispatcher {
    suspend fun execute(action: RemapAction)
}
