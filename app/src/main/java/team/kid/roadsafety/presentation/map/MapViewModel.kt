package team.kid.roadsafety.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.kid.roadsafety.domain.aggregates.map.MapArea
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.map.MapRepository
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAreas()
    }

    fun loadAreas(cityId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            mapRepository.getAreas(cityId)
                .fold(
                    onSuccess = { areas ->
                        val finalAreas = areas.ifEmpty { getMockAreas() }
                        _uiState.update { it.copy(isLoading = false, areas = finalAreas) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, areas = getMockAreas(), error = "Using mock data: ${error.message}") }
                    }
                )
        }
    }

    private fun getMockAreas(): List<MapArea> {
        val baseLat = 55.7558
        val baseLon = 37.6173
        val size = 0.005 // Size of each square

        return listOf(
            // Top Row
            createMockArea(baseLat + size, baseLon, size, MapAreaColor.RED),
            createMockArea(baseLat + size, baseLon + size, size, MapAreaColor.YELLOW),
            createMockArea(baseLat + size, baseLon + (size * 2), size, MapAreaColor.GREEN),
            
            // Bottom Row
            createMockArea(baseLat, baseLon, size, MapAreaColor.NONE),
            createMockArea(baseLat, baseLon + size, size, MapAreaColor.NONE),
            createMockArea(baseLat, baseLon + (size * 2), size, MapAreaColor.YELLOW)
        )
    }

    private fun createMockArea(lat: Double, lon: Double, size: Double, color: MapAreaColor): MapArea {
        return MapArea(
            id = team.kid.roadsafety.domain.AreaId(java.util.UUID.randomUUID()),
            osmId = null,
            color = color,
            points = listOf(
                team.kid.roadsafety.domain.aggregates.map.GeoPoint(lat, lon),
                team.kid.roadsafety.domain.aggregates.map.GeoPoint(lat, lon + size),
                team.kid.roadsafety.domain.aggregates.map.GeoPoint(lat + size, lon + size),
                team.kid.roadsafety.domain.aggregates.map.GeoPoint(lat + size, lon),
                team.kid.roadsafety.domain.aggregates.map.GeoPoint(lat, lon)
            ),
            cityId = null
        )
    }

    fun onPaintColorSelected(color: MapAreaColor?) {
        _uiState.update { it.copy(activePaintColor = color) }
    }

    fun onAreaClicked(area: MapArea) {
        val paintColor = _uiState.value.activePaintColor
        if (paintColor != null) {
            updateAreaColor(area, paintColor)
        }
    }

    private fun updateAreaColor(area: MapArea, color: MapAreaColor) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = mapRepository.updateAreaColor(area.id.value.toString(), color)
            
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            areas = state.areas.map { 
                                if (it.id == area.id) it.copy(color = color) else it 
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            areas = state.areas.map { 
                                if (it.id == area.id) it.copy(color = color) else it 
                            },
                            error = "Note: Local update only (API Error: ${error.message})"
                        )
                    }
                }
            )
        }
    }

    fun dismissAreaSelection() {
        _uiState.update { it.copy(activePaintColor = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class MapUiState(
    val areas: List<MapArea> = emptyList(),
    val activePaintColor: MapAreaColor? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
