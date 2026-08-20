package dev.vicent.veil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.WorkspaceAvailability
import dev.vicent.veil.launcher.model.WorkspaceCapability
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
internal fun WorkspaceSettingsScreen(
    preferences: LauncherPreferences,
    catalog: List<LauncherContext>,
    firstRun: Boolean,
    onBack: () -> Unit,
    onWorkspaceReplaced: (Int, LauncherContextKind) -> Unit,
    onWorkspaceMoved: (Int, Int) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var replacingPosition by remember { mutableStateOf<Int?>(null) }
    val selectedKinds = preferences.selectedWorkspaceKinds
    val definitions = remember(catalog) { catalog.associateBy(LauncherContext::kind) }
    val palette = LocalVeilPalette.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.drawerBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        SettingsHeader(
            title = stringResource(
                if (firstRun) R.string.settings_workspace_welcome_title
                else R.string.settings_workspaces_header,
            ),
            onBack = if (replacingPosition != null) {
                { replacingPosition = null }
            } else onBack,
        )

        if (replacingPosition != null) {
            val position = checkNotNull(replacingPosition)
            SettingsDescription(stringResource(R.string.settings_workspace_choose_title))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = catalog.filter {
                        it.kind !in selectedKinds &&
                            it.availability == WorkspaceAvailability.AVAILABLE
                    },
                    key = LauncherContext::id,
                ) { definition ->
                    WorkspaceCatalogRow(definition) {
                        onWorkspaceReplaced(position, definition.kind)
                        replacingPosition = null
                    }
                }
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "workspace-intro") {
                SettingsDescription(
                    stringResource(
                        if (firstRun) R.string.settings_workspace_welcome_body
                        else R.string.settings_workspaces_intro,
                    ),
                )
            }
            item(key = "workspace-home") {
                WorkspaceFixedHomeRow()
            }
            items(
                count = selectedKinds.size,
                key = { position -> "workspace-${selectedKinds[position].name}" },
            ) { position ->
                val kind = selectedKinds[position]
                val definition = definitions[kind] ?: return@items
                SettingsActionRow(
                    title = stringResource(definition.titleResource),
                    detail = stringResource(definition.descriptionResource),
                    status = stringResource(R.string.settings_workspace_replace),
                    onClick = { replacingPosition = position },
                )
                if (definition.availability == WorkspaceAvailability.RETIRING) {
                    SettingsDescription(stringResource(R.string.settings_workspace_retiring))
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp),
                ) {
                    BasicText(
                        text = stringResource(R.string.settings_workspace_position, position + 2),
                        style = workspaceMonoStyle(palette.contentMuted, 8),
                        modifier = Modifier.weight(1f).padding(start = 7.dp),
                    )
                    RofiAction(
                        label = stringResource(R.string.settings_workspace_move_before),
                        enabled = position > 0,
                        onClick = { onWorkspaceMoved(position, position - 1) },
                    )
                    RofiAction(
                        label = stringResource(R.string.settings_workspace_move_after),
                        enabled = position < selectedKinds.lastIndex,
                        onClick = { onWorkspaceMoved(position, position + 1) },
                    )
                }
            }
            if (firstRun) {
                item(key = "workspace-complete") {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                    ) {
                        RofiAction(
                            label = stringResource(R.string.settings_workspace_done),
                            onClick = onComplete,
                        )
                    }
                }
            }
            item(key = "workspace-bottom") { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun WorkspaceFixedHomeRow() {
    val palette = LocalVeilPalette.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 13.dp)) {
        BasicText(
            text = stringResource(R.string.settings_workspace_home_fixed),
            style = workspaceTitleStyle(palette.contentPrimary, prominent = false),
        )
        BasicText(
            text = stringResource(R.string.settings_workspace_home_detail),
            style = workspaceMonoStyle(palette.contentMuted, 8),
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun WorkspaceCatalogRow(definition: LauncherContext, onClick: () -> Unit) {
    val requirements = definition.capabilities.mapNotNull { capability ->
        capability.requirementLabel()
    }.distinct()
    val requirementDetail = if (requirements.isEmpty()) {
        stringResource(R.string.settings_workspace_no_requirements)
    } else {
        stringResource(R.string.settings_workspace_requirements, requirements.joinToString(" · "))
    }
    Column {
        SettingsActionRow(
            title = stringResource(definition.titleResource),
            detail = stringResource(definition.descriptionResource),
            status = stringResource(R.string.state_choose),
            onClick = onClick,
        )
        SettingsDescription(requirementDetail)
        if (definition.availability == WorkspaceAvailability.RETIRING) {
            SettingsDescription(stringResource(R.string.settings_workspace_retiring))
        }
    }
}

@Composable
private fun WorkspaceCapability.requirementLabel(): String? = when (this) {
    WorkspaceCapability.CALENDAR -> stringResource(R.string.workspace_requirement_calendar)
    WorkspaceCapability.WEATHER -> stringResource(R.string.workspace_requirement_location)
    WorkspaceCapability.CONTINUITY,
    WorkspaceCapability.WORK_PROGRESS,
    -> stringResource(R.string.workspace_requirement_continuity)
    WorkspaceCapability.AUDIO -> stringResource(R.string.workspace_requirement_audio)
    WorkspaceCapability.STEAM -> stringResource(R.string.workspace_requirement_network)
    WorkspaceCapability.SYSTEM_STATUS -> null
}
