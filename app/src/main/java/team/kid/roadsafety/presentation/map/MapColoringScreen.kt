package team.kid.roadsafety.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.TileSetOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.sources.rememberVectorSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position
import team.kid.roadsafety.BuildConfig
import team.kid.roadsafety.domain.aggregates.map.GeoPoint
import team.kid.roadsafety.domain.aggregates.map.MapArea
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.map.MapCityBbox
import org.maplibre.spatialk.geojson.Feature as GeoJsonFeature

@Composable
fun MapColoringScreen(
    sessionKey: String = "",
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val cameraState = rememberCameraState(
        firstPosition = state.lastCameraPosition ?: CameraPosition(
            target = Position(60.6122, 56.8519),
            zoom = 12.0
        )
    )

    LaunchedEffect(sessionKey) {
        viewModel.refreshForCurrentUser(sessionKey)
    }
    DisposableEffect(sessionKey) {
        onDispose {
            viewModel.stopScreenWork()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLocationUpdates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(cameraState, state.canTrackCameraCity) {
        if (!state.canTrackCameraCity) return@LaunchedEffect

        snapshotFlow {
            val target = cameraState.position.target
            GeoPoint(latitude = target.latitude, longitude = target.longitude)
        }
            .distinctUntilChanged()
            .collectLatest { center ->
                delay(400L)
                viewModel.viewCityForCamera(center)
            }
    }

    LaunchedEffect(cameraState, state.isCameraInitialized) {
        if (!state.isCameraInitialized) return@LaunchedEffect

        snapshotFlow { cameraState.position }
            .distinctUntilChanged()
            .collectLatest { position ->
                delay(500L)
                viewModel.updateCameraPosition(position)
            }
    }

    val currentLocationSource = remember(state.currentLocation, state.isParent) {
        if (state.currentLocation != null && !state.isParent) {
            val point = Point(
                Position(state.currentLocation!!.longitude, state.currentLocation!!.latitude)
            )
            FeatureCollection(listOf(GeoJsonFeature(point, JsonObject(emptyMap()), null)))
        } else {
            FeatureCollection(emptyList<GeoJsonFeature<Point, JsonObject>>())
        }
    }

    val visibleChildLocations = state.visibleChildLocations
    val childLocationSource = remember(visibleChildLocations) {
        if (visibleChildLocations.isNotEmpty()) {
            FeatureCollection(
                visibleChildLocations.map { child ->
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

    val customAreasSource = remember(state.customAreas) {
        customAreasFeatureCollection(state.customAreas)
    }

    val draftFillSource = remember(state.draftPoints, state.draftRisk) {
        draftPolygonFeatureCollection(state.draftPoints, state.draftRisk)
    }

    val draftLineSource = remember(state.draftPoints, state.draftRisk) {
        draftLineFeatureCollection(state.draftPoints, state.draftRisk)
    }

    val draftPointsSource = remember(state.draftPoints) {
        FeatureCollection(
            state.draftPoints.mapIndexed { index, point ->
                GeoJsonFeature(
                    Point(Position(point.longitude, point.latitude)),
                    JsonObject(mapOf("index" to JsonPrimitive(index))),
                    null
                )
            }
        )
    }

    LaunchedEffect(state.activeMapCityId, state.familyCityId, state.metadata?.bbox, state.cities) {
        if (state.lastCameraPosition != null) return@LaunchedEffect
        val bbox = state.metadata?.bbox
            ?: state.cities.firstOrNull { it.cityId == state.activeMapCityId }?.bbox
        if (bbox != null) {
            cameraState.position = CameraPosition(
                target = bbox.centerPosition(),
                zoom = bbox.defaultZoom()
            )
            viewModel.markCameraInitialized()
        }
    }

    val baseStyle = remember(state.mapStyleJson) {
        state.mapStyleJson?.let { BaseStyle.Json(it) } ?: BaseStyle.Uri(MapBaseStyleUrl)
    }

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = baseStyle,
            cameraState = cameraState,
            onMapClick = { position, _ ->
                if (state.isCreatingCustomArea && state.canEditMap) {
                    viewModel.onMapPointClicked(
                        GeoPoint(
                            latitude = position.latitude,
                            longitude = position.longitude
                        )
                    )
                    ClickResult.Consume
                } else {
                    ClickResult.Pass
                }
            }
        ) {
             state.tileUrl?.let { tileUrl ->
                key(tileUrl) {
                    val tileList = remember(tileUrl) { listOf(tileUrl) }
                    val tileOptions = remember { TileSetOptions(minZoom = 9, maxZoom = 18) }
                    val vectorSource = rememberVectorSource(
                        tiles = tileList,
                        options = tileOptions
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

            val customGeoJsonData = remember(customAreasSource) {
                customAreasSource.toSafeGeoJsonData()
            }
            val customGeoJsonSource = rememberGeoJsonSource(
                data = customGeoJsonData
            )
            FillLayer(
                id = "custom-safety-zones-layer",
                source = customGeoJsonSource,
                color = customAreaColorExpression(),
                opacity = const(0.45f),
                outlineColor = const(Color(0xFF263238)),
                onClick = { clickedFeatures ->
                    val areaId = clickedFeatures
                        .firstOrNull()
                        ?.properties
                        ?.get("id")
                        ?.jsonPrimitive
                        ?.contentOrNull

                    if (areaId != null && state.isEraseMode) {
                        viewModel.onCustomAreaClicked(areaId)
                        ClickResult.Consume
                    } else {
                        ClickResult.Pass
                    }
                }
            )

            val draftGeoJsonData = remember(draftFillSource) {
                draftFillSource.toSafeGeoJsonData()
            }
            val draftGeoJsonSource = rememberGeoJsonSource(
                data = draftGeoJsonData
            )
            FillLayer(
                id = "draft-custom-zone-fill",
                source = draftGeoJsonSource,
                color = const(state.draftRisk.toComposeColor()),
                opacity = const(0.28f),
                outlineColor = const(state.draftRisk.toComposeColor())
            )

            val draftLineGeoJsonData = remember(draftLineSource) {
                draftLineSource.toSafeGeoJsonData()
            }
            val draftLineGeoJsonSource = rememberGeoJsonSource(
                data = draftLineGeoJsonData
            )
            LineLayer(
                id = "draft-custom-zone-line",
                source = draftLineGeoJsonSource,
                color = const(state.draftRisk.toComposeColor()),
                width = const(3.dp)
            )

            val draftPointGeoJsonData = remember(draftPointsSource) {
                draftPointsSource.toSafeGeoJsonData()
            }
            val draftPointGeoJsonSource = rememberGeoJsonSource(
                data = draftPointGeoJsonData
            )
            CircleLayer(
                id = "draft-custom-zone-points",
                source = draftPointGeoJsonSource,
                color = const(Color.White),
                radius = const(5.dp),
                strokeColor = const(state.draftRisk.toComposeColor()),
                strokeWidth = const(2.dp)
            )

            val locationGeoJsonData = remember(currentLocationSource) {
                currentLocationSource.toSafeGeoJsonData()
            }
            val locationGeoJsonSource = rememberGeoJsonSource(
                data = locationGeoJsonData
            )
            CircleLayer(
                id = "current-location-marker",
                source = locationGeoJsonSource,
                color = const(Color.Blue),
                radius = const(8.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp)
            )

            val childrenGeoJsonData = remember(childLocationSource) {
                childLocationSource.toSafeGeoJsonData()
            }
            val childrenGeoJsonSource = rememberGeoJsonSource(
                data = childrenGeoJsonData
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

        if (state.canEditMap) {
            ZoneTargetDropdown(
                targets = state.zoneTargets,
                selectedTarget = state.selectedZoneTarget,
                onTargetSelected = viewModel::onZoneTargetSelected,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )

            PaintPanel(
                selectedColor = state.activePaintColor,
                isEraseMode = state.isEraseMode,
                onColorSelected = viewModel::onPaintColorSelected,
                onEraseModeToggled = viewModel::onEraseModeToggled,
                onCreateCustomArea = viewModel::startCustomAreaDraft,
                isCreatingCustomArea = state.isCreatingCustomArea,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )

            if (state.isCreatingCustomArea) {
                CustomAreaDraftPanel(
                    targetLabel = state.selectedZoneTarget.label,
                    selectedRisk = state.draftRisk,
                    pointCount = state.draftPoints.distinct().size,
                    canSave = state.canSaveCustomArea,
                    isSaving = state.isSavingCustomArea,
                    onRiskSelected = viewModel::onDraftRiskSelected,
                    onUndo = viewModel::undoDraftPoint,
                    onCancel = viewModel::cancelCustomAreaDraft,
                    onSave = viewModel::saveCustomAreaDraft,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ),
            shape = CircleShape
        ) {
            IconButton(
                onClick = viewModel::forceRefresh,
                enabled = !state.isLoading,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Обновить карту"
                )
            }
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

private fun childRiskColorExpression(): Expression<ColorValue> {
    return switch(
        input = feature["risk"].convertToString(),
        case(listOf("Green", "green", "GREEN"), const(Color(0xFF2E7D32))),
        case(listOf("Yellow", "yellow", "YELLOW"), const(Color(0xFFF9A825))),
        case(listOf("Red", "red", "RED"), const(Color(0xFFC62828))),
        fallback = const(Color(0xFF1565C0))
    )
}

private fun customAreaColorExpression(): Expression<ColorValue> {
    return switch(
        input = feature["risk"].convertToString(),
        case(listOf("GREEN"), const(MapAreaColor.GREEN.toComposeColor())),
        case(listOf("YELLOW"), const(MapAreaColor.YELLOW.toComposeColor())),
        case(listOf("RED"), const(MapAreaColor.RED.toComposeColor())),
        fallback = const(Color(0xFF607D8B))
    )
}

private fun customAreasFeatureCollection(areas: List<MapArea>): FeatureCollection<Polygon, JsonObject> {
    return FeatureCollection(
        areas.mapNotNull { area ->
            val ring = area.points.toClosedPositions()
            if (ring.size < 4) return@mapNotNull null
            GeoJsonFeature(
                Polygon(listOf(ring)),
                JsonObject(
                    mapOf(
                        "id" to JsonPrimitive(area.id.value.toString()),
                        "risk" to JsonPrimitive(area.color.name)
                    )
                ),
                null
            )
        }
    )
}

private fun draftPolygonFeatureCollection(
    points: List<GeoPoint>,
    risk: MapAreaColor
): FeatureCollection<Polygon, JsonObject> {
    val ring = points.toClosedPositions()
    return if (ring.size >= 4) {
        FeatureCollection(
            listOf(
                GeoJsonFeature(
                    Polygon(listOf(ring)),
                    JsonObject(mapOf("risk" to JsonPrimitive(risk.name))),
                    null
                )
            )
        )
    } else {
        FeatureCollection(emptyList<GeoJsonFeature<Polygon, JsonObject>>())
    }
}

private fun draftLineFeatureCollection(
    points: List<GeoPoint>,
    risk: MapAreaColor
): FeatureCollection<LineString, JsonObject> {
    return if (points.size >= 2) {
        FeatureCollection(
            listOf(
                GeoJsonFeature(
                    LineString(points.map { Position(it.longitude, it.latitude) }),
                    JsonObject(mapOf("risk" to JsonPrimitive(risk.name))),
                    null
                )
            )
        )
    } else {
        FeatureCollection(emptyList<GeoJsonFeature<LineString, JsonObject>>())
    }
}

private fun List<GeoPoint>.toClosedPositions(): List<Position> {
    if (isEmpty()) return emptyList()
    val closed = if (first() == last()) this else this + first()
    return closed.map { Position(it.longitude, it.latitude) }
}

private val MapBaseStyleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=${BuildConfig.MAPTILER_KEY}"

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

private fun FeatureCollection<*, *>.toSafeGeoJsonData(): GeoJsonData {
    return if (this.isEmpty()) {
        GeoJsonData.JsonString("""{"type":"FeatureCollection","features":[]}""")
    } else {
        GeoJsonData.Features(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneTargetDropdown(
    targets: List<ZoneTarget>,
    selectedTarget: ZoneTarget,
    onTargetSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.width(220.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedTarget.label,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Зоны") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                targets.forEach { target ->
                    DropdownMenuItem(
                        text = { Text(target.label) },
                        onClick = {
                            expanded = false
                            onTargetSelected(target.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PaintPanel(
    selectedColor: MapAreaColor?,
    isEraseMode: Boolean,
    onColorSelected: (MapAreaColor?) -> Unit,
    onEraseModeToggled: () -> Unit,
    onCreateCustomArea: () -> Unit,
    isCreatingCustomArea: Boolean,
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

            FilledIconButton(
                onClick = onEraseModeToggled,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isEraseMode)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Удалить или сбросить зону",
                    tint = if (isEraseMode)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledIconButton(
                onClick = onCreateCustomArea,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isCreatingCustomArea)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.AddLocationAlt,
                    contentDescription = "Новая зона",
                    tint = if (isCreatingCustomArea)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CustomAreaDraftPanel(
    targetLabel: String,
    selectedRisk: MapAreaColor,
    pointCount: Int,
    canSave: Boolean,
    isSaving: Boolean,
    onRiskSelected: (MapAreaColor) -> Unit,
    onUndo: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = targetLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "Точек: $pointCount",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledIconButton(
                        onClick = onUndo,
                        enabled = pointCount > 0,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Undo, contentDescription = "Отменить точку")
                    }
                    FilledIconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Отменить")
                    }
                    FilledIconButton(
                        onClick = onSave,
                        enabled = canSave && !isSaving,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Сохранить")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MapAreaColor.RED, MapAreaColor.YELLOW, MapAreaColor.GREEN).forEach { risk ->
                    TextButton(
                        onClick = { onRiskSelected(risk) },
                        modifier = Modifier
                            .border(
                                width = if (selectedRisk == risk) 2.dp else 1.dp,
                                color = if (selectedRisk == risk)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(risk.toComposeColor())
                        )
                    }
                }
            }
        }
    }
}
