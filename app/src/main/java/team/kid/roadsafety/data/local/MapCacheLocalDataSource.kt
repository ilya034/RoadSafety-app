package team.kid.roadsafety.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import team.kid.roadsafety.data.dto.AlertZonesResponseDto
import team.kid.roadsafety.data.dto.MapCitiesResponseDto
import team.kid.roadsafety.data.dto.MapCityMetadataDto
import team.kid.roadsafety.data.dto.UserMapAreaFeatureCollectionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapCacheLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val prefs = context.getSharedPreferences("map_cache", Context.MODE_PRIVATE)

    fun getCityMetadata(cityId: String): MapCityMetadataDto? {
        return prefs.getString(cityMetadataKey(cityId), null)?.decodeOrNull()
    }

    fun saveCityMetadata(cityId: String, metadata: MapCityMetadataDto) {
        prefs.edit()
            .putString(cityMetadataKey(cityId), json.encodeToString(metadata))
            .apply()
    }

    fun getUserAreas(familyId: String, childId: String?): UserMapAreaFeatureCollectionDto? {
        return prefs.getString(userAreasKey(familyId, childId), null)?.decodeOrNull()
    }

    fun saveUserAreas(familyId: String, childId: String?, areas: UserMapAreaFeatureCollectionDto) {
        prefs.edit()
            .putString(userAreasKey(familyId, childId), json.encodeToString(areas))
            .apply()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun getAlertZones(cityId: String, familyId: String, childId: String?): AlertZonesResponseDto? {
        val key = alertZonesKey(cityId, familyId, childId)
        if (prefs.contains(key)) {
            val oldData = prefs.getString(key, null)
            prefs.edit().remove(key).apply()
            if (oldData != null) {
                try {
                    getAlertZonesFile(cityId, familyId, childId).writeText(oldData)
                } catch (_: Exception) {}
            }
        }

        val file = getAlertZonesFile(cityId, familyId, childId)
        if (!file.exists()) return null
        return try {
            file.inputStream().use { input ->
                json.decodeFromStream<AlertZonesResponseDto>(input)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun saveAlertZonesStream(cityId: String, familyId: String, childId: String?, inputStream: java.io.InputStream) {
        try {
            val file = getAlertZonesFile(cityId, familyId, childId)
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
        } catch (_: Exception) {}

        val key = alertZonesKey(cityId, familyId, childId)
        prefs.edit()
            .remove(key)
            .putString(ActiveAlertCityIdKey, cityId)
            .putString(ActiveAlertFamilyIdKey, familyId)
            .putString(ActiveAlertChildIdKey, childId)
            .apply()
    }

    fun saveAlertZones(cityId: String, familyId: String, childId: String?, zones: AlertZonesResponseDto) {
        try {
            val jsonString = json.encodeToString(zones)
            getAlertZonesFile(cityId, familyId, childId).writeText(jsonString)
        } catch (_: Exception) {}

        val key = alertZonesKey(cityId, familyId, childId)
        prefs.edit()
            .remove(key)
            .putString(ActiveAlertCityIdKey, cityId)
            .putString(ActiveAlertFamilyIdKey, familyId)
            .putString(ActiveAlertChildIdKey, childId)
            .apply()
    }

    private fun getAlertZonesFile(cityId: String, familyId: String, childId: String?): java.io.File {
        val fileName = "alert_zones_${cityId}_${familyId}_${childId ?: "all"}.json"
        return java.io.File(context.filesDir, fileName)
    }

    fun getActiveAlertZones(): AlertZonesResponseDto? {
        val cityId = prefs.getString(ActiveAlertCityIdKey, null) ?: return null
        val familyId = prefs.getString(ActiveAlertFamilyIdKey, null) ?: return null
        val childId = prefs.getString(ActiveAlertChildIdKey, null)
        return getAlertZones(cityId, familyId, childId)
    }

    fun getCities(): MapCitiesResponseDto? {
        return prefs.getString(CitiesKey, null)?.decodeOrNull()
    }

    fun saveCities(cities: MapCitiesResponseDto) {
        prefs.edit()
            .putString(CitiesKey, json.encodeToString(cities))
            .apply()
    }

    private inline fun <reified T> String.decodeOrNull(): T? {
        return try {
            json.decodeFromString<T>(this)
        } catch (_: Exception) {
            null
        }
    }

    private fun cityMetadataKey(cityId: String): String = "city_metadata_$cityId"

    private fun userAreasKey(familyId: String, childId: String?): String {
        return "user_areas_${familyId}_${childId ?: "all"}"
    }

    private fun alertZonesKey(cityId: String, familyId: String, childId: String?): String {
        return "alert_zones_${cityId}_${familyId}_${childId ?: "all"}"
    }

    private companion object {
        const val CitiesKey = "cities"
        const val ActiveAlertCityIdKey = "active_alert_city_id"
        const val ActiveAlertFamilyIdKey = "active_alert_family_id"
        const val ActiveAlertChildIdKey = "active_alert_child_id"
    }
}
