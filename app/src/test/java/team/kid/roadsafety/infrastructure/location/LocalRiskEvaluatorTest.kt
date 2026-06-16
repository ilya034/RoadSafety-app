package team.kid.roadsafety.infrastructure.location

import org.junit.Assert.assertEquals
import org.junit.Test
import team.kid.roadsafety.domain.aggregates.map.AlertZone
import team.kid.roadsafety.domain.aggregates.map.GeoPoint
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor

class LocalRiskEvaluatorTest {
    @Test
    fun emptyCacheReturnsGreen() {
        assertEquals(MapAreaColor.GREEN, LocalRiskEvaluator.evaluate(10.0, 10.0, emptyList()))
    }

    @Test
    fun pointInsidePolygonReturnsZoneRisk() {
        val zones = listOf(square("yellow", MapAreaColor.YELLOW, 0.0, 0.0, 10.0, 10.0))

        assertEquals(MapAreaColor.YELLOW, LocalRiskEvaluator.evaluate(5.0, 5.0, zones))
        assertEquals(MapAreaColor.GREEN, LocalRiskEvaluator.evaluate(15.0, 15.0, zones))
    }

    @Test
    fun redWinsOverYellowWhenZonesOverlap() {
        val zones = listOf(
            square("yellow", MapAreaColor.YELLOW, 0.0, 0.0, 10.0, 10.0),
            square("red", MapAreaColor.RED, 2.0, 2.0, 8.0, 8.0)
        )

        assertEquals(MapAreaColor.RED, LocalRiskEvaluator.evaluate(5.0, 5.0, zones))
    }

    private fun square(id: String, risk: MapAreaColor, minLat: Double, minLon: Double, maxLat: Double, maxLon: Double): AlertZone {
        return AlertZone(
            id = id,
            baseAreaKey = null,
            risk = risk,
            points = listOf(
                GeoPoint(minLat, minLon),
                GeoPoint(minLat, maxLon),
                GeoPoint(maxLat, maxLon),
                GeoPoint(maxLat, minLon),
                GeoPoint(minLat, minLon)
            )
        )
    }
}
