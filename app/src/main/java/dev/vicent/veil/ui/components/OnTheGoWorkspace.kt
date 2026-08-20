package dev.vicent.veil.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.AgendaPolicy
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.ui.theme.LocalVeilPalette
import kotlin.math.roundToInt

@Composable
internal fun OnTheGoWorkspace(
    state: OnTheGoWorkspaceUiState,
    compact: Boolean,
    onContinuityAccessRequested: () -> Unit,
    onLocationPermissionRequested: () -> Unit,
    onCalendarPermissionRequested: () -> Unit,
    onCalendarEventSelected: (Long) -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
) {
    val nextEvent = remember(state.calendarEvents) {
        AgendaPolicy.workEvents(state.calendarEvents, System.currentTimeMillis()).firstOrNull()
    }
    val navigation = state.navigation

    Column(verticalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
        CozyTile(
            label = stringResource(R.string.on_the_go_navigation),
            prominent = true,
            modifier = Modifier.fillMaxWidth().heightIn(
                min = WorkspaceLayoutTokens.PRIMARY_TILE_HEIGHT,
            ),
        ) {
            when {
                !state.continuityAccessGranted -> TileAction(
                    stringResource(R.string.media_enable_continuity),
                    onContinuityAccessRequested,
                )
                navigation == null -> {
                    TileTitle(stringResource(R.string.on_the_go_no_navigation), prominent = true)
                    TileBody(stringResource(R.string.on_the_go_no_navigation_body))
                }
                else -> {
                    BasicText(
                        text = navigation.appLabel.uppercase(),
                        style = workspaceMonoStyle(LocalVeilPalette.current.accentActive, 9),
                    )
                    TileTitle(navigation.title, prominent = true)
                    navigation.subtitle?.let { subtitle ->
                        BasicText(
                            text = subtitle,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = workspaceBodyStyle(LocalVeilPalette.current.contentSecondary),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (ContinuityAction.OPEN in navigation.supportedActions) {
                        TileAction(stringResource(R.string.on_the_go_open_route)) {
                            onContinuityAction(navigation.id, ContinuityAction.OPEN, null)
                        }
                    }
                }
            }
        }
        ResponsivePair(
            compact = compact,
            left = {
                CozyTile(
                    label = stringResource(R.string.on_the_go_weather),
                    modifier = Modifier.fillMaxWidth().heightIn(
                        min = WorkspaceLayoutTokens.SECONDARY_TILE_HEIGHT,
                    ),
                ) {
                    when (state.weather.availability) {
                        WeatherAvailability.NEEDS_PERMISSION -> TileAction(
                            stringResource(R.string.current_weather_enable),
                            onLocationPermissionRequested,
                        )
                        WeatherAvailability.LOADING -> TileBody(
                            stringResource(R.string.current_weather_loading),
                        )
                        WeatherAvailability.UNAVAILABLE -> TileBody(
                            stringResource(R.string.current_weather_unavailable),
                        )
                        WeatherAvailability.AVAILABLE -> {
                            WeatherGlyph(state.weather.weatherCode, Modifier.size(44.dp))
                            TileTitle(
                                stringResource(
                                    R.string.current_weather_value,
                                    state.weather.temperatureCelsius?.roundToInt()?.toString() ?: "—",
                                    stringResource(
                                        weatherDescriptionResource(state.weather.weatherCode),
                                    ),
                                ),
                            )
                            TileBody(
                                stringResource(
                                    R.string.on_the_go_weather_range,
                                    state.weather.apparentTemperatureCelsius?.roundToInt()?.toString() ?: "—",
                                    state.weather.minimumCelsius?.roundToInt()?.toString() ?: "—",
                                    state.weather.maximumCelsius?.roundToInt()?.toString() ?: "—",
                                ),
                            )
                        }
                    }
                }
            },
            right = {
                CozyTile(
                    label = stringResource(R.string.on_the_go_next_event),
                    modifier = Modifier.fillMaxWidth().heightIn(
                        min = WorkspaceLayoutTokens.SECONDARY_TILE_HEIGHT,
                    ),
                ) {
                    when {
                        !state.calendarAccessGranted -> TileAction(
                            stringResource(R.string.agenda_connect),
                            onCalendarPermissionRequested,
                        )
                        nextEvent == null -> TileBody(
                            stringResource(R.string.on_the_go_no_event),
                        )
                        else -> EventRow(nextEvent, onCalendarEventSelected)
                    }
                }
            },
        )
    }
}
