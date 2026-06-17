package team.kid.roadsafety.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import team.kid.roadsafety.domain.FamilyId
import team.kid.roadsafety.domain.UserId
import team.kid.roadsafety.BuildConfig
import team.kid.roadsafety.domain.aggregates.family.FamilyMemberEntity
import team.kid.roadsafety.domain.aggregates.family.FamilyRepository
import team.kid.roadsafety.domain.aggregates.map.GeoPoint
import team.kid.roadsafety.domain.aggregates.map.MapArea
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.map.MapCity
import team.kid.roadsafety.domain.aggregates.map.MapCityBbox
import team.kid.roadsafety.domain.aggregates.map.MapCityMetadata
import team.kid.roadsafety.domain.aggregates.map.MapRepository
import team.kid.roadsafety.domain.aggregates.tracking.TrackingRepository
import team.kid.roadsafety.domain.aggregates.user.AuthRepository
import team.kid.roadsafety.domain.aggregates.user.UserRole
import team.kid.roadsafety.infrastructure.NetworkMonitor
import team.kid.roadsafety.infrastructure.location.LocationObserver
import team.kid.roadsafety.infrastructure.map.MapTileCacheService
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val familyRepository: FamilyRepository,
    private val authRepository: AuthRepository,
    private val trackingRepository: TrackingRepository,
    private val locationObserver: LocationObserver,
    private val networkMonitor: NetworkMonitor,
    private val mapTileCacheService: MapTileCacheService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState = _uiState.asStateFlow()

    private var initializationJob: Job? = null
    private var childrenLocationPollingJob: Job? = null
    private var alertZonesSyncJob: Job? = null
    private var currentLocationJob: Job? = null
    private var cityLoadJob: Job? = null
    private var requestedMapCityId: String? = null

    fun refreshForCurrentUser() {
        initializationJob?.cancel()
        initializationJob = viewModelScope.launch {
            initializeMap()
        }
    }

    fun stopScreenWork() {
        initializationJob?.cancel()
        initializationJob = null
        childrenLocationPollingJob?.cancel()
        childrenLocationPollingJob = null
        alertZonesSyncJob?.cancel()
        alertZonesSyncJob = null
        currentLocationJob?.cancel()
        currentLocationJob = null
        cityLoadJob?.cancel()
        cityLoadJob = null
    }

    private suspend fun initializeMap() {
        childrenLocationPollingJob?.cancel()
        childrenLocationPollingJob = null
        alertZonesSyncJob?.cancel()
        alertZonesSyncJob = null
        requestedMapCityId = null

        _uiState.update {
            MapUiState(
                currentLocation = if (it.isParent) null else it.currentLocation,
                isLoading = true
            )
        }

        val userResult = authRepository.getCurrentUser()
        val currentUser = userResult.getOrNull()
        val familyId = currentUser?.familyId
        val isParent = UserRole.fromString(currentUser?.familyRole) == UserRole.PARENT
        val familyCityId = familyRepository.getSelectedCityId() ?: DefaultCityId
        val cities = familyRepository.getSupportedCities().getOrDefault(listOf(DefaultCity))
        val activeCity = cities.firstOrNull { it.cityId == familyCityId } ?: cities.firstOrNull() ?: DefaultCity

        _uiState.update {
            it.copy(
                familyId = familyId,
                isParent = isParent,
                familyCityId = familyCityId,
                activeMapCityId = activeCity.cityId,
                cities = cities
            )
        }
        if (familyId != null && isParent) {
            loadZoneTargets(familyId)
        }
        if (isParent) {
            stopCurrentLocationUpdates()
            startChildrenLocationPolling()
        } else {
            startCurrentLocationUpdates()
        }
        loadCity(activeCity.cityId, familyId)
    }

    private suspend fun loadZoneTargets(familyId: String) {
        familyRepository.getFamilyMembers(FamilyId(UUID.fromString(familyId)))
            .fold(
                onSuccess = { members ->
                    val childTargets = members
                        .filter { it.role == UserRole.CHILD }
                        .map { member -> member.toZoneTarget(_uiState.value.childLocations) }
                    _uiState.update { state ->
                        val targets = listOf(ZoneTarget.AllFamily) + childTargets
                        state.copy(
                            zoneTargets = targets,
                            selectedZoneTarget = targets.firstOrNull { target ->
                                target.id == state.selectedZoneTarget.id
                            } ?: ZoneTarget.AllFamily,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to load children")
                    }
                }
            )
    }

    private suspend fun loadCity(cityId: String, familyId: String?) {
        val styleJob = viewModelScope.launch {
            val styleJson = mapTileCacheService.getStyleJsonForCity(cityId)
            _uiState.update { it.copy(mapStyleJson = styleJson) }
        }

        loadCachedCity(cityId, familyId)
        styleJob.join()

        val isOnline = networkMonitor.isOnline()
        _uiState.update { it.copy(isOnline = isOnline) }
        if (!isOnline) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        val metadataResult = mapRepository.getCityMetadata(cityId)
        metadataResult.fold(
            onSuccess = { metadata ->
                val tileUrl = buildTileUrl(cityId, metadata.generationVersion)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeMapCityId = cityId,
                        metadata = metadata,
                        tileUrl = tileUrl,
                        error = null
                    )
                }
                refreshTileCache(cityId, metadata, tileUrl)
            },
            onFailure = { error ->
                val generationVersion = "fallback"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        activeMapCityId = cityId,
                        metadata = null,
                        tileUrl = buildTileUrl(cityId, generationVersion),
                        error = error.message ?: "Failed to load city metadata"
                    )
                }
            }
        )

        if (familyId != null) {
            loadOverrides(familyId)
            syncAlertZonesInBackground(
                cityId = cityId,
                familyId = familyId,
                childId = _uiState.value.selectedZoneTarget.childUserId()
            )
        } else {
            _uiState.update { it.copy(overrides = emptyMap(), customAreas = emptyList()) }
        }
    }

    private fun startCurrentLocationUpdates() {
        if (currentLocationJob?.isActive == true) return

        currentLocationJob = viewModelScope.launch {
            try {
                locationObserver.getLastKnownLocation()?.let { location ->
                    _uiState.update {
                        it.copy(
                            currentLocation = GeoPoint(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        )
                    }
                }
                locationObserver.observeLocation(5000L).collect { location ->
                    val point = GeoPoint(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    _uiState.update { it.copy(currentLocation = point) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Нет доступа к местоположению") }
            }
        }
    }

    private fun stopCurrentLocationUpdates() {
        currentLocationJob?.cancel()
        currentLocationJob = null
        _uiState.update { it.copy(currentLocation = null) }
    }

    private fun startChildrenLocationPolling() {
        childrenLocationPollingJob?.cancel()
        childrenLocationPollingJob = viewModelScope.launch {
            while (true) {
                trackingRepository.getChildrenLocations()
                    .fold(
                        onSuccess = { response ->
                            val childLocations = response.children
                                .map { child ->
                                    ChildMapLocation(
                                        childId = child.childId,
                                        displayName = child.displayName,
                                        point = GeoPoint(child.latitude, child.longitude),
                                        currentRisk = child.currentRisk.name,
                                        lastUpdatedAt = child.lastUpdatedAt
                                    )
                                }
                                .latestByChildId()
                            _uiState.update {
                                val refreshedTargets = it.zoneTargets.refreshChildLabels(childLocations)
                                it.copy(
                                    childLocations = childLocations,
                                    zoneTargets = refreshedTargets,
                                    selectedZoneTarget = refreshedTargets.firstOrNull { target ->
                                        target.id == it.selectedZoneTarget.id
                                    } ?: it.selectedZoneTarget,
                                    error = null
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(error = error.message ?: "Failed to load children locations")
                            }
                        }
                    )

                delay(15_000L)
            }
        }
    }

    private suspend fun loadOverrides(
        familyId: String,
        childId: UserId? = _uiState.value.selectedZoneTarget.childUserId()
    ) {
        mapRepository.getUserAreas(familyId, childId)
            .fold(
                onSuccess = { areas ->
                    if (_uiState.value.selectedZoneTarget.childUserId() != childId) return@fold
                    _uiState.update {
                        it.copy(
                            overrides = areas
                                .filter { area -> area.baseAreaKey != null }
                                .mapNotNull { area -> area.baseAreaKey?.let { key -> key to area.color } }
                                .toMap(),
                            customAreas = areas.filter { area ->
                                area.baseAreaKey == null && area.points.isNotEmpty()
                            },
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    if (_uiState.value.selectedZoneTarget.childUserId() != childId) return@fold
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to load map overrides")
                    }
                }
            )
    }

    private fun applyCachedOverrides(familyId: String, childId: UserId?): Boolean {
        val cachedAreas = mapRepository.getCachedUserAreas(familyId, childId) ?: return false
        _uiState.update {
            it.copy(
                overrides = cachedAreas
                    .filter { area -> area.baseAreaKey != null }
                    .mapNotNull { area -> area.baseAreaKey?.let { key -> key to area.color } }
                    .toMap(),
                customAreas = cachedAreas.filter { area ->
                    area.baseAreaKey == null && area.points.isNotEmpty()
                },
                error = null
            )
        }
        return true
    }

    private fun syncAlertZonesInBackground(cityId: String, familyId: String, childId: UserId?) {
        alertZonesSyncJob?.cancel()
        alertZonesSyncJob = viewModelScope.launch {
            syncAlertZones(cityId, familyId, childId)
        }
    }

    private suspend fun syncAlertZones(
        cityId: String,
        familyId: String,
        childId: UserId? = _uiState.value.selectedZoneTarget.childUserId()
    ) {
        mapRepository.getAlertZones(cityId, familyId, childId)
            .onFailure { error ->
                if (_uiState.value.selectedZoneTarget.childUserId() != childId) return@onFailure
                _uiState.update {
                    it.copy(error = error.message ?: "Failed to sync alert zones")
                }
            }
    }

    private fun loadCachedCity(cityId: String, familyId: String?) {
        val cachedMetadata = mapRepository.getCachedCityMetadata(cityId)
        val cachedAreas = familyId
            ?.let { mapRepository.getCachedUserAreas(it, _uiState.value.selectedZoneTarget.childUserId()) }
        val cachedOverrides = cachedAreas
            ?.mapNotNull { area -> area.baseAreaKey?.let { key -> key to area.color } }
            ?.toMap()
        val cachedCustomAreas = cachedAreas
            ?.filter { area -> area.baseAreaKey == null && area.points.isNotEmpty() }

        if (cachedMetadata != null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeMapCityId = cityId,
                    metadata = cachedMetadata,
                    tileUrl = buildTileUrl(cityId, cachedMetadata.generationVersion),
                    overrides = cachedOverrides ?: it.overrides,
                    customAreas = cachedCustomAreas ?: it.customAreas,
                    error = null
                )
            }
        } else if (cachedOverrides != null && _uiState.value.activeMapCityId == cityId) {
            _uiState.update {
                it.copy(
                    overrides = cachedOverrides,
                    customAreas = cachedCustomAreas ?: it.customAreas,
                    error = null
                )
            }
        }
    }

    private fun refreshTileCache(cityId: String, metadata: MapCityMetadata, tileUrl: String) {
        viewModelScope.launch {
            mapTileCacheService.refreshMapCache(
                cityId = cityId,
                generationVersion = metadata.generationVersion,
                bbox = metadata.bbox,
                styleUrl = BaseMapStyleUrl,
                tileUrlTemplate = tileUrl
            )
        }
    }

    fun onPaintColorSelected(color: MapAreaColor?) {
        if (!_uiState.value.canEditMap) return

        _uiState.update {
            it.copy(
                activePaintColor = color,
                isEraseMode = false,
                isCreatingCustomArea = false,
                draftPoints = emptyList()
            )
        }
    }

    fun onEraseModeToggled() {
        if (!_uiState.value.canEditMap) return

        _uiState.update {
            it.copy(
                isEraseMode = !it.isEraseMode,
                activePaintColor = null,
                isCreatingCustomArea = false,
                draftPoints = emptyList()
            )
        }
    }

    fun onBaseAreaClicked(baseAreaKey: String) {
        if (!_uiState.value.canEditMap) return

        if (_uiState.value.isEraseMode) {
            resetBaseAreaColor(baseAreaKey)
            return
        }

        val paintColor = _uiState.value.activePaintColor ?: return
        updateBaseAreaColor(baseAreaKey, paintColor)
    }

    fun onCustomAreaClicked(areaId: String) {
        val state = _uiState.value
        if (!state.canEditMap || !state.isEraseMode) return

        val area = state.customAreas.firstOrNull { it.id.value.toString() == areaId } ?: return
        deleteCustomArea(area)
    }

    private fun updateBaseAreaColor(baseAreaKey: String, color: MapAreaColor) {
        viewModelScope.launch {
            val familyId = _uiState.value.familyId
            if (familyId == null) {
                _uiState.update { it.copy(error = "Family ID not found") }
                return@launch
            }

            val childId = _uiState.value.selectedZoneTarget.childUserId()
            if (childId != null && !networkMonitor.isOnline()) {
                _uiState.update { it.copy(isOnline = false, error = "Network required to update a child zone") }
                return@launch
            }

            val previousColor = _uiState.value.overrides[baseAreaKey]
            _uiState.update {
                it.copy(
                    error = null,
                    overrides = it.overrides + (baseAreaKey to color)
                )
            }
            val result = mapRepository.updateBaseAreaColor(baseAreaKey, familyId, color, childId)

            result.fold(
                onSuccess = {
                    refreshMapEditsInBackground(familyId, childId)
                },
                onFailure = { error ->
                    if (childId == null) {
                        _uiState.update { state ->
                            state.copy(
                                error = "Note: Local update only (API Error: ${error.message})"
                            )
                        }
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                overrides = previousColor?.let {
                                    state.overrides + (baseAreaKey to it)
                                } ?: state.overrides - baseAreaKey,
                                error = error.message ?: "Failed to update child zone"
                            )
                        }
                    }
                }
            )
        }
    }

    private fun resetBaseAreaColor(baseAreaKey: String) {
        viewModelScope.launch {
            val familyId = _uiState.value.familyId
            if (familyId == null) {
                _uiState.update { it.copy(error = "Family ID not found") }
                return@launch
            }

            if (!networkMonitor.isOnline()) {
                _uiState.update { it.copy(isOnline = false, error = "Network required to reset a zone") }
                return@launch
            }

            val childId = _uiState.value.selectedZoneTarget.childUserId()
            val previousColor = _uiState.value.overrides[baseAreaKey]
            _uiState.update {
                it.copy(
                    isOnline = true,
                    error = null,
                    overrides = it.overrides - baseAreaKey
                )
            }

            mapRepository.resetBaseAreaColor(baseAreaKey, familyId, childId)
                .fold(
                    onSuccess = {
                        refreshMapEditsInBackground(familyId, childId)
                    },
                    onFailure = { error ->
                        _uiState.update { state ->
                            state.copy(
                                overrides = previousColor?.let { color ->
                                    state.overrides + (baseAreaKey to color)
                                } ?: state.overrides,
                                error = error.message ?: "Failed to reset zone"
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
                _uiState.update { it.copy(familyCityId = cityId, overrides = emptyMap(), customAreas = emptyList()) }
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
                    _uiState.update { it.copy(familyCityId = cityId, overrides = emptyMap(), customAreas = emptyList()) }
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

    fun viewCity(cityId: String) {
        if (cityId == _uiState.value.activeMapCityId || cityId == requestedMapCityId) return
        requestedMapCityId = cityId

        cityLoadJob?.cancel()
        cityLoadJob = viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        activePaintColor = null,
                        isEraseMode = false,
                        isCreatingCustomArea = false,
                        draftPoints = emptyList(),
                        error = null
                    )
                }
                loadCity(cityId, _uiState.value.familyId)
            } finally {
                if (requestedMapCityId == cityId) {
                    requestedMapCityId = null
                }
            }
        }
    }

    fun viewCityForCamera(center: GeoPoint) {
        val state = _uiState.value
        val city = state.cities.firstOrNull { city ->
            city.bbox?.contains(center) == true
        } ?: return

        viewCity(city.cityId)
    }

    fun dismissAreaSelection() {
        _uiState.update {
            it.copy(
                activePaintColor = null,
                isEraseMode = false,
                isCreatingCustomArea = false,
                draftPoints = emptyList()
            )
        }
    }

    fun onZoneTargetSelected(targetId: String) {
        val state = _uiState.value
        if (!state.canEditMap || targetId == state.selectedZoneTarget.id) return
        val target = state.zoneTargets.firstOrNull { it.id == targetId } ?: return
        val childId = target.childUserId()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedZoneTarget = target,
                    activePaintColor = null,
                    isEraseMode = false,
                    isCreatingCustomArea = false,
                    draftPoints = emptyList(),
                    error = null
                )
            }
            val familyId = _uiState.value.familyId
            if (familyId != null) {
                val hadCachedOverrides = applyCachedOverrides(familyId, childId)
                if (!hadCachedOverrides) {
                    _uiState.update { it.copy(overrides = emptyMap(), customAreas = emptyList()) }
                }
                loadOverrides(familyId, childId)
                syncAlertZonesInBackground(_uiState.value.activeMapCityId, familyId, childId)
            }
        }
    }

    fun startCustomAreaDraft() {
        if (!_uiState.value.canEditMap) return

        val isOnline = networkMonitor.isOnline()
        _uiState.update {
            it.copy(
                isCreatingCustomArea = true,
                activePaintColor = null,
                isEraseMode = false,
                draftPoints = emptyList(),
                draftRisk = MapAreaColor.YELLOW,
                isOnline = isOnline,
                error = null
            )
        }
    }

    fun onMapPointClicked(point: GeoPoint) {
        val state = _uiState.value
        if (!state.canEditMap || state.isEraseMode || !state.isCreatingCustomArea) return

        _uiState.update { it.copy(draftPoints = it.draftPoints + point) }
    }

    fun onDraftRiskSelected(risk: MapAreaColor) {
        if (!_uiState.value.canEditMap) return

        _uiState.update { it.copy(draftRisk = risk) }
    }

    fun undoDraftPoint() {
        if (!_uiState.value.canEditMap) return

        _uiState.update { state ->
            state.copy(draftPoints = state.draftPoints.dropLast(1))
        }
    }

    fun cancelCustomAreaDraft() {
        _uiState.update {
            it.copy(isCreatingCustomArea = false, draftPoints = emptyList())
        }
    }

    fun saveCustomAreaDraft() {
        val state = _uiState.value
        val familyId = state.familyId
        if (!state.canSaveCustomArea || familyId == null) return

        viewModelScope.launch {
            if (!networkMonitor.isOnline()) {
                _uiState.update { it.copy(isOnline = false, error = "Network required to create a zone") }
                return@launch
            }

            val childId = _uiState.value.selectedZoneTarget.childUserId()
            _uiState.update {
                it.copy(
                    isOnline = true,
                    isSavingCustomArea = true,
                    isCreatingCustomArea = false,
                    error = null
                )
            }
            val optimisticArea = createOptimisticCustomArea(
                risk = _uiState.value.draftRisk,
                points = _uiState.value.draftPoints
            )
            _uiState.update {
                it.copy(
                    customAreas = it.customAreas + optimisticArea,
                    draftPoints = emptyList()
                )
            }

            val result = mapRepository.createCustomArea(
                familyId = familyId,
                childId = childId,
                risk = optimisticArea.color,
                points = optimisticArea.points
            )
            result.fold(
                onSuccess = { createdArea ->
                    _uiState.update {
                        it.copy(
                            isSavingCustomArea = false,
                            isCreatingCustomArea = false,
                            customAreas = it.customAreas
                                .filterNot { area -> area.id == optimisticArea.id } + createdArea,
                            error = null
                        )
                    }
                    refreshMapEditsInBackground(familyId, childId)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingCustomArea = false,
                            customAreas = it.customAreas.filterNot { area -> area.id == optimisticArea.id },
                            error = error.message ?: "Failed to create custom zone"
                        )
                    }
                }
            )
        }
    }

    private fun deleteCustomArea(area: MapArea) {
        viewModelScope.launch {
            val familyId = _uiState.value.familyId
            if (familyId == null) {
                _uiState.update { it.copy(error = "Family ID not found") }
                return@launch
            }

            if (!networkMonitor.isOnline()) {
                _uiState.update { it.copy(isOnline = false, error = "Network required to delete a zone") }
                return@launch
            }

            val childId = _uiState.value.selectedZoneTarget.childUserId()
            _uiState.update {
                it.copy(
                    isOnline = true,
                    error = null,
                    customAreas = it.customAreas.filterNot { customArea -> customArea.id == area.id }
                )
            }

            mapRepository.deleteCustomArea(area.id)
                .fold(
                    onSuccess = {
                        refreshMapEditsInBackground(familyId, childId)
                    },
                    onFailure = { error ->
                        _uiState.update { state ->
                            state.copy(
                                customAreas = if (state.customAreas.any { customArea -> customArea.id == area.id }) {
                                    state.customAreas
                                } else {
                                    state.customAreas + area
                                },
                                error = error.message ?: "Failed to delete custom zone"
                            )
                        }
                    }
                )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun buildTileUrl(cityId: String, generationVersion: String): String {
        val apiRoot = BuildConfig.BASE_URL.trimEnd('/')
        val encodedVersion = URLEncoder.encode(generationVersion, StandardCharsets.UTF_8.toString())
        return "$apiRoot/maps/tiles/$cityId/{z}/{x}/{y}.pbf?v=$encodedVersion"
    }

    private fun refreshMapEditsInBackground(familyId: String, childId: UserId?) {
        viewModelScope.launch { loadOverrides(familyId, childId) }
        viewModelScope.launch { syncAlertZones(_uiState.value.activeMapCityId, familyId, childId) }
    }

    private companion object {
        const val DefaultCityId = "ekb"
        const val BaseMapStyleUrl = "https://tiles.openfreemap.org/styles/bright"
        val DefaultCity = MapCity(
            cityId = DefaultCityId,
            name = "Екатеринбург",
            bbox = team.kid.roadsafety.domain.aggregates.map.MapCityBbox(
                minLon = 60.3,
                minLat = 56.7,
                maxLon = 60.9,
                maxLat = 56.9
            )
        )
    }
}

data class MapUiState(
    val activeMapCityId: String = "ekb",
    val familyCityId: String = "ekb",
    val cities: List<MapCity> = emptyList(),
    val familyId: String? = null,
    val isParent: Boolean = false,
    val metadata: MapCityMetadata? = null,
    val tileUrl: String? = null,
    val mapStyleJson: String? = null,
    val overrides: Map<String, MapAreaColor> = emptyMap(),
    val customAreas: List<MapArea> = emptyList(),
    val zoneTargets: List<ZoneTarget> = listOf(ZoneTarget.AllFamily),
    val selectedZoneTarget: ZoneTarget = ZoneTarget.AllFamily,
    val activePaintColor: MapAreaColor? = null,
    val isEraseMode: Boolean = false,
    val isCreatingCustomArea: Boolean = false,
    val draftPoints: List<GeoPoint> = emptyList(),
    val draftRisk: MapAreaColor = MapAreaColor.YELLOW,
    val isSavingCustomArea: Boolean = false,
    val isOnline: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentLocation: GeoPoint? = null,
    val childLocations: List<ChildMapLocation> = emptyList()
) {
    val visibleChildLocations: List<ChildMapLocation>
        get() = when (val target = selectedZoneTarget) {
            ZoneTarget.AllFamily -> childLocations.latestByChildId()
            is ZoneTarget.Child -> childLocations.filter { child -> child.childId == target.childId }.latestByChildId()
        }

    val canEditMap: Boolean
        get() = familyId != null && isParent && activeMapCityId == familyCityId

    val canSaveCustomArea: Boolean
        get() = canEditMap && isOnline && !isSavingCustomArea && draftPoints.distinct().size >= 3

    val canEraseAreas: Boolean
        get() = canEditMap
}

private fun MapCityBbox.contains(point: GeoPoint): Boolean {
    return point.longitude in minLon..maxLon && point.latitude in minLat..maxLat
}

data class ChildMapLocation(
    val childId: String,
    val displayName: String,
    val point: GeoPoint,
    val currentRisk: String,
    val lastUpdatedAt: String
) {
    val label: String
        get() = displayName.ifBlank { "Child ${childId.take(8)}" }
}

private fun List<ChildMapLocation>.latestByChildId(): List<ChildMapLocation> {
    return groupBy { it.childId }
        .values
        .map { locations -> locations.maxBy { it.lastUpdatedAt } }
}

sealed class ZoneTarget(
    open val id: String,
    open val label: String
) {
    data object AllFamily : ZoneTarget("all", "Вся семья")

    data class Child(
        val childId: String,
        override val label: String
    ) : ZoneTarget("child:$childId", label)
}

private fun ZoneTarget.childUserId(): UserId? {
    return when (this) {
        ZoneTarget.AllFamily -> null
        is ZoneTarget.Child -> runCatching { UserId(UUID.fromString(childId)) }.getOrNull()
    }
}

private fun FamilyMemberEntity.toZoneTarget(childLocations: List<ChildMapLocation>): ZoneTarget.Child {
    val childLabel = childLocations.firstOrNull { it.childId == userId }?.label
        ?: displayLabel
    return ZoneTarget.Child(childId = userId, label = childLabel)
}

private fun List<ZoneTarget>.refreshChildLabels(children: List<ChildMapLocation>): List<ZoneTarget> {
    return map { target ->
        if (target is ZoneTarget.Child) {
            target.copy(label = children.firstOrNull { it.childId == target.childId }?.label ?: target.label)
        } else {
            target
        }
    }
}

private fun createOptimisticCustomArea(
    risk: MapAreaColor,
    points: List<GeoPoint>
): MapArea {
    return MapArea(
        id = team.kid.roadsafety.domain.AreaId(UUID.randomUUID()),
        baseAreaKey = null,
        source = "local",
        osmId = null,
        color = risk,
        points = points,
        cityId = null
    )
}
