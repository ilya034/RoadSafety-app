package team.kid.roadsafety.data.repository

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
import org.junit.Assert.assertEquals
import org.junit.Test
import team.kid.roadsafety.data.dto.GeoJsonGeometryDto
import team.kid.roadsafety.domain.aggregates.map.GeoPoint

class MapRepositoryImplTest {
    @Test
    fun buildPolygonGeometryClosesRingAndUsesLongitudeLatitudeOrder() {
        val geometry = buildPolygonGeometry(
            listOf(
                GeoPoint(latitude = 56.0, longitude = 60.0),
                GeoPoint(latitude = 57.0, longitude = 61.0),
                GeoPoint(latitude = 58.0, longitude = 62.0)
            )
        )

        assertEquals("Polygon", geometry.type)
        assertEquals(4, geometry.coordinates.size)
        assertEquals(GeoPoint(latitude = 56.0, longitude = 60.0), geometry.coordinates[0])
        assertEquals(GeoPoint(latitude = 57.0, longitude = 61.0), geometry.coordinates[1])
        assertEquals(GeoPoint(latitude = 58.0, longitude = 62.0), geometry.coordinates[2])
        assertEquals(GeoPoint(latitude = 56.0, longitude = 60.0), geometry.coordinates[3])
    }

    @Test
    fun geoJsonGeometryDtoSerializationAndDeserialization() {
        val original = buildPolygonGeometry(
            listOf(
                GeoPoint(latitude = 56.0, longitude = 60.0),
                GeoPoint(latitude = 57.0, longitude = 61.0),
                GeoPoint(latitude = 58.0, longitude = 62.0)
            )
        )

        // Verify serialization outputs standard GeoJSON format
        val jsonString = Json.encodeToString(original)
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        assertEquals("Polygon", jsonObject["type"]?.jsonPrimitive?.content)
        
        val coords = jsonObject["coordinates"]?.jsonArray
        val ring = coords?.firstOrNull()?.jsonArray
        assertEquals(4, ring?.size)
        
        val firstPoint = ring?.get(0)?.jsonArray
        assertEquals(60.0, firstPoint?.get(0)?.jsonPrimitive?.double ?: 0.0, 0.0) // longitude
        assertEquals(56.0, firstPoint?.get(1)?.jsonPrimitive?.double ?: 0.0, 0.0) // latitude

        // Verify deserialization parses it back correctly
        val decoded = Json.decodeFromString<GeoJsonGeometryDto>(jsonString)
        assertEquals("Polygon", decoded.type)
        assertEquals(original.coordinates, decoded.coordinates)
    }
}
