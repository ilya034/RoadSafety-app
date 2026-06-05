package team.kid.roadsafety.data.repository

import team.kid.roadsafety.data.dto.UpdateAreaColorRequestDto
import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.domain.AreaId
import team.kid.roadsafety.domain.CityId
import team.kid.roadsafety.domain.UserId
import team.kid.roadsafety.domain.aggregates.map.GeoPoint
import team.kid.roadsafety.domain.aggregates.map.MapArea
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.map.MapRepository
import team.kid.roadsafety.infrastructure.parseErrorMessage
import java.util.UUID
import javax.inject.Inject

class MapRepositoryImpl @Inject constructor(
    private val api: RoadSafetyApi
) : MapRepository {

    override suspend fun getAreas(cityId: String?): Result<List<MapArea>> {
        return try {
            val response = api.getAreas(cityId)
            if (response.isSuccessful) {
                Result.success(response.body()?.map { it.toMapArea() } ?: emptyList())
            } else {
                Result.failure(Exception(response.parseErrorMessage("Failed to fetch map areas")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserAreas(familyId: String, childId: UserId?): Result<List<MapArea>> {
        return try {
            val response = api.getUserAreas(familyId, childId?.value?.toString())
            if (response.isSuccessful) {
                Result.success(response.body()?.map { it.toMapArea() } ?: emptyList())
            } else {
                Result.failure(Exception(response.parseErrorMessage("Failed to fetch user areas")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAreaColor(areaId: String, color: MapAreaColor, childId: UserId?): Result<Unit> {
        return try {
            val response = api.updateAreaColor(
                areaId,
                UpdateAreaColorRequestDto(color.name.lowercase(), childId?.value?.toString())
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.parseErrorMessage("Failed to update area color")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun team.kid.roadsafety.data.dto.MapAreaResponseDto.toMapArea(): MapArea {
        // Simple mapping from GeoJSON structure (assuming first ring of polygon)
        val points = geometry.coordinates.firstOrNull()?.map { coord ->
            GeoPoint(latitude = coord[1], longitude = coord[0])
        } ?: emptyList()

        return MapArea(
            id = AreaId(UUID.fromString(id)),
            osmId = osmId,
            color = MapAreaColor.fromString(color),
            points = points,
            cityId = cityId?.let { CityId(it) }
        )
    }
}
