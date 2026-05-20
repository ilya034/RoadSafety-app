package team.kid.roadsafety.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import team.kid.roadsafety.data.dto.FamilyCreateRequestDto
import team.kid.roadsafety.data.dto.FamilyJoinRequestDto
import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.domain.FamilyId
import team.kid.roadsafety.domain.aggregates.family.FamilyEntity
import team.kid.roadsafety.domain.aggregates.family.FamilyMemberEntity
import team.kid.roadsafety.domain.aggregates.family.FamilyRepository
import team.kid.roadsafety.domain.aggregates.user.UserRole
import java.util.UUID
import javax.inject.Inject

class FamilyRepositoryImpl @Inject constructor(
    private val api: RoadSafetyApi,
    @ApplicationContext context: Context
) : FamilyRepository {

    private val prefs = context.getSharedPreferences("family_prefs", Context.MODE_PRIVATE)

    override suspend fun createFamily(name: String): Result<FamilyEntity> {
        return try {
            val response = api.createFamily(FamilyCreateRequestDto(name))
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.success(
                    FamilyEntity(
                        id = FamilyId(UUID.fromString(body.id)),
                        name = body.name,
                        createdAt = body.createdAt
                    )
                )
            } else {
                Result.failure(Exception("Family creation failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinFamily(inviteCode: String): Result<FamilyMemberEntity> {
        return try {
            val response = api.joinFamily(FamilyJoinRequestDto(inviteCode))
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.success(
                    FamilyMemberEntity(
                        id = body.id,
                        familyId = FamilyId(UUID.fromString(body.familyId)),
                        userId = body.userId,
                        role = UserRole.valueOf(body.role.uppercase()),
                        joinedAt = body.joinedAt
                    )
                )
            } else {
                Result.failure(Exception("Join family failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateInviteCode(familyId: FamilyId): Result<String> {
        return try {
            val response = api.createInviteCode(familyId.value.toString())
            if (response.isSuccessful) {
                Result.success(response.body()!!.code)
            } else {
                Result.failure(Exception("Invite code generation failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFamily(familyId: FamilyId): Result<FamilyEntity> {
        return try {
            val response = api.getFamily(familyId.value.toString())
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.success(
                    FamilyEntity(
                        id = FamilyId(UUID.fromString(body.id)),
                        name = body.name,
                        createdAt = body.createdAt
                    )
                )
            } else {
                Result.failure(Exception("Get family failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFamilyMembers(familyId: FamilyId): Result<List<FamilyMemberEntity>> {
        return try {
            val response = api.getFamilyMembers(familyId.value.toString())
            if (response.isSuccessful) {
                Result.success(response.body()!!.map { body ->
                    FamilyMemberEntity(
                        id = body.id,
                        familyId = FamilyId(UUID.fromString(body.familyId)),
                        userId = body.userId,
                        role = UserRole.valueOf(body.role.uppercase()),
                        joinedAt = body.joinedAt
                    )
                })
            } else {
                Result.failure(Exception("Get members failed: ${response.code()}"))
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
}
