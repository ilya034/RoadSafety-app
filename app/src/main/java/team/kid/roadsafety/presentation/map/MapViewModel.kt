package team.kid.roadsafety.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.kid.roadsafety.BuildConfig
import team.kid.roadsafety.domain.aggregates.family.FamilyRepository
import team.kid.roadsafety.domain.aggregates.map.GeoPoint
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.map.MapCityMetadata
import team.kid.roadsafety.domain.aggregates.map.MapRepository
import team.kid.roadsafety.domain.aggregates.user.AuthRepository
import team.kid.roadsafety.infrastructure.location.LocationObserver
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val familyRepository: FamilyRepository,
    private val authRepository: AuthRepository,
    private val locationObserver: LocationObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState = _uiState.asStateFlow()

    init {
        initializeMap()
        observeLocationUpdates()
    }

    private fun initializeMap() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val userResult = authRepository.getCurrentUser()
            val familyId = userResult.getOrNull()?.familyId
            val cityId = familyRepository.getSelectedCityId() ?: DefaultCityId

            _uiState.update { it.copy(familyId = familyId, cityId = cityId) }
            loadCity(cityId, familyId)
        }
    }

    private suspend fun loadCity(cityId: String, familyId: String?) {
        val metadataResult = mapRepository.getCityMetadata(cityId)
        metadataResult.fold(
            onSuccess = { metadata ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cityId = cityId,
                        metadata = metadata,
                        tileUrl = buildTileUrl(cityId, metadata.generationVersion),
                        error = null
                    )
                }
            },
            onFailure = { error ->
                val generationVersion = "fallback"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cityId = cityId,
                        metadata = null,
                        tileUrl = buildTileUrl(cityId, generationVersion),
                        error = error.message ?: "Failed to load city metadata"
                    )
                }
            }
        )

        if (familyId != null) {
            loadOverrides(familyId)
        }
    }

    private fun observeLocationUpdates() {
        viewModelScope.launch {
            try {
                locationObserver.observeLocation(5000L).collect { location ->
                    val point = GeoPoint(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    _uiState.update { it.copy(currentLocation = point) }
                }
            } catch (e: Exception) {
                // Permissions might not be granted, or user is not a child. Ignore gracefully.
            }
        }
    }

    private suspend fun loadOverrides(familyId: String) {
        mapRepository.getUserAreas(familyId)
            .fold(
                onSuccess = { areas ->
                    _uiState.update {
                        it.copy(
                            overrides = areas
                                .mapNotNull { area -> area.baseAreaKey?.let { key -> key to area.color } }
                                .toMap(),
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to load map overrides")
                    }
                }
            )
    }

    fun onPaintColorSelected(color: MapAreaColor?) {
        _uiState.update { it.copy(activePaintColor = color) }
    }

    fun onBaseAreaClicked(baseAreaKey: String) {
        val paintColor = _uiState.value.activePaintColor ?: return
        updateBaseAreaColor(baseAreaKey, paintColor)
    }

    private fun updateBaseAreaColor(baseAreaKey: String, color: MapAreaColor) {
        viewModelScope.launch {
            val familyId = _uiState.value.familyId
            if (familyId == null) {
                _uiState.update { it.copy(error = "Family ID not found") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = mapRepository.updateBaseAreaColor(baseAreaKey, familyId, color)

            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            overrides = state.overrides + (baseAreaKey to color)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            overrides = state.overrides + (baseAreaKey to color),
                            error = "Note: Local update only (API Error: ${error.message})"
                        )
                    }
                }
            )
        }
    }

    fun changeCity(cityId: String) {
        viewModelScope.launch {
            val familyId = _uiState.value.familyId
            if (familyId == null) {
                familyRepository.setSelectedCityId(cityId)
                _uiState.update { it.copy(overrides = emptyMap()) }
                loadCity(cityId, null)
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = familyRepository.updateFamilyCity(
                familyId = team.kid.roadsafety.domain.FamilyId(java.util.UUID.fromString(familyId)),
                cityId = cityId
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(overrides = emptyMap()) }
                    loadCity(cityId, familyId)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to update family city")
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

    private fun buildTileUrl(cityId: String, generationVersion: String): String {
        val apiRoot = BuildConfig.BASE_URL.trimEnd('/')
        return "$apiRoot/maps/tiles/$cityId/{z}/{x}/{y}.pbf?v=$generationVersion"
    }

    private companion object {
        const val DefaultCityId = "ekb"
    }
}

data class MapUiState(
    val cityId: String = "ekb",
    val familyId: String? = null,
    val metadata: MapCityMetadata? = null,
    val tileUrl: String? = null,
    val overrides: Map<String, MapAreaColor> = emptyMap(),
    val activePaintColor: MapAreaColor? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentLocation: GeoPoint? = null
)
