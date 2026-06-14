package team.kid.roadsafety.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.const
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

    val hasCentered = remember { mutableStateOf(false) }
    LaunchedEffect(state.currentLocation) {
        if (!hasCentered.value && state.currentLocation != null) {
            cameraState.position = CameraPosition(
                target = Position(state.currentLocation!!.longitude, state.currentLocation!!.latitude),
                zoom = 15.0
            )
            hasCentered.value = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/bright"),
            cameraState = cameraState
        ) {
            state.tileUrl?.let { tileUrl ->
                key(tileUrl, state.overrides) {
                    val vectorSource = rememberVectorSource(
                        tiles = listOf(tileUrl),
                        options = TileSetOptions(minZoom = 12, maxZoom = 18)
                    )

                    FillLayer(
                        id = "safety-zones-layer",
                        source = vectorSource,
                        sourceLayer = "safety_zones",
                        minZoom = 12f,
                        maxZoom = 18f,
                        color = safetyZoneColorExpression(state.overrides),
                        opacity = const(0.35f),
                        onClick = { clickedFeatures ->
                            val baseAreaKey = clickedFeatures
                                .firstOrNull()
                                ?.properties
                                ?.get("base_area_key")
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
        }

        PaintPanel(
            selectedColor = state.activePaintColor,
            onColorSelected = viewModel::onPaintColorSelected,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )

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

private fun safetyZoneColorExpression(
    overrides: Map<String, MapAreaColor>
): Expression<ColorValue> {
    val baseRiskColor = switch(
        input = feature["risk"].asString(),
        case("green", const(Color(0xFF4CAF50))),
        case("yellow", const(Color(0xFFFFEB3B))),
        case("red", const(Color(0xFFF44336))),
        fallback = const(Color(0xFFBDBDBD))
    )

    if (overrides.isEmpty()) {
        return baseRiskColor
    }

    val overrideCases = overrides.map { (baseAreaKey, color) ->
        case(baseAreaKey, const(color.toComposeColor()))
    }.toTypedArray()

    return switch(
        input = feature["base_area_key"].asString(),
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
                MapAreaColor.NONE to Color(0xFFBDBDBD)
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
