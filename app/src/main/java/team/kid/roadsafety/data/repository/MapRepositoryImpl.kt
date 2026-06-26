package team.kid.roadsafety.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import team.kid.roadsafety.data.local.MapCacheLocalDataSource
import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.domain.AreaId
import team.kid.roadsafety.domain.UserId
import team.kid.roadsafety.domain.aggregates.map.AlertZone
import team.kid.roadsafety.domain.aggregates.map.GeoPoint
import team.kid.roadsafety.domain.aggregates.map.MapArea
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.map.MapCityBbox
import team.kid.roadsafety.domain.aggregates.map.MapCityMetadata
import team.kid.roadsafety.domain.aggregates.map.MapRepository
import team.kid.roadsafety.infrastructure.parseErrorMessage
import java.util.UUID
import javax.inject.Inject

class MapRepositoryImpl @Inject constructor(
    private val api: RoadSafetyApi,
    private val cache: MapCacheLocalDataSource
) : MapRepository {

    override fun getCachedUserAreas(familyId: String, childId: UserId?): List<MapArea>? {
        return cache.getUserAreas(familyId, childId?.value?.toString())
            ?.features
            ?.map { it.toMapArea() }
    }

    override fun getActiveCachedAlertZones(): List<AlertZone> {
        return cache.getActiveAlertZones()?.zones?.map { it.toAlertZone() } ?: emptyList()
    }

    override fun getCachedCityMetadata(cityId: String): MapCityMetadata? {
        return cache.getCityMetadata(cityId)?.toDomain()
    }

    override suspend fun getUserAreas(familyId: String, childId: UserId?): Result<List<MapArea>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getUserAreas(familyId, childId?.value?.toString())
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    cache.saveUserAreas(familyId, childId?.value?.toString(), body)
                }
                Result.success(body?.features?.map { it.toMapArea() } ?: emptyList())
            } else {
                cachedUserAreas(familyId, childId)
                    ?: Result.failure(Exception(response.parseErrorMessage("Failed to fetch user areas")))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            cachedUserAreas(familyId, childId) ?: Result.failure(e)
        }
    }

    override suspend fun getAlertZones(cityId: String, familyId: String, childId: UserId?): Result<List<AlertZone>> = withContext(Dispatchers.IO) {
        try {
            val childKey = childId?.value?.toString()
            val response = api.getAlertZones(cityId, familyId, childKey)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    body.use { b ->
                        cache.saveAlertZonesStream(cityId, familyId, childKey, b.byteStream())
                    }
                }
                val cached = cache.getAlertZones(cityId, familyId, childKey)
                Result.success(cached?.zones?.map { it.toAlertZone() } ?: emptyList())
            } else {
                cachedAlertZones(cityId, familyId, childId)
                    ?: Result.failure(Exception(response.parseErrorMessage("Failed to fetch alert zones")))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            cachedAlertZones(cityId, familyId, childId) ?: Result.failure(e)
        }
    }

    override suspend fun getCityMetadata(cityId: String): Result<MapCityMetadata> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCityMetadata(cityId)
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext Result.failure(Exception("City metadata response is empty"))
                cache.saveCityMetadata(cityId, body)
                Result.success(body.toDomain())
            } else {
                cachedCityMetadata(cityId)
                    ?: Result.failure(Exception(response.parseErrorMessage("Failed to fetch city metadata")))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            cachedCityMetadata(cityId) ?: Result.failure(e)
        }
    }

    override suspend fun updateBaseAreaColor(baseAreaKey: String, familyId: String, color: MapAreaColor, childId: UserId?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = team.kid.roadsafety.data.dto.CreateBaseAreaOverrideRequestDto(
                familyId = familyId,
                childId = childId?.value?.toString(),
                baseAreaKey = baseAreaKey,
                risk = when (color) {
                    MapAreaColor.GREEN -> team.kid.roadsafety.data.dto.RiskLevelDto.Green
                    MapAreaColor.YELLOW -> team.kid.roadsafety.data.dto.RiskLevelDto.Yellow
                    MapAreaColor.RED -> team.kid.roadsafety.data.dto.RiskLevelDto.Red
                    MapAreaColor.NONE -> team.kid.roadsafety.data.dto.RiskLevelDto.Green // Fallback
                }
            )
            val response = api.createBaseAreaOverride(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.parseErrorMessage("Failed to update area color")))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun createCustomArea(
        familyId: String,
        childId: UserId?,
        risk: MapAreaColor,
        points: List<GeoPoint>
    ): Result<MapArea> = withContext(Dispatchers.IO) {
        try {
            val request = team.kid.roadsafety.data.dto.CreateCustomUserMapAreaRequestDto(
                familyId = familyId,
                childId = childId?.value?.toString(),
                risk = risk.toRiskLevelDto(),
                geometry = buildPolygonGeometry(points)
            )
            val response = api.createCustomUserMapArea(request)
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext Result.failure(Exception("Custom area response is empty"))
                val childKey = childId?.value?.toString()
                body.toAlertZoneDto()?.let { alertZone ->
                    cache.upsertActiveAlertZone(familyId, childKey, alertZone)
                }
                Result.success(body.toMapArea())
            } else {
                Result.failure(Exception(response.parseErrorMessage("Failed to create custom area")))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun resetBaseAreaColor(baseAreaKey: String, familyId: String, childId: UserId?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.resetBaseAreaColor(
                familyId = familyId,
                baseAreaKey = baseAreaKey,
                childId = childId?.value?.toString()
            )
            if (response.isSuccessful || response.code() == 404) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.parseErrorMessage("Failed to reset area color")))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun deleteCustomArea(areaId: AreaId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val areaIdValue = areaId.value.toString()
            val response = api.deleteCustomArea(areaId.value.toString())
            if (response.isSuccessful || response.code() == 404) {
                cache.removeActiveAlertZone(areaIdValue)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.parseErrorMessage("Failed to delete custom zone")))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    private fun team.kid.roadsafety.data.dto.UserMapAreaFeatureDto.toMapArea(): MapArea {
        return MapArea(
            id = AreaId(UUID.fromString(properties.id)),
            baseAreaKey = properties.baseAreaKey,
            source = properties.source,
            osmId = null,
            color = MapAreaColor.fromString(properties.risk.name),
            points = geometry.toGeoPoints(),
            cityId = null
        )
    }

    private fun team.kid.roadsafety.data.dto.UserMapAreaFeatureDto.toAlertZoneDto(): team.kid.roadsafety.data.dto.AlertZoneDto? {
        val geometry = geometry ?: return null
        val risk = properties.risk
        if (risk == team.kid.roadsafety.data.dto.RiskLevelDto.Green) return null
        return team.kid.roadsafety.data.dto.AlertZoneDto(
            id = properties.id,
            baseAreaKey = properties.baseAreaKey,
            risk = risk,
            source = properties.source,
            geometry = geometry
        )
    }

    private fun team.kid.roadsafety.data.dto.AlertZoneDto.toAlertZone(): AlertZone {
        return AlertZone(
            id = id,
            baseAreaKey = baseAreaKey,
            risk = MapAreaColor.fromString(risk.name),
            points = geometry.toGeoPoints()
        )
    }

    private fun team.kid.roadsafety.data.dto.GeoJsonGeometryDto?.toGeoPoints(): List<GeoPoint> {
        return this?.coordinates ?: emptyList()
    }

    private fun MapAreaColor.toRiskLevelDto(): team.kid.roadsafety.data.dto.RiskLevelDto {
        return when (this) {
            MapAreaColor.GREEN -> team.kid.roadsafety.data.dto.RiskLevelDto.Green
            MapAreaColor.YELLOW -> team.kid.roadsafety.data.dto.RiskLevelDto.Yellow
            MapAreaColor.RED -> team.kid.roadsafety.data.dto.RiskLevelDto.Red
            MapAreaColor.NONE -> team.kid.roadsafety.data.dto.RiskLevelDto.Green
        }
    }

    private fun cachedUserAreas(familyId: String, childId: UserId?): Result<List<MapArea>>? {
        return cache.getUserAreas(familyId, childId?.value?.toString())
            ?.features
            ?.map { it.toMapArea() }
            ?.let { Result.success(it) }
    }

    private fun cachedAlertZones(cityId: String, familyId: String, childId: UserId?): Result<List<AlertZone>>? {
        return cache.getAlertZones(cityId, familyId, childId?.value?.toString())
            ?.zones
            ?.map { it.toAlertZone() }
            ?.let { Result.success(it) }
    }

    private fun cachedCityMetadata(cityId: String): Result<MapCityMetadata>? {
        return cache.getCityMetadata(cityId)?.toDomain()?.let { Result.success(it) }
    }

    private fun team.kid.roadsafety.data.dto.MapCityMetadataDto.toDomain(): MapCityMetadata {
        return MapCityMetadata(
            cityId = cityId,
            generationVersion = generationVersion,
            bbox = bbox?.let {
                MapCityBbox(
                    minLon = it.minLon,
                    minLat = it.minLat,
                    maxLon = it.maxLon,
                    maxLat = it.maxLat
                )
            }
        )
    }

    override fun clearData() {
        cache.clearData()
    }
}



internal fun buildPolygonGeometry(points: List<GeoPoint>): team.kid.roadsafety.data.dto.GeoJsonGeometryDto {
    val closedRing = if (points.firstOrNull() == points.lastOrNull()) {
        points
    } else {
        points + points.first()
    }
    return team.kid.roadsafety.data.dto.GeoJsonGeometryDto(
        type = "Polygon",
        coordinates = closedRing
    )
}
