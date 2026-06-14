package team.kid.roadsafety.data.repository

import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.domain.AreaId
import team.kid.roadsafety.domain.CityId
import team.kid.roadsafety.domain.UserId
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
    private val api: RoadSafetyApi
) : MapRepository {

    override suspend fun getUserAreas(familyId: String, childId: UserId?): Result<List<MapArea>> {
        return try {
            val response = api.getUserAreas(familyId, childId?.value?.toString())
            if (response.isSuccessful) {
                Result.success(response.body()?.features?.map { it.toMapArea() } ?: emptyList())
            } else {
                Result.failure(Exception(response.parseErrorMessage("Failed to fetch user areas")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCityMetadata(cityId: String): Result<MapCityMetadata> {
        return try {
            val response = api.getCityMetadata(cityId)
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.failure(Exception("City metadata response is empty"))
                Result.success(
                    MapCityMetadata(
                        cityId = body.cityId,
                        generationVersion = body.generationVersion,
                        bbox = body.bbox?.let {
                            MapCityBbox(
                                minLon = it.minLon,
                                minLat = it.minLat,
                                maxLon = it.maxLon,
                                maxLat = it.maxLat
                            )
                        }
                    )
                )
            } else {
                Result.failure(Exception(response.parseErrorMessage("Failed to fetch city metadata")))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
        val coordsArray = geometry?.coordinates as? kotlinx.serialization.json.JsonArray
        val firstRing = coordsArray?.firstOrNull() as? kotlinx.serialization.json.JsonArray
        val points = firstRing?.mapNotNull { pointElement ->
            val pointArray = pointElement as? kotlinx.serialization.json.JsonArray
            if (pointArray != null && pointArray.size >= 2) {
                val lon = (pointArray[0] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                val lat = (pointArray[1] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
                GeoPoint(latitude = lat, longitude = lon)
            } else null
        } ?: emptyList()

        return MapArea(
            id = AreaId(UUID.fromString(properties.id)),
            baseAreaKey = properties.baseAreaKey,
            source = properties.source,
            osmId = null,
            color = MapAreaColor.fromString(properties.risk.name),
            points = points,
            cityId = null
        )
    }
}
