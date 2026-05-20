package team.kid.roadsafety.domain.aggregates.map

import team.kid.roadsafety.domain.UserId

interface MapRepository {
    suspend fun getAreas(cityId: String? = null): Result<List<MapArea>>
    suspend fun getUserAreas(familyId: String, childId: UserId? = null): Result<List<MapArea>>
    suspend fun updateAreaColor(areaId: String, color: MapAreaColor, childId: UserId? = null): Result<Unit>
}
