package team.kid.roadsafety.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import team.kid.roadsafety.data.dto.CreateFamilyRequestDto
import team.kid.roadsafety.data.dto.CreateInviteCodeRequestDto
import team.kid.roadsafety.data.dto.GetFamilyMembersResponseDto
import team.kid.roadsafety.data.dto.JoinFamilyByInviteCodeRequestDto
import team.kid.roadsafety.data.dto.MapCityDto
import team.kid.roadsafety.data.dto.UpdateFamilyCityRequestDto
import team.kid.roadsafety.data.local.MapCacheLocalDataSource
import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.domain.FamilyId
import team.kid.roadsafety.domain.aggregates.family.FamilyEntity
import team.kid.roadsafety.domain.aggregates.family.FamilyMemberEntity
import team.kid.roadsafety.domain.aggregates.family.FamilyRepository
import team.kid.roadsafety.domain.aggregates.map.MapCity
import team.kid.roadsafety.domain.aggregates.map.MapCityBbox
import team.kid.roadsafety.domain.aggregates.user.UserRole
import team.kid.roadsafety.infrastructure.parseErrorMessage
import java.util.UUID
import javax.inject.Inject

class FamilyRepositoryImpl @Inject constructor(
    private val api: RoadSafetyApi,
    private val mapCache: MapCacheLocalDataSource,
    private val json: Json,
    @ApplicationContext context: Context
) : FamilyRepository {

    private val prefs = context.getSharedPreferences("family_prefs", Context.MODE_PRIVATE)

    override suspend fun createFamily(name: String, cityId: String): Result<FamilyEntity> {
        return try {
            val response = api.createFamily(CreateFamilyRequestDto(name = name, cityId = cityId))
            if (response.isSuccessful) {
                val body = response.body()!!
                setSelectedCityId(cityId)
                Result.success(
                    FamilyEntity(
                        id = FamilyId(UUID.fromString(body.familyId)),
                        name = name,
                        createdAt = "" // Removed from contract
                    )
                )
            } else {
                Result.failure(Exception(response.parseErrorMessage("Family creation failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSupportedCities(): Result<List<MapCity>> {
        return try {
            val response = api.getMapCities()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    mapCache.saveCities(body)
                }
                val apiCities = body?.cities?.map { it.toDomain() }.orEmpty()
                val mergedCities = (apiCities + fallbackCities).distinctBy { it.cityId }
                Result.success(mergedCities)
            } else {
                Result.success(cachedCities())
            }
        } catch (e: Exception) {
            Result.success(cachedCities())
        }
    }

    override suspend fun updateFamilyCity(familyId: FamilyId, cityId: String): Result<Unit> {
        return try {
            val response = api.updateFamilyCity(
                familyId = familyId.value.toString(),
                request = UpdateFamilyCityRequestDto(cityId)
            )
            if (response.isSuccessful) {
                setSelectedCityId(cityId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.parseErrorMessage("Family city update failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinFamily(inviteCode: String, role: UserRole): Result<FamilyMemberEntity> {
        return try {
            val response = api.joinFamily(JoinFamilyByInviteCodeRequestDto(inviteCode, role.name))
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.success(
                    FamilyMemberEntity(
                        id = UUID.randomUUID().toString(),
                        familyId = FamilyId(UUID.fromString(body.familyId)),
                        userId = body.userId,
                        role = UserRole.valueOf(body.role.uppercase()),
                        joinedAt = ""
                    )
                )
            } else {
                Result.failure(Exception(response.parseErrorMessage("Join family failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateInviteCode(role: UserRole): Result<String> {
        return try {
            val response = api.createInviteCode(CreateInviteCodeRequestDto(role.name))
            if (response.isSuccessful) {
                Result.success(response.body()!!.inviteCode)
            } else {
                Result.failure(Exception(response.parseErrorMessage("Invite code generation failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun getFamilyMembers(familyId: FamilyId): Result<List<FamilyMemberEntity>> {
        return try {
            val response = api.getFamilyMembers(familyId.value.toString())
            if (response.isSuccessful) {
                val body = response.body()!!
                saveCachedMembers(familyId, body)
                Result.success(body.members.map { bodyMember ->
                    FamilyMemberEntity(
                        id = UUID.randomUUID().toString(),
                        familyId = familyId,
                        userId = bodyMember.id,
                        role = UserRole.valueOf(bodyMember.role.uppercase()),
                        joinedAt = ""
                    )
                })
            } else {
                Result.failure(Exception(response.parseErrorMessage("Get members failed")))
            }
        } catch (e: Exception) {
            val cachedDto = getCachedMembers(familyId)
            if (cachedDto != null) {
                Result.success(cachedDto.members.map { bodyMember ->
                    FamilyMemberEntity(
                        id = UUID.randomUUID().toString(),
                        familyId = familyId,
                        userId = bodyMember.id,
                        role = UserRole.valueOf(bodyMember.role.uppercase()),
                        joinedAt = ""
                    )
                })
            } else {
                Result.failure(e)
            }
        }
    }

    private fun saveCachedMembers(familyId: FamilyId, body: GetFamilyMembersResponseDto) {
        try {
            val jsonStr = json.encodeToString(GetFamilyMembersResponseDto.serializer(), body)
            prefs.edit().putString("cached_members_${familyId.value}", jsonStr).apply()
        } catch (e: Exception) {
            // Ignore serialization exceptions
        }
    }

    private fun getCachedMembers(familyId: FamilyId): GetFamilyMembersResponseDto? {
        val jsonStr = prefs.getString("cached_members_${familyId.value}", null) ?: return null
        return try {
            json.decodeFromString(GetFamilyMembersResponseDto.serializer(), jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    override fun setSelectedRole(role: UserRole) {
        prefs.edit().putString("selected_role", role.name).apply()
    }

    override fun getSelectedRole(): UserRole? {
        return prefs.getString("selected_role", null)?.let { UserRole.valueOf(it) }
    }

    override fun setSelectedCityId(cityId: String) {
        prefs.edit().putString("selected_city_id", cityId).apply()
    }

    override fun getSelectedCityId(): String? {
        return prefs.getString("selected_city_id", null)
    }

    override fun clearData() {
        prefs.edit().clear().apply()
    }

    private fun MapCityDto.toDomain(): MapCity {
        return MapCity(
            cityId = cityId,
            name = name,
            bbox = bbox?.let {
                MapCityBbox(
                    minLon = it.minLon,
                    minLat = it.minLat,
                    maxLon = it.maxLon,
                    maxLat = it.maxLat
                )
            }
        )
    }

    private fun cachedCities(): List<MapCity> {
        val cached = mapCache.getCities()?.cities?.map { it.toDomain() } ?: fallbackCities
        return (cached + fallbackCities).distinctBy { it.cityId }
    }

    private companion object {
        val fallbackCities = listOf(
            MapCity(
                cityId = "ekb",
                name = "Екатеринбург",
                bbox = MapCityBbox(
                    minLon = 60.3,
                    minLat = 56.7,
                    maxLon = 60.9,
                    maxLat = 56.9
                )
            ),
            MapCity(
                cityId = "salekhard",
                name = "Салехард",
                bbox = MapCityBbox(
                    minLon = 66.5,
                    minLat = 66.5,
                    maxLon = 66.7,
                    maxLat = 66.6
                )
            )
        )
    }
}
