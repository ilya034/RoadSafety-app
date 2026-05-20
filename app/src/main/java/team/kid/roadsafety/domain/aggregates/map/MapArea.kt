package team.kid.roadsafety.domain.aggregates.map

import team.kid.roadsafety.domain.AreaId
import team.kid.roadsafety.domain.CityId

data class MapArea(
    val id: AreaId,
    val osmId: Long?,
    val color: MapAreaColor,
    val points: List<GeoPoint>,
    val cityId: CityId?
)

enum class MapAreaColor {
    GREEN,
    YELLOW,
    RED,
    NONE;

    companion object {
        fun fromString(value: String): MapAreaColor = when (value.lowercase()) {
            "green" -> GREEN
            "yellow" -> YELLOW
            "red" -> RED
            "none" -> NONE
            else -> NONE
        }
    }
}

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)
