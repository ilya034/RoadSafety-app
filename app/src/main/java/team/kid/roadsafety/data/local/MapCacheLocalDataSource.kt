package team.kid.roadsafety.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import team.kid.roadsafety.data.dto.MapCitiesResponseDto
import team.kid.roadsafety.data.dto.MapCityMetadataDto
import team.kid.roadsafety.data.dto.UserMapAreaFeatureCollectionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapCacheLocalDataSource @Inject constructor(
    @ApplicationContext context: Context,
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

    private companion object {
        const val CitiesKey = "cities"
    }
}
