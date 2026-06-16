package team.kid.roadsafety.domain.aggregates.map

import team.kid.roadsafety.domain.AreaId
import team.kid.roadsafety.domain.CityId

data class MapArea(
    val id: AreaId,
    val baseAreaKey: String?,
    val source: String?,
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

data class AlertZone(
    val id: String,
    val baseAreaKey: String?,
    val risk: MapAreaColor,
    val points: List<GeoPoint>
)

data class MapCity(
    val cityId: String,
    val name: String,
    val bbox: MapCityBbox? = null
)

data class MapCityMetadata(
    val cityId: String,
    val generationVersion: String,
    val bbox: MapCityBbox? = null
)

data class MapCityBbox(
    val minLon: Double,
    val minLat: Double,
    val maxLon: Double,
    val maxLat: Double
)
