package team.kid.roadsafety.domain.aggregates.family

import team.kid.roadsafety.domain.FamilyId
import team.kid.roadsafety.domain.aggregates.user.UserRole

interface FamilyRepository {
    suspend fun createFamily(name: String): Result<FamilyEntity>
    suspend fun joinFamily(inviteCode: String, role: UserRole): Result<FamilyMemberEntity>
    suspend fun generateInviteCode(role: UserRole): Result<String>
    suspend fun getFamily(familyId: FamilyId): Result<FamilyEntity>
    suspend fun getFamilyMembers(familyId: FamilyId): Result<List<FamilyMemberEntity>>

    fun setSelectedRole(role: UserRole)
    fun getSelectedRole(): UserRole?
    fun clearData()
}

data class FamilyEntity(
    val id: FamilyId,
    val name: String,
    val createdAt: String
)

data class FamilyMemberEntity(
    val id: String,
    val familyId: FamilyId,
    val userId: String,
    val role: UserRole,
    val joinedAt: String
)
