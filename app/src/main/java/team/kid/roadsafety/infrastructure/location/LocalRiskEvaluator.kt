package team.kid.roadsafety.infrastructure.location

import team.kid.roadsafety.domain.aggregates.map.AlertZone
import team.kid.roadsafety.domain.aggregates.map.GeoPoint
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.map.MapRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRiskEvaluator @Inject constructor(
    private val mapRepository: MapRepository
) {
    fun evaluate(latitude: Double, longitude: Double): MapAreaColor {
        return evaluate(latitude, longitude, mapRepository.getActiveCachedAlertZones())
    }

    companion object {
        fun evaluate(latitude: Double, longitude: Double, zones: List<AlertZone>): MapAreaColor {
            val point = GeoPoint(latitude = latitude, longitude = longitude)
            var matchedRisk = MapAreaColor.GREEN

            for (zone in zones) {
                if (zone.points.size < 3 || !contains(point, zone.points)) {
                    continue
                }

                if (zone.risk == MapAreaColor.RED) {
                    return MapAreaColor.RED
                }

                if (zone.risk == MapAreaColor.YELLOW) {
                    matchedRisk = MapAreaColor.YELLOW
                }
            }

            return matchedRisk
        }

        private fun contains(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
            var inside = false
            var previous = polygon.last()

            for (current in polygon) {
                val crossesLatitude = (current.latitude > point.latitude) != (previous.latitude > point.latitude)
                if (crossesLatitude) {
                    val intersectionLongitude =
                        (previous.longitude - current.longitude) *
                            (point.latitude - current.latitude) /
                            (previous.latitude - current.latitude) +
                            current.longitude
                    if (point.longitude < intersectionLongitude) {
                        inside = !inside
                    }
                }
                previous = current
            }

            return inside
        }
    }
}
