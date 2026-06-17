package team.kid.roadsafety.domain.aggregates.map

import team.kid.roadsafety.domain.UserId
import team.kid.roadsafety.domain.AreaId

interface MapRepository {
    suspend fun getUserAreas(familyId: String, childId: UserId? = null): Result<List<MapArea>>
    fun getCachedUserAreas(familyId: String, childId: UserId? = null): List<MapArea>?
    suspend fun getAlertZones(cityId: String, familyId: String, childId: UserId? = null): Result<List<AlertZone>>
    fun getCachedAlertZones(cityId: String, familyId: String, childId: UserId? = null): List<AlertZone>?
    fun getActiveCachedAlertZones(): List<AlertZone>
    suspend fun getCityMetadata(cityId: String): Result<MapCityMetadata>
    fun getCachedCityMetadata(cityId: String): MapCityMetadata?
    suspend fun updateAreaColor(area: MapArea, familyId: String, color: MapAreaColor, childId: UserId? = null): Result<Unit>
    suspend fun updateBaseAreaColor(baseAreaKey: String, familyId: String, color: MapAreaColor, childId: UserId? = null): Result<Unit>
    suspend fun resetBaseAreaColor(baseAreaKey: String, familyId: String, childId: UserId? = null): Result<Unit>
    suspend fun createCustomArea(familyId: String, childId: UserId?, risk: MapAreaColor, points: List<GeoPoint>): Result<MapArea>
    suspend fun deleteCustomArea(areaId: AreaId): Result<Unit>
}
