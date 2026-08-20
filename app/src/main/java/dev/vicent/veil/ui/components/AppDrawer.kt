package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.AppSearchLearningState
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.launcher.AppSearchCandidate
import dev.vicent.veil.launcher.AppSearchPolicy
import dev.vicent.veil.launcher.normalizeSearchText
import dev.vicent.veil.R
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun AppDrawer(
    installedApps: List<LauncherApp>,
    searchLearning: AppSearchLearningState,
    settingsShortcuts: List<SettingsShortcut>,
    isLoading: Boolean,
    isOpen: Boolean,
    onAppSelected: (LauncherApp, String) -> Unit,
    onAppLongPressed: (LauncherApp, String) -> Unit,
    onSettingsSelected: (SettingsShortcut) -> Unit,
    onVeilSettingsSelected: () -> Unit,
    continuityAccessGranted: Boolean,
    onContinuityAccessSelected: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val rankingTimeMillis = remember { System.currentTimeMillis() }
    val veilSettingsSearchTerms = stringResource(R.string.drawer_veil_settings_search)
    val continuitySearchTerms = stringResource(R.string.drawer_continuity_search)
    val settingsSearchPrefix = stringResource(R.string.drawer_settings_search_prefix)
    val normalizedTerms = remember(query) {
        query.normalizeForSearch().split(' ').filter(String::isNotBlank)
    }
    val veilSettingsVisible = remember(query, veilSettingsSearchTerms) {
        veilSettingsMatches(query, veilSettingsSearchTerms)
    }
    val continuityVisible = remember(query, continuitySearchTerms) {
        AppSearchPolicy.matches(query, continuitySearchTerms)
    }
    val visibleSettings = remember(settingsShortcuts, normalizedTerms, settingsSearchPrefix) {
        if (normalizedTerms.isEmpty()) {
            settingsShortcuts.take(1)
        } else {
            settingsShortcuts.filter { shortcut ->
                val searchable = "$settingsSearchPrefix ${shortcut.label} " +
                    shortcut.searchTerms
                AppSearchPolicy.matches(query, searchable)
            }
        }
    }
    val searchCandidates = remember(installedApps) {
        installedApps.mapIndexed { index, app ->
            AppSearchCandidate(
                packageName = app.packageName,
                label = app.label,
                sourceIndex = index,
            )
        }
    }
    val appsByPackage = remember(installedApps) {
        installedApps.associateBy(LauncherApp::packageName)
    }
    val visibleApps = remember(
        installedApps,
        searchCandidates,
        appsByPackage,
        query,
        searchLearning,
        rankingTimeMillis,
    ) {
        if (normalizedTerms.isEmpty()) {
            installedApps
        } else {
            AppSearchPolicy.rank(
                candidates = searchCandidates,
                rawQuery = query,
                learning = searchLearning.entries,
                nowMillis = rankingTimeMillis,
            ).mapNotNull { appsByPackage[it.packageName] }
        }
    }
    val firstResult = visibleApps.firstOrNull()
        ?: when {
            veilSettingsVisible -> VeilSettingsResult
            continuityVisible -> ContinuityAccessResult
            else -> visibleSettings.firstOrNull()
        }

    LaunchedEffect(query) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            listState.scrollToItem(0)
        }
    }
    LaunchedEffect(isOpen) {
        if (!isOpen) query = ""
    }

    Column(
        modifier = modifier
            .background(palette.drawerBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        DrawerHeader(
            onClose = {
                query = ""
                onClose()
            },
        )
        SearchField(
            query = query,
            onQueryChanged = { query = it },
            onClear = { query = "" },
            onSubmit = {
                if (query.isBlank()) return@SearchField
                when (firstResult) {
                    VeilSettingsResult -> onVeilSettingsSelected()
                    ContinuityAccessResult -> onContinuityAccessSelected()
                    is SettingsShortcut -> onSettingsSelected(firstResult)
                    is LauncherApp -> onAppSelected(firstResult, query)
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (visibleApps.isNotEmpty()) {
                item(key = "apps-header") {
                    DrawerSectionLabel(
                        text = if (normalizedTerms.isEmpty()) {
                            pluralStringResource(
                                R.plurals.drawer_apps_count,
                                visibleApps.size,
                                visibleApps.size,
                            )
                        } else {
                            pluralStringResource(
                                R.plurals.drawer_results_count,
                                visibleApps.size,
                                visibleApps.size,
                            )
                        },
                    )
                }
                items(
                    items = visibleApps,
                    key = { "app-${it.componentName.flattenToShortString()}" },
                ) { app ->
                    DrawerAppRow(
                        app = app,
                        onClick = { onAppSelected(app, query) },
                        onLongClick = {
                            keyboardController?.hide()
                            onAppLongPressed(app, query)
                        },
                    )
                }
            }

            val hasSystemResults = veilSettingsVisible || continuityVisible || visibleSettings.isNotEmpty()
            if (hasSystemResults) {
                item(key = "system-header") {
                    DrawerSectionLabel(text = stringResource(R.string.drawer_system))
                }
            }
            if (veilSettingsVisible) {
                item(key = "veil-settings") {
                    VeilSettingsRow(onClick = onVeilSettingsSelected)
                }
            }
            if (continuityVisible) {
                item(key = "continuity-access") {
                    ContinuityAccessRow(
                        accessGranted = continuityAccessGranted,
                        onClick = onContinuityAccessSelected,
                    )
                }
            }
            if (visibleSettings.isNotEmpty()) {
                items(visibleSettings, key = { "settings-${it.id}" }) { shortcut ->
                    SettingsRow(
                        shortcut = shortcut,
                        onClick = { onSettingsSelected(shortcut) },
                    )
                }
            }

            if (
                !isLoading &&
                !veilSettingsVisible &&
                !continuityVisible &&
                visibleSettings.isEmpty() &&
                visibleApps.isEmpty()
            ) {
                item(key = "empty") {
                    EmptyResult(query = query)
                }
            }

            if (isLoading) {
                item(key = "loading") {
                    DrawerSectionLabel(text = stringResource(R.string.drawer_loading_apps))
                }
            }

            item(key = "bottom-space") { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private data object VeilSettingsResult
private data object ContinuityAccessResult

internal fun veilSettingsMatches(query: String, searchable: String): Boolean =
    AppSearchPolicy.matches(query, searchable)

@Composable
private fun ContinuityAccessRow(
    accessGranted: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    val label = if (accessGranted) {
        stringResource(R.string.continuity_access_enabled)
    } else {
        stringResource(R.string.continuity_access_review)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityGlyph(
            kind = if (accessGranted) ActivityGlyphKind.CURRENT else ActivityGlyphKind.PROGRESS,
            size = 28.dp,
            isActive = accessGranted,
        )
        BasicText(
            text = label,
            style = TextStyle(
                color = palette.contentPrimary,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.padding(start = 20.dp),
        )
    }
}

@Composable
private fun VeilSettingsRow(onClick: () -> Unit) {
    val palette = LocalVeilPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.drawer_open_veil_settings),
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            drawCircle(
                color = palette.accentActive,
                radius = 10.dp.toPx(),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(color = palette.accentActive, radius = 3.dp.toPx())
        }
        BasicText(
            text = stringResource(R.string.drawer_veil_settings),
            style = TextStyle(
                color = palette.contentPrimary,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.padding(start = 20.dp),
        )
    }
}

@Composable
private fun DrawerHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = stringResource(R.string.drawer_header),
            style = TextStyle(
                color = palette.contentSecondary,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
                fontSize = 12.sp,
                letterSpacing = 1.8.sp,
            ),
        )
        BasicText(
            text = stringResource(R.string.drawer_close),
            style = TextStyle(
                color = palette.contentSecondary,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
            ),
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.drawer_close_action),
                ) {
                    onClose()
                }
                .padding(horizontal = 8.dp, vertical = 12.dp),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.divider),
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(palette.fieldBackground)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = ">",
            style = TextStyle(
                color = palette.accentActive,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
                fontSize = 18.sp,
            ),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            singleLine = true,
            textStyle = TextStyle(
                color = palette.contentPrimary,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                fontSize = 17.sp,
                letterSpacing = 0.4.sp,
            ),
            cursorBrush = SolidColor(palette.accentActive),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        BasicText(
                            text = stringResource(R.string.drawer_search_hint),
                            style = TextStyle(
                                color = palette.contentMuted,
                                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                                fontSize = 17.sp,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
                .testTag(APP_DRAWER_SEARCH_TEST_TAG)
                .focusRequester(focusRequester),
        )
        if (query.isNotEmpty()) {
            BasicText(
                text = "×",
                style = TextStyle(
                    color = palette.contentSecondary,
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                    fontSize = 22.sp,
                ),
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.drawer_clear_search),
                    ) {
                        onClear()
                    }
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    val palette = LocalVeilPalette.current
    BasicText(
        text = text,
        style = TextStyle(
            color = palette.contentMuted,
            fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
            fontSize = 11.sp,
            letterSpacing = 1.6.sp,
        ),
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun DrawerAppRow(
    app: LauncherApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.action_open_named, app.label),
                onLongClickLabel = stringResource(R.string.action_options_named, app.label),
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LauncherAppIcon(app = app, size = 28.dp)
        BasicText(
            text = app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = palette.contentPrimary,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                fontSize = 16.sp,
            ),
            modifier = Modifier.padding(start = 20.dp),
        )
    }
}

@Composable
private fun SettingsRow(
    shortcut: SettingsShortcut,
    onClick: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.action_open_named, shortcut.label),
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            drawCircle(
                color = palette.contentSecondary,
                radius = 9.dp.toPx(),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(color = palette.accentActive, radius = 2.dp.toPx())
        }
        BasicText(
            text = shortcut.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = palette.contentPrimary,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.padding(start = 20.dp),
        )
    }
}

@Composable
private fun EmptyResult(query: String) {
    val palette = LocalVeilPalette.current
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp)) {
        BasicText(
            text = stringResource(R.string.drawer_no_results),
            style = TextStyle(
                color = palette.contentSecondary,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
                fontSize = 12.sp,
                letterSpacing = 1.6.sp,
            ),
        )
        BasicText(
            text = stringResource(R.string.drawer_no_results_detail, query),
            style = TextStyle(
                color = palette.contentMuted,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                fontSize = 14.sp,
            ),
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

internal fun String.normalizeForSearch(): String = normalizeSearchText(this)

internal const val APP_DRAWER_SEARCH_TEST_TAG = "app-drawer-search"
