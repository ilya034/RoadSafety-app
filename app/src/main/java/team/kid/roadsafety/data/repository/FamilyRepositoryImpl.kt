package team.kid.roadsafety.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import team.kid.roadsafety.data.dto.CreateFamilyRequestDto
import team.kid.roadsafety.data.dto.CreateInviteCodeRequestDto
import team.kid.roadsafety.data.dto.JoinFamilyByInviteCodeRequestDto
import team.kid.roadsafety.data.dto.MapCityDto
import team.kid.roadsafety.data.dto.UpdateFamilyCityRequestDto
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
                Result.success(response.body()?.cities?.map { it.toDomain() } ?: fallbackCities)
            } else {
                Result.success(fallbackCities)
            }
        } catch (e: Exception) {
            Result.success(fallbackCities)
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
                Result.success(response.body()!!.members.map { body ->
                    FamilyMemberEntity(
                        id = UUID.randomUUID().toString(),
                        familyId = familyId,
                        userId = body.id,
                        role = UserRole.valueOf(body.role.uppercase()),
                        joinedAt = ""
                    )
                })
            } else {
                Result.failure(Exception(response.parseErrorMessage("Get members failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
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

    private companion object {
        val fallbackCities = listOf(MapCity(cityId = "ekb", name = "Екатеринбург"))
    }
}
