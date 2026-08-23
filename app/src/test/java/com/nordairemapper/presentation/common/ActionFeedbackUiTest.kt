package com.nordairemapper.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ScreenLockRotation
import androidx.compose.material.icons.outlined.ScreenRotation
import com.nordairemapper.domain.model.ActionFeedback
import com.nordairemapper.domain.model.ActionFeedbackState
import com.nordairemapper.domain.model.RemapAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionFeedbackUiTest {

    @Test
    fun autoRotate_statesUseDistinctIconsAndCaptions() {
        val on = ActionFeedback(RemapAction.ToggleAutoRotate, ActionFeedbackState.AUTO_ROTATE_ON)
        val off = ActionFeedback(RemapAction.ToggleAutoRotate, ActionFeedbackState.AUTO_ROTATE_OFF)

        assertEquals(Icons.Outlined.ScreenRotation, on.icon())
        assertEquals(Icons.Outlined.ScreenLockRotation, off.icon())
        assertEquals("On", on.caption())
        assertEquals("Off", off.caption())
    }

    @Test
    fun genericAction_hasNoCaption() {
        assertNull(ActionFeedback(RemapAction.TakeScreenshot).caption())
    }
}
