package com.nordairemapper.presentation.common

import com.nordairemapper.domain.model.RemapAction

enum class RemapActionCategory(val label: String) {
    APPS("Apps"),
    MEDIA("Media"),
    SYSTEM("System"),
    OVERLAY("Floating Menu"),
    NONE("None"),
}

data class RemapActionItem(
    val category: RemapActionCategory,
    val action: RemapAction,
    /** When true, tapping opens a picker/sheet instead of saving immediately. */
    val needsPicker: Boolean = false,
)

/**
 * Canonical list of assignable actions for Remap Config and Floating Menu settings.
 * Parameterized actions appear as templates (Launch App, URL, camera faces, volume).
 */
object RemapActionCatalog {

    val items: List<RemapActionItem> = listOf(
        RemapActionItem(RemapActionCategory.APPS, RemapAction.LaunchApp("", ""), needsPicker = true),
        RemapActionItem(RemapActionCategory.APPS, RemapAction.OpenUrl(""), needsPicker = true),

        RemapActionItem(RemapActionCategory.MEDIA, RemapAction.PlayPauseMedia),
        RemapActionItem(RemapActionCategory.MEDIA, RemapAction.NextTrack),
        RemapActionItem(RemapActionCategory.MEDIA, RemapAction.PreviousTrack),
        RemapActionItem(RemapActionCategory.MEDIA, RemapAction.AdjustMediaVolume(up = true)),
        RemapActionItem(RemapActionCategory.MEDIA, RemapAction.AdjustMediaVolume(up = false)),

        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.OpenAssistant),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.OpenCamera(front = false)),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.OpenCamera(front = true)),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.ToggleFlashlight),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.TakeScreenshot),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.ToggleDoNotDisturb),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.CycleRingerMode),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.OpenNotificationShade),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.OpenQuickSettings),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.OpenRecents),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.GoHome),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.GoBack),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.LockScreen),
        RemapActionItem(RemapActionCategory.SYSTEM, RemapAction.ToggleAutoRotate),

        RemapActionItem(RemapActionCategory.OVERLAY, RemapAction.ShowOverlay),

        RemapActionItem(RemapActionCategory.NONE, RemapAction.None),
    )

    fun grouped(): Map<RemapActionCategory, List<RemapActionItem>> =
        RemapActionCategory.entries.associateWith { category ->
            items.filter { it.category == category }
        }.filterValues { it.isNotEmpty() }
}
