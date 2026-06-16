package team.kid.roadsafety.data.repository

import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.data.local.MapCacheLocalDataSource
import team.kid.roadsafety.domain.AreaId
import team.kid.roadsafety.domain.CityId
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

    override fun getCachedAlertZones(cityId: String, familyId: String, childId: UserId?): List<AlertZone>? {
        return cache.getAlertZones(cityId, familyId, childId?.value?.toString())
            ?.zones
            ?.map { it.toAlertZone() }
    }

    override fun getActiveCachedAlertZones(): List<AlertZone> {
        return cache.getActiveAlertZones()?.zones?.map { it.toAlertZone() } ?: emptyList()
    }

    override fun getCachedCityMetadata(cityId: String): MapCityMetadata? {
        return cache.getCityMetadata(cityId)?.toDomain()
    }

    override suspend fun getUserAreas(familyId: String, childId: UserId?): Result<List<MapArea>> {
        return try {
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
            cachedUserAreas(familyId, childId) ?: Result.failure(e)
        }
    }

    override suspend fun getAlertZones(cityId: String, familyId: String, childId: UserId?): Result<List<AlertZone>> {
        return try {
            val childKey = childId?.value?.toString()
            val response = api.getAlertZones(cityId, familyId, childKey)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    cache.saveAlertZones(cityId, familyId, childKey, body)
                }
                Result.success(body?.zones?.map { it.toAlertZone() } ?: emptyList())
            } else {
                cachedAlertZones(cityId, familyId, childId)
                    ?: Result.failure(Exception(response.parseErrorMessage("Failed to fetch alert zones")))
            }
        } catch (e: Exception) {
            cachedAlertZones(cityId, familyId, childId) ?: Result.failure(e)
        }
    }

    override suspend fun getCityMetadata(cityId: String): Result<MapCityMetadata> {
        return try {
            val response = api.getCityMetadata(cityId)
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.failure(Exception("City metadata response is empty"))
                cache.saveCityMetadata(cityId, body)
                Result.success(body.toDomain())
            } else {
                cachedCityMetadata(cityId)
                    ?: Result.failure(Exception(response.parseErrorMessage("Failed to fetch city metadata")))
            }
        } catch (e: Exception) {
            cachedCityMetadata(cityId) ?: Result.failure(e)
        }
    }

    override suspend fun updateAreaColor(area: MapArea, familyId: String, color: MapAreaColor, childId: UserId?): Result<Unit> {
        val baseAreaKey = area.baseAreaKey
            ?: return Result.failure(Exception("Cannot override color for an area without baseAreaKey"))
        return updateBaseAreaColor(baseAreaKey, familyId, color, childId)
    }

    override suspend fun updateBaseAreaColor(baseAreaKey: String, familyId: String, color: MapAreaColor, childId: UserId?): Result<Unit> {
        return try {
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

    private fun team.kid.roadsafety.data.dto.AlertZoneDto.toAlertZone(): AlertZone {
        return AlertZone(
            id = id,
            baseAreaKey = baseAreaKey,
            risk = MapAreaColor.fromString(risk.name),
            points = geometry.toGeoPoints()
        )
    }

    private fun team.kid.roadsafety.data.dto.GeoJsonGeometryDto?.toGeoPoints(): List<GeoPoint> {
        val coordsArray = this?.coordinates as? kotlinx.serialization.json.JsonArray
        val firstRing = coordsArray?.firstOrNull() as? kotlinx.serialization.json.JsonArray
        return firstRing?.mapNotNull { pointElement ->
            val pointArray = pointElement as? kotlinx.serialization.json.JsonArray
            if (pointArray != null && pointArray.size >= 2) {
                val lon = (pointArray[0] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                val lat = (pointArray[1] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                GeoPoint(latitude = lat, longitude = lon)
            } else {
                null
            }
        } ?: emptyList()
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
}
