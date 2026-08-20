package dev.vicent.veil.ui.components

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.StorageUsagePolicy
import dev.vicent.veil.launcher.model.ConnectionType
import dev.vicent.veil.launcher.model.SystemStatus
import dev.vicent.veil.ui.theme.LocalVeilPalette

private val deviceDashboardTileHeight = 184.dp
private val toolsSecondaryTileHeight = 116.dp
private val settingsTileHeight = 150.dp

@Composable
internal fun ToolsWorkspace(
    systemStatus: SystemStatus,
    compact: Boolean,
    onVeilSettingsSelected: () -> Unit,
    onSettingsSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
        DeviceDashboardTile(systemStatus, onSettingsSelected)
        ResponsivePair(
            compact = compact,
            left = {
                val system = systemStatus
                CozyTile(
                    label = stringResource(R.string.tools_battery),
                    modifier = Modifier.fillMaxWidth().heightIn(min = toolsSecondaryTileHeight),
                ) {
                    TileTitle(
                        system.batteryPercent
                            ?.let { percent ->
                                if (system.isCharging) {
                                    stringResource(R.string.tools_battery_charging, percent)
                                } else {
                                    stringResource(R.string.tools_battery_percent, percent)
                                }
                            }
                            ?: stringResource(R.string.state_unavailable),
                    )
                    TileBody(
                        when {
                            system.batteryPercent == null ->
                                stringResource(R.string.tools_battery_unknown)
                            system.isCharging -> stringResource(R.string.tools_battery_powered)
                            else -> stringResource(R.string.tools_battery_running)
                        },
                    )
                    TileAction(stringResource(R.string.tools_open_battery)) {
                        onSettingsSelected("battery")
                    }
                }
            },
            right = {
                val connectionLabel = systemStatus.connectionType.presentationLabel()
                CozyTile(
                    label = stringResource(R.string.tools_connectivity),
                    modifier = Modifier.fillMaxWidth().heightIn(min = toolsSecondaryTileHeight),
                ) {
                    TileTitle(connectionLabel)
                    TileBody(
                        if (systemStatus.connectionType == ConnectionType.NONE) {
                            stringResource(R.string.tools_no_active_network)
                        } else {
                            stringResource(R.string.tools_active_transport)
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TileAction(stringResource(R.string.tools_networks)) {
                            onSettingsSelected("network")
                        }
                        TileAction(stringResource(R.string.tools_bluetooth)) {
                            onSettingsSelected("bluetooth")
                        }
                    }
                }
            },
        )
        SettingsPanel(onVeilSettingsSelected, onSettingsSelected)
    }
}

@Composable
private fun ConnectionType.presentationLabel(): String = when (this) {
    ConnectionType.NONE -> stringResource(R.string.connection_none)
    ConnectionType.WIFI -> stringResource(R.string.connection_wifi)
    ConnectionType.CELLULAR -> stringResource(R.string.connection_cellular)
    ConnectionType.ETHERNET -> stringResource(R.string.connection_ethernet)
    ConnectionType.OTHER -> stringResource(R.string.connection_other)
}

@Composable
private fun DeviceDashboardTile(
    system: SystemStatus,
    onSettingsSelected: (String) -> Unit,
) {
    val unavailableDevice = stringResource(R.string.tools_device_unavailable)
    val deviceName = listOfNotNull(system.deviceManufacturer, system.deviceModel)
        .distinct()
        .joinToString(" ")
        .ifBlank { unavailableDevice }
    val androidLabel = system.androidVersion?.let {
        stringResource(R.string.tools_android_version, it)
    } ?: stringResource(R.string.tools_android_unavailable)
    val patchLabel = system.securityPatch?.let {
        stringResource(R.string.tools_security_patch, it)
    } ?: stringResource(R.string.tools_patch_unavailable)

    CozyTile(
        label = stringResource(R.string.tools_device),
        prominent = true,
        modifier = Modifier.fillMaxWidth().heightIn(min = deviceDashboardTileHeight),
    ) {
        TileTitle(deviceName, prominent = true)
        TileBody(stringResource(R.string.tools_device_version_summary, androidLabel, patchLabel))
        DeviceMetric(
            label = stringResource(R.string.tools_storage),
            availableBytes = system.storageAvailableBytes,
            totalBytes = system.storageTotalBytes,
        )
        DeviceMetric(
            label = stringResource(R.string.tools_memory),
            availableBytes = system.memoryAvailableBytes,
            totalBytes = system.memoryTotalBytes,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            TileAction(stringResource(R.string.tools_details)) {
                onSettingsSelected("device_info")
            }
            TileAction(stringResource(R.string.tools_storage)) {
                onSettingsSelected("storage")
            }
        }
    }
}

@Composable
private fun DeviceMetric(label: String, availableBytes: Long, totalBytes: Long) {
    val palette = LocalVeilPalette.current
    val context = LocalContext.current
    val usedFraction = StorageUsagePolicy.usedFraction(availableBytes, totalBytes)
    val detail = if (usedFraction == null) {
        stringResource(R.string.state_unavailable).uppercase()
    } else {
        val usedBytes = totalBytes - availableBytes.coerceIn(0L, totalBytes)
        val unavailable = stringResource(R.string.state_unavailable)
        stringResource(
            R.string.tools_capacity_usage,
            formatCapacity(context, usedBytes, unavailable),
            formatCapacity(context, totalBytes, unavailable),
        )
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        BasicText(label.uppercase(), style = workspaceMonoStyle(palette.contentMuted, 9))
        BasicText(detail, style = workspaceMonoStyle(palette.contentSecondary, 9))
    }
    if (usedFraction != null) SimpleProgress(usedFraction)
}

@Composable
private fun SettingsPanel(
    onVeilSettingsSelected: () -> Unit,
    onSettingsSelected: (String) -> Unit,
) {
    CozyTile(
        label = stringResource(R.string.tools_control_center),
        modifier = Modifier.fillMaxWidth().heightIn(min = settingsTileHeight),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCell(
                stringResource(R.string.tools_display),
                "display",
                onSettingsSelected,
                Modifier.weight(1f),
            )
            SettingsCell(
                stringResource(R.string.tools_sound),
                "sound",
                onSettingsSelected,
                Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            SettingsCell(
                stringResource(R.string.tools_apps),
                "applications",
                onSettingsSelected,
                Modifier.weight(1f),
            )
            SettingsCell(
                stringResource(R.string.tools_security),
                "security",
                onSettingsSelected,
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            TileAction(stringResource(R.string.tools_veil_settings), onVeilSettingsSelected)
            TileAction(stringResource(R.string.tools_all_settings)) {
                onSettingsSelected("settings")
            }
        }
    }
}

@Composable
private fun SettingsCell(
    label: String,
    id: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = label.uppercase(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 9),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LocalVeilPalette.current.subtleFill)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.action_open_named, label),
            ) { onSelected(id) }
            .padding(horizontal = 10.dp, vertical = 12.dp),
    )
}

private fun formatCapacity(context: Context, bytes: Long, unavailableLabel: String): String {
    if (bytes < 0L) return unavailableLabel
    return Formatter.formatShortFileSize(context, bytes)
}
