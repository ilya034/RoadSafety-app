package team.kid.roadsafety.domain.aggregates.map

import team.kid.roadsafety.domain.UserId

interface MapRepository {
    suspend fun getUserAreas(familyId: String, childId: UserId? = null): Result<List<MapArea>>
    fun getCachedUserAreas(familyId: String, childId: UserId? = null): List<MapArea>?
    suspend fun getCityMetadata(cityId: String): Result<MapCityMetadata>
    fun getCachedCityMetadata(cityId: String): MapCityMetadata?
    suspend fun updateAreaColor(area: MapArea, familyId: String, color: MapAreaColor, childId: UserId? = null): Result<Unit>
    suspend fun updateBaseAreaColor(baseAreaKey: String, familyId: String, color: MapAreaColor, childId: UserId? = null): Result<Unit>
}
