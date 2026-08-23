package com.nordairemapper.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Nord 5 CPH2707: 2800px height @ 560dpi (density 3.5). */
class VisualActionPopupPlacementTest {

    @Test
    fun nord5_anchor_is_raised_above_diagram_center() {
        val screenHeightPx = 2800f
        val density = 3.5f
        val diagramCenterPx = screenHeightPx * VISUAL_OVERLAY_KEY_CENTER_FRACTION
        val anchorPx = computeVisualOverlayAnchorY(screenHeightPx, density)
        val fixedRaisePx = VISUAL_OVERLAY_SCREEN_RAISE.value * density
        val fractionalRaisePx = screenHeightPx * VISUAL_OVERLAY_SCREEN_RAISE_FRACTION
        val nudgeDownPx = VISUAL_OVERLAY_SCREEN_NUDGE_DOWN.value * density
        val netRaisePx = fixedRaisePx + fractionalRaisePx - nudgeDownPx

        assertEquals(diagramCenterPx - netRaisePx, anchorPx, 0.5f)
        // 40dp + 2.5% − 7.5dp nudge (~25px @ 560dpi)
        assertEquals(netRaisePx, diagramCenterPx - anchorPx, 0.5f)
    }

    @Test
    fun deployed_010_anchor_was_only_slightly_above_key_top() {
        val screenHeightPx = 2800f
        val keyTopPx = screenHeightPx * VISUAL_OVERLAY_KEY_TOP_FRACTION
        val oldFraction = VISUAL_OVERLAY_KEY_TOP_FRACTION +
            VISUAL_OVERLAY_KEY_HEIGHT_FRACTION * 0.10f
        val oldCenterPx = screenHeightPx * oldFraction
        assertEquals(15f, oldCenterPx - keyTopPx, 1f)
    }
}
