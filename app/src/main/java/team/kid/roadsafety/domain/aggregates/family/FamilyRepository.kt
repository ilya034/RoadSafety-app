package team.kid.roadsafety.domain.aggregates.family

import team.kid.roadsafety.domain.FamilyId
import team.kid.roadsafety.domain.aggregates.map.MapCity
import team.kid.roadsafety.domain.aggregates.user.UserRole

interface FamilyRepository {
    suspend fun createFamily(name: String, cityId: String): Result<FamilyEntity>
    suspend fun joinFamily(inviteCode: String, role: UserRole): Result<FamilyMemberEntity>
    suspend fun generateInviteCode(role: UserRole): Result<String>
    suspend fun getFamilyMembers(familyId: FamilyId): Result<List<FamilyMemberEntity>>
    suspend fun getSupportedCities(): Result<List<MapCity>>
    suspend fun updateFamilyCity(familyId: FamilyId, cityId: String): Result<Unit>

    fun setSelectedRole(role: UserRole)
    fun getSelectedRole(): UserRole?
    fun setSelectedCityId(cityId: String)
    fun getSelectedCityId(): String?
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
    val joinedAt: String,
    val displayName: String = "",
    val login: String = ""
) {
    val displayLabel: String
        get() = displayName.ifBlank { login }.ifBlank { "User ${userId.take(8)}" }
}
