package com.nordairemapper.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nordairemapper.domain.model.ThemeMode
import com.nordairemapper.ui.components.NordGhostButton
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.StatusChip
import com.nordairemapper.ui.components.StatusTone

private val GroupShape = RoundedCornerShape(16.dp)
private val SegmentTrackShape = RoundedCornerShape(12.dp)
private val SegmentBtnShape = RoundedCornerShape(9.dp)

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = GroupShape,
        content = content,
    )
}

/** @deprecated Use [SettingsGroup]. Kept as a thin alias during migration. */
@Composable
fun SettingsHubGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsGroup(modifier = modifier, content = content)
}

/** @deprecated Use [SettingsGroup]. */
@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsGroup(modifier = modifier, content = content)
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
}

/** @deprecated Use [SettingsDivider]. */
@Composable
fun SettingsHubDivider() = SettingsDivider()

@Composable
fun SettingsNavRow(
    title: String,
    subtitle: String = "",
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    status: @Composable (() -> Unit)? = null,
    subtitleContent: @Composable (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
) {
    val base = Modifier.fillMaxWidth()
    val clickable = if (onClick != null) base.clickable(onClick = onClick) else base
    Row(
        modifier = clickable.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.1).sp,
                    ),
                )
                status?.invoke()
            }
            when {
                subtitleContent != null -> subtitleContent()
                subtitle.isNotBlank() -> Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Compatibility wrapper for older hub call sites that passed accent wells.
 * Ignores decorative colors — icons stay monochrome.
 */
@Composable
fun SettingsHubRow(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    accentContainer: Color = Color.Transparent,
    accentTint: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    titleTrailing: @Composable (() -> Unit)? = null,
    subtitleContent: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedAccents = accentContainer to accentTint
    SettingsNavRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        icon = icon,
        status = titleTrailing,
        subtitleContent = subtitleContent,
        showChevron = onClick != null && trailing == null,
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.toggleable(
                        value = checked,
                        role = Role.Switch,
                        onValueChange = onCheckedChange,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                },
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.5f,
                    ),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

/** @deprecated Use [SettingsToggleRow]. */
@Composable
fun SettingsHubToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) = SettingsToggleRow(title, subtitle, checked, onCheckedChange)

data class SettingsSegmentOption(
    val key: String,
    val label: String,
)

@Composable
fun SettingsSegmentedControl(
    options: List<SettingsSegmentOption>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, SegmentTrackShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, SegmentTrackShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val selected = selectedKey == option.key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(SegmentBtnShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option.key) },
                    )
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
fun ThemeSegmentBlock(
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = "Dark, light, or match the system",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
        )
        SettingsSegmentedControl(
            options = ThemeMode.entries.map { mode ->
                SettingsSegmentOption(
                    key = mode.name,
                    label = when (mode) {
                        ThemeMode.DARK -> "Dark"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.SYSTEM -> "System"
                    },
                )
            },
            selectedKey = selectedMode.name,
            onSelect = { key ->
                runCatching { ThemeMode.valueOf(key) }.getOrNull()?.let(onSelect)
            },
        )
    }
}

enum class SettingsStatusTone { Ok, Warn, Muted }

fun SettingsStatusTone.toStatusTone(): StatusTone = when (this) {
    SettingsStatusTone.Ok -> StatusTone.Active
    SettingsStatusTone.Warn -> StatusTone.Warning
    SettingsStatusTone.Muted -> StatusTone.Inactive
}

@Composable
fun SettingsStatusChip(
    label: String,
    tone: SettingsStatusTone,
) {
    StatusChip(
        label = label,
        tone = tone.toStatusTone(),
        showDot = tone != SettingsStatusTone.Muted,
    )
}

@Composable
fun PermissionStatusRow(
    title: String,
    subtitle: String,
    statusLabel: String,
    tone: SettingsStatusTone,
    onClick: () -> Unit,
) {
    SettingsNavRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        status = { SettingsStatusChip(label = statusLabel, tone = tone) },
    )
}

@Composable
fun BatteryOptimizationBlock(
    exempt: Boolean,
    icon: ImageVector,
    onCta: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Battery optimization",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    SettingsStatusChip(
                        label = if (exempt) "Exempt" else "At risk",
                        tone = if (exempt) SettingsStatusTone.Ok else SettingsStatusTone.Warn,
                    )
                }
                Text(
                    text = if (exempt) {
                        "Detection can keep running in the background"
                    } else {
                        "OxygenOS may kill detection in the background"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (exempt) {
            NordGhostButton(text = "Open battery settings", onClick = onCta)
        } else {
            NordPrimaryButton(text = "Exempt from battery optimization", onClick = onCta)
        }
    }
}

@Composable
fun ExclusionsEmptyPanel(
    icon: ImageVector,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "No apps excluded",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = "Remapping stays on in every app. Add apps where the Plus Key should stay stock. Banking apps may still need Accessibility turned off.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        NordGhostButton(text = "Add excluded app", onClick = onAdd)
    }
}

@Composable
fun VersionMeta(versionName: String, buildLabel: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                append(versionName)
            }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                append(" · $buildLabel")
            }
        },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        modifier = Modifier.padding(top = 2.dp),
    )
}
