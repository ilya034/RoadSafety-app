package team.kid.roadsafety.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.ast.Expression
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.value.ColorValue
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.TileSetOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.sources.rememberVectorSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.map.MapCity
import team.kid.roadsafety.domain.aggregates.map.MapCityBbox
import org.maplibre.spatialk.geojson.Feature as GeoJsonFeature

@Composable
fun MapColoringScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(60.6122, 56.8519),
            zoom = 12.0
        )
    )

    LaunchedEffect(Unit) {
        viewModel.refreshForCurrentUser()
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScreenWork()
        }
    }

    val currentLocationSource = remember(state.currentLocation) {
        if (state.currentLocation != null) {
            val point = Point(
                Position(state.currentLocation!!.longitude, state.currentLocation!!.latitude)
            )
            FeatureCollection(listOf(GeoJsonFeature(point, JsonObject(emptyMap()), null)))
        } else {
            FeatureCollection(emptyList<GeoJsonFeature<Point, JsonObject>>())
        }
    }

    val childLocationSource = remember(state.childLocations) {
        if (state.childLocations.isNotEmpty()) {
            FeatureCollection(
                state.childLocations.map { child ->
                    GeoJsonFeature(
                        Point(Position(child.point.longitude, child.point.latitude)),
                        JsonObject(
                            mapOf(
                                "childId" to JsonPrimitive(child.childId),
                                "displayName" to JsonPrimitive(child.label),
                                "risk" to JsonPrimitive(child.currentRisk),
                                "lastUpdatedAt" to JsonPrimitive(child.lastUpdatedAt)
                            )
                        ),
                        null
                    )
                }
            )
        } else {
            FeatureCollection(emptyList<GeoJsonFeature<Point, JsonObject>>())
        }
    }

    LaunchedEffect(state.activeMapCityId, state.metadata?.bbox, state.cities) {
        val bbox = state.metadata?.bbox
            ?: state.cities.firstOrNull { it.cityId == state.activeMapCityId }?.bbox
        if (bbox != null) {
            cameraState.position = CameraPosition(
                target = bbox.centerPosition(),
                zoom = bbox.defaultZoom()
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri(MapBaseStyleUrl),
            cameraState = cameraState
        ) {
            state.tileUrl?.let { tileUrl ->
                key(tileUrl) {
                    val vectorSource = rememberVectorSource(
                        tiles = listOf(tileUrl),
                        options = TileSetOptions(minZoom = 9, maxZoom = 18)
                    )

                    FillLayer(
                        id = "safety-zones-layer",
                        source = vectorSource,
                        sourceLayer = "safety_zones",
                        minZoom = 9f,
                        maxZoom = 18f,
                        color = safetyZoneColorExpression(state.overrides),
                        opacity = const(0.35f),
                        onClick = { clickedFeatures ->
                            val baseAreaKey = clickedFeatures
                                .firstOrNull()
                                ?.properties
                                ?.get("baseAreaKey")
                                ?.jsonPrimitive
                                ?.contentOrNull

                            if (baseAreaKey != null) {
                                viewModel.onBaseAreaClicked(baseAreaKey)
                                ClickResult.Consume
                            } else {
                                ClickResult.Pass
                            }
                        }
                    )
                }
            }

            if (state.currentLocation != null) {
                key(state.currentLocation) {
                    val locationGeoJsonSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(currentLocationSource)
                    )
                    CircleLayer(
                        id = "current-location-marker",
                        source = locationGeoJsonSource,
                        color = const(Color.Blue),
                        radius = const(8.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp)
                    )
                }
            }

            if (state.childLocations.isNotEmpty()) {
                key(state.childLocations) {
                    val childrenGeoJsonSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(childLocationSource)
                    )
                    CircleLayer(
                        id = "children-location-markers",
                        source = childrenGeoJsonSource,
                        color = childRiskColorExpression(),
                        radius = const(9.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp)
                    )
                }
            }
        }

        if (state.canEditMap) {
            PaintPanel(
                selectedColor = state.activePaintColor,
                onColorSelected = viewModel::onPaintColorSelected,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )
        }

        CitySelector(
            cities = state.cities,
            activeCityId = state.activeMapCityId,
            onCitySelected = viewModel::viewCity,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        if (state.isParent && state.childLocations.isNotEmpty()) {
            ChildrenLegend(
                children = state.childLocations,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        state.error?.let { errorMsg ->
            LaunchedEffect(errorMsg) {
                delay(3000)
                viewModel.clearError()
            }
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(errorMsg)
            }
        }
    }
}

@Composable
private fun ChildrenLegend(
    children: List<ChildMapLocation>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            children.take(5).forEach { child ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(child.currentRisk.toRiskColor())
                    )
                    Text(
                        text = child.label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun String.toRiskColor(): Color = when {
    equals("Red", ignoreCase = true) -> Color(0xFFC62828)
    equals("Yellow", ignoreCase = true) -> Color(0xFFF9A825)
    equals("Green", ignoreCase = true) -> Color(0xFF2E7D32)
    else -> Color(0xFF1565C0)
}

private fun childRiskColorExpression(): Expression<ColorValue> {
    return switch(
        input = feature["risk"].convertToString(),
        case(listOf("Green", "green", "GREEN"), const(Color(0xFF2E7D32))),
        case(listOf("Yellow", "yellow", "YELLOW"), const(Color(0xFFF9A825))),
        case(listOf("Red", "red", "RED"), const(Color(0xFFC62828))),
        fallback = const(Color(0xFF1565C0))
    )
}

private const val MapBaseStyleUrl = "https://tiles.openfreemap.org/styles/bright"

private fun safetyZoneColorExpression(
    overrides: Map<String, MapAreaColor>
): Expression<ColorValue> {
    val baseRiskColor = switch(
        input = feature["risk"].convertToString(),
        case(listOf("green", "Green", "GREEN"), const(Color(0xFF4CAF50))),
        case(listOf("yellow", "Yellow", "YELLOW"), const(Color(0xFFFFEB3B))),
        case(listOf("red", "Red", "RED"), const(Color(0xFFF44336))),
        fallback = const(Color(0xFFBDBDBD))
    )

    if (overrides.isEmpty()) {
        return baseRiskColor
    }

    val overrideCases = overrides.map { (baseAreaKey, color) ->
        case(baseAreaKey, const(color.toComposeColor()))
    }.toTypedArray()

    return switch(
        input = feature["baseAreaKey"].convertToString(),
        cases = overrideCases,
        fallback = baseRiskColor
    )
}

private fun MapAreaColor.toComposeColor(): Color = when (this) {
    MapAreaColor.GREEN -> Color(0xFF4CAF50)
    MapAreaColor.YELLOW -> Color(0xFFFFEB3B)
    MapAreaColor.RED -> Color(0xFFF44336)
    MapAreaColor.NONE -> Color(0xFF4CAF50)
}

private fun MapCityBbox.centerPosition(): Position {
    return Position(
        longitude = (minLon + maxLon) / 2.0,
        latitude = (minLat + maxLat) / 2.0
    )
}

private fun MapCityBbox.defaultZoom(): Double {
    val span = maxOf(maxLon - minLon, maxLat - minLat)
    return when {
        span <= 0.03 -> 13.0
        span <= 0.08 -> 12.0
        span <= 0.2 -> 11.0
        span <= 0.5 -> 10.0
        else -> 9.0
    }
}

@Composable
fun PaintPanel(
    selectedColor: MapAreaColor?,
    onColorSelected: (MapAreaColor?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(60.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf(
                MapAreaColor.RED to Color(0xFFF44336),
                MapAreaColor.YELLOW to Color(0xFFFFEB3B),
                MapAreaColor.GREEN to Color(0xFF4CAF50),
            ).forEach { (domainColor, composeColor) ->
                val isSelected = selectedColor == domainColor
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(composeColor)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            if (isSelected) onColorSelected(null)
                            else onColorSelected(domainColor)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (domainColor == MapAreaColor.NONE) {
                        Text("X", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

            FilledIconButton(
                onClick = { onColorSelected(null) },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (selectedColor == null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedColor == null)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CitySelector(
    cities: List<MapCity>,
    activeCityId: String,
    onCitySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (cities.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val activeCity = cities.firstOrNull { it.cityId == activeCityId }
    val label = activeCity?.name ?: activeCityId

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box {
            Text(
                text = label,
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                cities.forEach { city ->
                    DropdownMenuItem(
                        text = {
                            Text(text = city.name)
                        },
                        onClick = {
                            onCitySelected(city.cityId)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
