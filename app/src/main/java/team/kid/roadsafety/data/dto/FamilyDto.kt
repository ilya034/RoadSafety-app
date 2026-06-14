package team.kid.roadsafety.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateFamilyRequestDto(
    val name: String? = null,
    val cityId: String
)

@Serializable
data class UpdateFamilyCityRequestDto(
    val cityId: String
)

@Serializable
data class CreateFamilyResponseDto(
    val familyId: String,
    val createdByUserId: String
)

@Serializable
data class JoinFamilyByInviteCodeRequestDto(
    val inviteCode: String,
    val userRole: String
)

@Serializable
data class JoinFamilyByInviteCodeResponseDto(
    val userId: String,
    val familyId: String,
    val role: String
)

@Serializable
data class CreateInviteCodeRequestDto(
    val inviteCodeRole: String
)

@Serializable
data class InviteCodeResponseDto(
    val inviteCode: String
)

@Serializable
data class GetFamilyMembersResponseDto(
    val members: List<MemberDto>
)

@Serializable
data class MemberDto(
    val id: String,
    val role: String
)
