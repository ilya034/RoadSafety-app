package team.kid.roadsafety.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import team.kid.roadsafety.domain.aggregates.map.GeoPoint

@kotlinx.serialization.Serializable(with = RiskLevelDtoSerializer::class)
enum class RiskLevelDto {
    Green, Yellow, Red
}

object RiskLevelDtoSerializer : KSerializer<RiskLevelDto> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("RiskLevelDto", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RiskLevelDto {
        return when (decoder.decodeString().lowercase()) {
            "green" -> RiskLevelDto.Green
            "yellow" -> RiskLevelDto.Yellow
            "red" -> RiskLevelDto.Red
            else -> RiskLevelDto.Green
        }
    }

    override fun serialize(encoder: Encoder, value: RiskLevelDto) {
        encoder.encodeString(value.name)
    }
}

object GeoJsonGeometryDtoSerializer : KSerializer<GeoJsonGeometryDto> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GeoJsonGeometry") {
        element("type", kotlinx.serialization.serializer<String>().descriptor)
        element("coordinates", JsonElement.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): GeoJsonGeometryDto {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("Only Json format is supported")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        val type = jsonObject["type"]?.jsonPrimitive?.content ?: ""
        val coordsElement = jsonObject["coordinates"]
        
        val points = mutableListOf<GeoPoint>()
        if (coordsElement is JsonArray) {
            extractPoints(type, coordsElement, points)
        }
        
        return GeoJsonGeometryDto(type, points)
    }

    private fun extractPoints(type: String, coordsElement: JsonArray, target: MutableList<GeoPoint>) {
        when (type.lowercase()) {
            "point" -> {
                if (coordsElement.size >= 2) {
                    val lon = coordsElement[0].jsonPrimitive.doubleOrNull ?: 0.0
                    val lat = coordsElement[1].jsonPrimitive.doubleOrNull ?: 0.0
                    target.add(GeoPoint(latitude = lat, longitude = lon))
                }
            }
            "linestring" -> {
                coordsElement.forEach { ptElement ->
                    val ptArray = ptElement as? JsonArray
                    if (ptArray != null && ptArray.size >= 2) {
                        val lon = ptArray[0].jsonPrimitive.doubleOrNull ?: 0.0
                        val lat = ptArray[1].jsonPrimitive.doubleOrNull ?: 0.0
                        target.add(GeoPoint(latitude = lat, longitude = lon))
                    }
                }
            }
            "polygon" -> {
                val firstRing = coordsElement.firstOrNull() as? JsonArray
                firstRing?.forEach { ptElement ->
                    val ptArray = ptElement as? JsonArray
                    if (ptArray != null && ptArray.size >= 2) {
                        val lon = ptArray[0].jsonPrimitive.doubleOrNull ?: 0.0
                        val lat = ptArray[1].jsonPrimitive.doubleOrNull ?: 0.0
                        target.add(GeoPoint(latitude = lat, longitude = lon))
                    }
                }
            }
            "multipolygon" -> {
                val firstPolygon = coordsElement.firstOrNull() as? JsonArray
                val firstRing = firstPolygon?.firstOrNull() as? JsonArray
                firstRing?.forEach { ptElement ->
                    val ptArray = ptElement as? JsonArray
                    if (ptArray != null && ptArray.size >= 2) {
                        val lon = ptArray[0].jsonPrimitive.doubleOrNull ?: 0.0
                        val lat = ptArray[1].jsonPrimitive.doubleOrNull ?: 0.0
                        target.add(GeoPoint(latitude = lat, longitude = lon))
                    }
                }
            }
            else -> {
                findPointsGeneric(coordsElement, target)
            }
        }
    }

    private fun findPointsGeneric(element: JsonElement, target: MutableList<GeoPoint>) {
        if (element is JsonArray) {
            if (element.size >= 2 && element[0] is JsonPrimitive && element[1] is JsonPrimitive) {
                val lon = element[0].jsonPrimitive.doubleOrNull
                val lat = element[1].jsonPrimitive.doubleOrNull
                if (lon != null && lat != null) {
                    target.add(GeoPoint(latitude = lat, longitude = lon))
                    return
                }
            }
            element.forEach { child ->
                findPointsGeneric(child, target)
            }
        }
    }

    override fun serialize(encoder: Encoder, value: GeoJsonGeometryDto) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("Only Json format is supported")
        val jsonObject = buildJsonObject {
            put("type", JsonPrimitive(value.type))
            val coordsJson = when (value.type.lowercase()) {
                "point" -> {
                    val pt = value.coordinates.firstOrNull()
                    if (pt != null) {
                        buildJsonArray {
                            add(JsonPrimitive(pt.longitude))
                            add(JsonPrimitive(pt.latitude))
                        }
                    } else {
                        JsonArray(emptyList())
                    }
                }
                "linestring" -> {
                    buildJsonArray {
                        value.coordinates.forEach { pt ->
                            add(buildJsonArray {
                                add(JsonPrimitive(pt.longitude))
                                add(JsonPrimitive(pt.latitude))
                            })
                        }
                    }
                }
                "polygon" -> {
                    buildJsonArray {
                        add(buildJsonArray {
                            value.coordinates.forEach { pt ->
                                add(buildJsonArray {
                                    add(JsonPrimitive(pt.longitude))
                                    add(JsonPrimitive(pt.latitude))
                                })
                            }
                        })
                    }
                }
                "multipolygon" -> {
                    buildJsonArray {
                        add(buildJsonArray {
                            add(buildJsonArray {
                                value.coordinates.forEach { pt ->
                                    add(buildJsonArray {
                                        add(JsonPrimitive(pt.longitude))
                                        add(JsonPrimitive(pt.latitude))
                                    })
                                }
                            })
                        })
                    }
                }
                else -> {
                    buildJsonArray {
                        add(buildJsonArray {
                            value.coordinates.forEach { pt ->
                                add(buildJsonArray {
                                    add(JsonPrimitive(pt.longitude))
                                    add(JsonPrimitive(pt.latitude))
                                })
                            }
                        })
                    }
                }
            }
            put("coordinates", coordsJson)
        }
        jsonEncoder.encodeJsonElement(jsonObject)
    }
}

@Serializable(with = GeoJsonGeometryDtoSerializer::class)
data class GeoJsonGeometryDto(
    val type: String, // Point, LineString, Polygon, MultiPolygon
    val coordinates: List<GeoPoint>
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

@Serializable
data class AlertZonesResponseDto(
    val cityId: String,
    val generationVersion: String? = null,
    val zones: List<AlertZoneDto>
)

@Serializable
data class AlertZoneDto(
    val id: String,
    val baseAreaKey: String? = null,
    val risk: RiskLevelDto,
    val source: String,
    val geometry: GeoJsonGeometryDto
)
