package com.nordairemapper.domain.model

import com.nordairemapper.presentation.common.RemapActionCatalog
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** TRD §4.1/§5: stable polymorphic JSON for Room columns and backup files. */
class RemapActionSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `every catalog action round-trips`() {
        val extra = listOf(
            RemapAction.LaunchApp("com.example.app", "Example"),
            RemapAction.OpenUrl("https://example.com/page"),
            RemapAction.AdjustMediaVolume(up = false),
            RemapAction.OpenCamera(front = true),
        )
        val actions = RemapActionCatalog.items.map { it.action } + extra
        for (action in actions) {
            val encoded = json.encodeToString(RemapAction.serializer(), action)
            val decoded = json.decodeFromString(RemapAction.serializer(), encoded)
            assertEquals("Round-trip failed for $encoded", action, decoded)
        }
    }

    @Test
    fun `serial names are stable for backup compatibility`() {
        val cases = mapOf(
            RemapAction.None to "\"type\":\"none\"",
            RemapAction.ShowOverlay to "\"type\":\"show_overlay\"",
            RemapAction.TakeScreenshot to "\"type\":\"screenshot\"",
            RemapAction.ToggleFlashlight to "\"type\":\"toggle_flashlight\"",
        )
        for ((action, expectedFragment) in cases) {
            assertTrue(
                "$action missing $expectedFragment",
                json.encodeToString(RemapAction.serializer(), action).contains(expectedFragment),
            )
        }
    }

    @Test
    fun `unknown subtype is rejected rather than silently dropped`() {
        val result = runCatching {
            json.decodeFromString<BackupPayload>(
                """
                {
                  "schemaVersion": 1,
                  "exportedAtEpochMs": 0,
                  "remap": {"single": {"type": "future_action"}},
                  "overlay": {},
                  "settings": {}
                }
                """.trimIndent(),
            )
        }
        assertTrue(result.isFailure)
    }
}
