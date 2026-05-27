package team.kid.roadsafety.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val FAMILY_ID = stringPreferencesKey("family_id")
        private val USER_ID = stringPreferencesKey("user_id")
        private val INTENDED_ROLE = stringPreferencesKey("intended_role")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val familyId: Flow<String?> = context.dataStore.data.map { it[FAMILY_ID] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val intendedRole: Flow<String?> = context.dataStore.data.map { it[INTENDED_ROLE] }

    suspend fun saveSession(userId: String, accessToken: String, refreshToken: String, intendedRole: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
            preferences[ACCESS_TOKEN] = accessToken
            preferences[REFRESH_TOKEN] = refreshToken
            if (intendedRole != null) {
                preferences[INTENDED_ROLE] = intendedRole
            }
        }
    }

    suspend fun saveFamilyId(familyId: String?) {
        context.dataStore.edit { preferences ->
            if (familyId == null) {
                preferences.remove(FAMILY_ID)
            } else {
                preferences[FAMILY_ID] = familyId
            }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
