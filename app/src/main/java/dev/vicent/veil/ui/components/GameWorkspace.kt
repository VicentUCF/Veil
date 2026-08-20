package dev.vicent.veil.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.GameFeedAvailability
import dev.vicent.veil.launcher.model.GameFeedState
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.SteamChartEntry
import dev.vicent.veil.launcher.model.SteamNewsItem
import dev.vicent.veil.ui.theme.LocalVeilPalette
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GameChartTileHeight = 204.dp
private val GameSecondaryTileHeight = 142.dp
private const val STEAM_CHART_URL = "https://store.steampowered.com/charts/mostplayed"

@Composable
internal fun GameWorkspace(
    feed: GameFeedState,
    library: List<LauncherApp>,
    compact: Boolean,
    onExternalLinkSelected: (String) -> Unit,
    onAppSelected: (LauncherApp) -> Unit,
    onAppLongPressed: (LauncherApp) -> Unit,
) {
    var showNews by remember { mutableStateOf(false) }
    var showLibrary by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
        SteamChartTile(
            feed = feed,
            onEntrySelected = onExternalLinkSelected,
            onFullChartSelected = { onExternalLinkSelected(STEAM_CHART_URL) },
            modifier = Modifier.fillMaxWidth().height(GameChartTileHeight),
        )
        GameSecondaryRow(
            compact = compact,
            news = {
                SteamNewsTile(
                    feed = feed,
                    onSelected = onExternalLinkSelected,
                    onMore = { showNews = true },
                )
            },
            library = {
                GameLibraryTile(
                    library = library,
                    onOpen = { showLibrary = true },
                )
            },
        )
    }

    if (showNews) {
        SteamNewsDialog(
            news = feed.news,
            onDismiss = { showNews = false },
            onSelected = onExternalLinkSelected,
        )
    }
    if (showLibrary) {
        GameLibraryDialog(
            library = library,
            onDismiss = { showLibrary = false },
            onAppSelected = onAppSelected,
            onAppLongPressed = onAppLongPressed,
        )
    }
}

@Composable
private fun SteamChartTile(
    feed: GameFeedState,
    onEntrySelected: (String) -> Unit,
    onFullChartSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = feed.chart.firstOrNull()
    CozyTile(
        label = stringResource(R.string.game_chart_label),
        prominent = true,
        onClick = onFullChartSelected,
        modifier = modifier,
    ) {
        when {
            top != null -> {
                val artwork = feed.heroArtwork?.let { bitmap ->
                    remember(bitmap) { bitmap.asImageBitmap() }
                }
                if (artwork != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .clickable(role = Role.Button) { onEntrySelected(top.storeUrl) },
                    ) {
                        Image(
                            bitmap = artwork,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .52f)))
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                            BasicText(
                                text = "#${top.rank}  ${top.title}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = workspaceTitleStyle(Color.White, prominent = true),
                            )
                            top.peakPlayers?.let { peak ->
                                BasicText(
                                    text = stringResource(
                                        R.string.game_peak_compact,
                                        formatPlayers(peak),
                                    ),
                                    style = workspaceMonoStyle(Color.White.copy(alpha = .76f), 8),
                                )
                            }
                        }
                    }
                } else {
                    BasicText(
                        "#${top.rank}  ${top.title}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = workspaceTitleStyle(
                            LocalVeilPalette.current.contentPrimary,
                            prominent = true,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onEntrySelected(top.storeUrl) }
                            .padding(vertical = 4.dp),
                    )
                    top.peakPlayers?.let {
                        TileBody(
                            stringResource(R.string.game_peak_players, formatPlayers(it)),
                        )
                    }
                }
                feed.chart.drop(1).take(4).forEach { entry ->
                    SteamCompactRank(entry) { onEntrySelected(entry.storeUrl) }
                }
                if (feed.isStale) {
                    BasicText(
                        stringResource(R.string.game_cached_stale),
                        style = workspaceMonoStyle(LocalVeilPalette.current.contentMuted, 8),
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
            feed.availability == GameFeedAvailability.LOADING -> {
                TileTitle(stringResource(R.string.game_loading_title), prominent = true)
                TileBody(stringResource(R.string.game_loading_body))
            }
            feed.availability == GameFeedAvailability.UNAVAILABLE -> {
                TileTitle(stringResource(R.string.game_unavailable_title), prominent = true)
                TileBody(stringResource(R.string.game_unavailable_body))
            }
            else -> {
                TileTitle(stringResource(R.string.game_idle_title), prominent = true)
                TileBody(stringResource(R.string.game_idle_body))
            }
        }
    }
}

@Composable
private fun SteamCompactRank(entry: SteamChartEntry, onClick: () -> Unit) {
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(top = 5.dp, bottom = 3.dp),
    ) {
        BasicText(
            "#${entry.rank}",
            style = workspaceMonoStyle(palette.accentActive, 9),
            modifier = Modifier.width(30.dp),
        )
        BasicText(
            entry.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = workspaceMonoStyle(palette.contentPrimary, 9),
            modifier = Modifier.weight(1f),
        )
        BasicText(rankMovement(entry), style = workspaceMonoStyle(palette.contentMuted, 8))
    }
}

@Composable
private fun SteamNewsTile(
    feed: GameFeedState,
    onSelected: (String) -> Unit,
    onMore: () -> Unit,
) {
    CozyTile(
        label = stringResource(R.string.game_news_label),
        modifier = Modifier.fillMaxWidth().height(GameSecondaryTileHeight),
    ) {
        if (feed.news.isNotEmpty()) {
            feed.news.take(2).forEach { item ->
                SteamNewsCompactRow(item) { onSelected(item.url) }
            }
            BasicText(
                stringResource(R.string.game_news_more),
                style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 9),
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.game_news_more_action),
                        onClick = onMore,
                    )
                    .padding(top = 4.dp, bottom = 1.dp),
            )
        } else {
            TileTitle(stringResource(R.string.game_news_empty_title))
            TileBody(stringResource(R.string.game_news_empty_body))
        }
    }
}

@Composable
private fun GameLibraryTile(library: List<LauncherApp>, onOpen: () -> Unit) {
    CozyTile(
        label = stringResource(R.string.game_library_label),
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().height(GameSecondaryTileHeight),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            GameFolderGlyph(modifier = Modifier.size(62.dp))
            BasicText(
                pluralStringResource(R.plurals.game_count, library.size, library.size),
                style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 10),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SteamNewsDialog(
    news: List<SteamNewsItem>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    RofiDialog(
        title = stringResource(R.string.game_news_dialog_title),
        onDismiss = onDismiss,
        actions = { RofiAction(stringResource(R.string.action_close), onDismiss) },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
        ) {
            news.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(3.dp))
                        .background(LocalVeilPalette.current.fieldBackground.copy(alpha = .52f))
                        .clickable(role = Role.Button) { onSelected(item.url) }
                        .padding(11.dp),
                ) {
                    BasicText(
                        stringResource(
                            R.string.game_news_metadata,
                            item.gameTitle.uppercase(),
                            formatNewsDate(item.publishedAtMillis).uppercase(),
                        ),
                        style = workspaceMonoStyle(LocalVeilPalette.current.accentActive, 8),
                    )
                    BasicText(
                        item.title,
                        style = workspaceBodyStyle(LocalVeilPalette.current.contentPrimary),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SteamNewsCompactRow(item: SteamNewsItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 5.dp),
    ) {
        BasicText(
            item.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = workspaceBodyStyle(LocalVeilPalette.current.contentPrimary),
        )
        BasicText(
            stringResource(
                R.string.game_news_metadata,
                item.gameTitle.uppercase(),
                formatNewsDate(item.publishedAtMillis).uppercase(),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = workspaceMonoStyle(LocalVeilPalette.current.contentMuted, 7),
        )
    }
}

@Composable
private fun GameFolderGlyph(modifier: Modifier = Modifier) {
    val palette = LocalVeilPalette.current
    Canvas(modifier = modifier) {
        val strokeWidth = 1.2.dp.toPx()
        val outline = Path().apply {
            moveTo(size.width * .12f, size.height * .30f)
            lineTo(size.width * .39f, size.height * .30f)
            lineTo(size.width * .49f, size.height * .20f)
            lineTo(size.width * .76f, size.height * .20f)
            lineTo(size.width * .88f, size.height * .36f)
            lineTo(size.width * .88f, size.height * .78f)
            lineTo(size.width * .12f, size.height * .78f)
            close()
        }
        drawPath(outline, palette.accentActive, style = Stroke(width = strokeWidth))
        repeat(3) { index ->
            drawRoundRect(
                color = palette.contentSecondary,
                topLeft = Offset(size.width * (.27f + index * .17f), size.height * .48f),
                size = Size(size.width * .10f, size.width * .10f),
                cornerRadius = CornerRadius(size.width * .02f),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

@Composable
private fun GameSecondaryRow(
    compact: Boolean,
    news: @Composable () -> Unit,
    library: @Composable () -> Unit,
) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
            news()
            library()
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
            Box(modifier = Modifier.weight(1.55f)) { news() }
            Box(modifier = Modifier.weight(.85f)) { library() }
        }
    }
}

@Composable
private fun GameLibraryDialog(
    library: List<LauncherApp>,
    onDismiss: () -> Unit,
    onAppSelected: (LauncherApp) -> Unit,
    onAppLongPressed: (LauncherApp) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = remember(query) { query.normalizeForSearch() }
    val visible = remember(library, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            library
        } else {
            library.filter { app ->
                "${app.label} ${app.packageName}".normalizeForSearch().contains(normalizedQuery)
            }
        }
    }
    RofiDialog(
        title = stringResource(R.string.game_library_dialog_title, library.size),
        onDismiss = onDismiss,
        actions = { RofiAction(stringResource(R.string.action_close), onDismiss) },
    ) {
        RofiEditorField(
            label = stringResource(R.string.game_library_search),
            value = query,
            onValueChange = { query = it.take(80) },
            hint = pluralStringResource(
                R.plurals.search_results_count,
                visible.size,
                visible.size,
            ),
            singleLine = true,
        )
        Column(
            modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
        ) {
            if (visible.isEmpty()) {
                RofiBody(
                    if (library.isEmpty()) {
                        stringResource(R.string.game_library_empty)
                    } else {
                        stringResource(R.string.search_no_matches)
                    },
                )
            }
            visible.forEach { app ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.action_open_named, app.label),
                            onLongClickLabel = stringResource(
                                R.string.action_options_named,
                                app.label,
                            ),
                            onClick = {
                                onDismiss()
                                onAppSelected(app)
                            },
                            onLongClick = {
                                onDismiss()
                                onAppLongPressed(app)
                            },
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                ) {
                    LauncherAppIcon(app = app, size = 34.dp)
                    Column(modifier = Modifier.padding(start = 11.dp).weight(1f)) {
                        BasicText(
                            app.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = workspaceBodyStyle(LocalVeilPalette.current.contentPrimary),
                        )
                        BasicText(
                            app.packageName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = workspaceMonoStyle(LocalVeilPalette.current.contentMuted, 7),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rankMovement(entry: SteamChartEntry): String {
    val previous = entry.previousRank ?: return stringResource(R.string.game_rank_new)
    val movement = previous - entry.rank
    return when {
        movement > 0 -> "▲$movement"
        movement < 0 -> "▼${-movement}"
        else -> "—"
    }
}

private fun formatPlayers(value: Int): String = NumberFormat.getIntegerInstance().format(value)

@Composable
private fun formatNewsDate(value: Long): String {
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val pattern = remember(locale) { DateFormat.getBestDateTimePattern(locale, "dMMM") }
    return remember(value, locale, pattern) {
        SimpleDateFormat(pattern, locale).format(Date(value))
    }
}
