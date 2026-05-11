package team.kid.roadsafety.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.*
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.*
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature as GeoJsonFeature
import org.maplibre.spatialk.geojson.*

@Composable
fun MapColoringScreen(modifier: Modifier = Modifier) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(37.6173, 55.7558), // Moscow center
            zoom = 12.0
        )
    )
    
    // Initial dummy data
    val initialFeatures = remember {
        listOf(
            createBlock("block1", listOf(
                Position(37.61, 55.75),
                Position(37.62, 55.75),
                Position(37.62, 55.76),
                Position(37.61, 55.76),
                Position(37.61, 55.75)
            ), Color.Gray),
            createBlock("block2", listOf(
                Position(37.62, 55.75),
                Position(37.63, 55.75),
                Position(37.63, 55.76),
                Position(37.62, 55.76),
                Position(37.62, 55.75)
            ), Color.Gray),
            // A "nested" or smaller block inside a larger one
            createBlock("small_block", listOf(
                Position(37.612, 55.752),
                Position(37.618, 55.752),
                Position(37.618, 55.758),
                Position(37.612, 55.758),
                Position(37.612, 55.752)
            ), Color.LightGray)
        )
    }

    var features by remember { mutableStateOf(initialFeatures) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
            cameraState = cameraState
        ) {
            val geoJsonSource = rememberGeoJsonSource(
                data = GeoJsonData.Features(FeatureCollection(features))
            )

            // Fill layer with color based on property
            FillLayer(
                id = "blocks-fill",
                source = geoJsonSource,
                // Using Expression DSL to get color from feature property
                color = feature["color"].convertToColor(),
                opacity = const(0.6f),
                onClick = { clickedFeatures ->
                    val clickedFeature = clickedFeatures.firstOrNull()
                    val id = clickedFeature?.id?.jsonPrimitive?.content
                    if (id != null) {
                        selectedIds = if (selectedIds.contains(id)) {
                            selectedIds - id
                        } else {
                            selectedIds + id
                        }
                        ClickResult.Consume
                    } else {
                        ClickResult.Pass
                    }
                }
            )

            // Outline layer
            LineLayer(
                id = "blocks-outline",
                source = geoJsonSource,
                color = const(Color.Black),
                width = const(2.dp)
            )
        }

        // UI Overlay for color selection
        if (selectedIds.isNotEmpty()) {
            ColorPickerOverlay(
                onColorSelected = { color ->
                    val colorHex = "#" + Integer.toHexString(color.toArgb()).substring(2)
                    features = features.map { f ->
                        val idStr = f.id?.jsonPrimitive?.content
                        if (idStr != null && selectedIds.contains(idStr)) {
                            val newProperties = f.properties.toMutableMap()
                            newProperties["color"] = JsonPrimitive(colorHex)
                            f.copy(properties = JsonObject(newProperties))
                        } else {
                            f
                        }
                    }
                    selectedIds = emptySet()
                },
                onClearSelection = {
                    selectedIds = emptySet()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun ColorPickerOverlay(
    onColorSelected: (Color) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Select color for blocks", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Gray).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onColorSelected(color) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onClearSelection) {
                Text("Cancel")
            }
        }
    }
}

fun createBlock(id: String, points: List<Position>, color: Color): GeoJsonFeature<Polygon, JsonObject> {
    val polygon = Polygon(listOf(points))
    val colorHex = "#" + Integer.toHexString(color.toArgb()).substring(2)
    val properties = JsonObject(mapOf("color" to JsonPrimitive(colorHex)))
    return GeoJsonFeature(polygon, properties, JsonPrimitive(id))
}
