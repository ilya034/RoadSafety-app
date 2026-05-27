package team.kid.roadsafety.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MapAreaResponseDto(
    val id: String,
    val osmId: Long? = null,
    val color: String,
    val geometry: GeometryDto,
    val cityId: String? = null
)

@Serializable
data class GeometryDto(
    val type: String, // e.g., "Polygon"
    val coordinates: List<List<List<Double>>> // GeoJSON structure for Polygon
)

@Serializable
data class UpdateAreaColorRequestDto(
    val color: String,
    val childId: String? = null
)
