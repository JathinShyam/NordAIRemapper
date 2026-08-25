package com.nordairemapper.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPermissionsTest {

    @Test
    fun hubSummaryLabel_reflectsAttentionCount() {
        assertEquals("All OK", AppPermissions.hubSummaryLabel(0))
        assertEquals("1 needs attention", AppPermissions.hubSummaryLabel(1))
        assertEquals("3 need attention", AppPermissions.hubSummaryLabel(3))
    }

    @Test
    fun hubAttentionCount_ignoresAdvancedSection() {
        val items = listOf(
            AppPermissions.Item(
                id = AppPermissions.Id.ACCESSIBILITY,
                title = "Accessibility Service",
                subtitle = "",
                statusLabel = "Not Enabled",
                isOk = false,
                section = AppPermissions.Section.CORE,
            ),
            AppPermissions.Item(
                id = AppPermissions.Id.USAGE_ACCESS,
                title = "Usage Access",
                subtitle = "",
                statusLabel = "Not Granted",
                isOk = false,
                section = AppPermissions.Section.ADVANCED,
            ),
        )

        assertEquals(1, AppPermissions.hubAttentionCount(items))
    }
}
