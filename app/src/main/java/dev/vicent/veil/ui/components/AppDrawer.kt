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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.ui.theme.LocalVeilPalette
import java.text.Normalizer
import java.util.Locale

@Composable
fun AppDrawer(
    installedApps: List<LauncherApp>,
    settingsShortcuts: List<SettingsShortcut>,
    isLoading: Boolean,
    onAppSelected: (LauncherApp) -> Unit,
    onAppLongPressed: (LauncherApp) -> Unit,
    onSettingsSelected: (SettingsShortcut) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    val normalizedTerms = remember(query) {
        query.normalizeForSearch().split(Regex("\\s+")).filter(String::isNotBlank)
    }
    val visibleSettings = remember(settingsShortcuts, normalizedTerms) {
        if (normalizedTerms.isEmpty()) {
            settingsShortcuts.take(1)
        } else {
            settingsShortcuts.filter { shortcut ->
                val searchable = "ajustes configuracion sistema ${shortcut.label} " +
                    shortcut.searchTerms
                val normalizedSearchable = searchable.normalizeForSearch()
                normalizedTerms.all(normalizedSearchable::contains)
            }
        }
    }
    val visibleApps = remember(installedApps, normalizedTerms) {
        if (normalizedTerms.isEmpty()) {
            installedApps
        } else {
            installedApps.filter { app ->
                val searchable = "${app.label} ${app.packageName}".normalizeForSearch()
                normalizedTerms.all(searchable::contains)
            }
        }
    }
    val firstResult = visibleSettings.firstOrNull() ?: visibleApps.firstOrNull()

    Column(
        modifier = modifier
            .background(palette.drawerBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        DrawerHeader(onClose = onClose)
        SearchField(
            query = query,
            onQueryChanged = { query = it },
            onClear = { query = "" },
            onSubmit = {
                when (firstResult) {
                    is SettingsShortcut -> onSettingsSelected(firstResult)
                    is LauncherApp -> onAppSelected(firstResult)
                }
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (visibleSettings.isNotEmpty()) {
                item(key = "system-header") {
                    DrawerSectionLabel(text = "SISTEMA")
                }
                items(visibleSettings, key = { "settings-${it.id}" }) { shortcut ->
                    SettingsRow(
                        shortcut = shortcut,
                        onClick = { onSettingsSelected(shortcut) },
                    )
                }
            }

            if (visibleApps.isNotEmpty()) {
                item(key = "apps-header") {
                    DrawerSectionLabel(
                        text = if (normalizedTerms.isEmpty()) {
                            "APLICACIONES  ${visibleApps.size}"
                        } else {
                            "RESULTADOS  ${visibleApps.size}"
                        },
                    )
                }
                items(
                    items = visibleApps,
                    key = { "app-${it.componentName.flattenToShortString()}" },
                ) { app ->
                    DrawerAppRow(
                        app = app,
                        onClick = { onAppSelected(app) },
                        onLongClick = {
                            keyboardController?.hide()
                            onAppLongPressed(app)
                        },
                    )
                }
            }

            if (!isLoading && visibleSettings.isEmpty() && visibleApps.isEmpty()) {
                item(key = "empty") {
                    EmptyResult(query = query)
                }
            }

            if (isLoading) {
                item(key = "loading") {
                    DrawerSectionLabel(text = "BUSCANDO APLICACIONES…")
                }
            }

            item(key = "bottom-space") { Spacer(modifier = Modifier.height(24.dp)) }
        }
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
            text = "VEIL / APPS",
            style = TextStyle(
                color = palette.contentSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 1.8.sp,
            ),
        )
        BasicText(
            text = "CERRAR  ×",
            style = TextStyle(
                color = palette.contentSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
            ),
            modifier = Modifier
                .clickable(role = Role.Button, onClickLabel = "Cerrar aplicaciones") {
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
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
            ),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            singleLine = true,
            textStyle = TextStyle(
                color = palette.contentPrimary,
                fontFamily = FontFamily.SansSerif,
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
                            text = "Buscar apps y ajustes",
                            style = TextStyle(
                                color = palette.contentMuted,
                                fontFamily = FontFamily.SansSerif,
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
                .focusRequester(focusRequester),
        )
        if (query.isNotEmpty()) {
            BasicText(
                text = "×",
                style = TextStyle(
                    color = palette.contentSecondary,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 22.sp,
                ),
                modifier = Modifier
                    .clickable(role = Role.Button, onClickLabel = "Borrar búsqueda") {
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
            fontFamily = FontFamily.Monospace,
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
                onClickLabel = "Abrir ${app.label}",
                onLongClickLabel = "Opciones de ${app.label}",
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LauncherAppIcon(app = app, size = 28.dp)
        Column(modifier = Modifier.padding(start = 20.dp)) {
            BasicText(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.contentPrimary,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 16.sp,
                ),
            )
            BasicText(
                text = app.packageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.contentMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                ),
            )
        }
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
                onClickLabel = "Abrir ${shortcut.label}",
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
                fontFamily = FontFamily.SansSerif,
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
            text = "SIN RESULTADOS",
            style = TextStyle(
                color = palette.contentSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 1.6.sp,
            ),
        )
        BasicText(
            text = "No hay apps ni ajustes que coincidan con “$query”.",
            style = TextStyle(
                color = palette.contentMuted,
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
            ),
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

private fun String.normalizeForSearch(): String = Normalizer
    .normalize(this, Normalizer.Form.NFD)
    .replace(COMBINING_MARKS, "")
    .lowercase(Locale.ROOT)

private val COMBINING_MARKS = Regex("\\p{M}+")
