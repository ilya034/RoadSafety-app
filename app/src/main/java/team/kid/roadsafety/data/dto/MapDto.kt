package team.kid.roadsafety.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
enum class RiskLevelDto {
    Green, Yellow, Red
}

@Serializable
data class GeoJsonGeometryDto(
    val type: String, // Point, LineString, Polygon, MultiPolygon
    val coordinates: JsonElement // Using JsonElement to allow any structure
)

@Serializable
data class UserMapAreaPropertiesDto(
    val id: String,
    val familyId: String,
    val childId: String? = null,
    val baseAreaKey: String? = null,
    val risk: RiskLevelDto,
    val source: String,
    val renderPriority: Int,
    val createdByUserId: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class UserMapAreaFeatureDto(
    val type: String,
    val geometry: GeoJsonGeometryDto? = null,
    val properties: UserMapAreaPropertiesDto
)

@Serializable
data class UserMapAreaFeatureCollectionDto(
    val type: String,
    val features: List<UserMapAreaFeatureDto>
)

@Serializable
data class CreateBaseAreaOverrideRequestDto(
    val familyId: String,
    val childId: String? = null,
    val baseAreaKey: String,
    val risk: RiskLevelDto
)

@Serializable
data class CreateCustomUserMapAreaRequestDto(
    val familyId: String,
    val childId: String? = null,
    val risk: RiskLevelDto,
    val geometry: GeoJsonGeometryDto
)

@Serializable
data class MapCityDto(
    val cityId: String,
    val name: String,
    val bbox: MapCityBboxDto? = null
)

@Serializable
data class MapCitiesResponseDto(
    val cities: List<MapCityDto>
)

@Serializable
data class MapCityMetadataDto(
    val cityId: String,
    val generationVersion: String,
    val bbox: MapCityBboxDto? = null
)

@Serializable
data class MapCityBboxDto(
    val minLon: Double,
    val minLat: Double,
    val maxLon: Double,
    val maxLat: Double
)
