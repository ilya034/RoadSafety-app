package team.kid.roadsafety.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class FamilyCreateRequestDto(
    val name: String? = null
)

@Serializable
data class FamilyJoinRequestDto(
    val inviteCode: String,
    val role: String
)

@Serializable
data class JoinFamilyByInviteCodeRequestDto(
    val inviteCode: String,
    val role: String
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
data class FamilyResponseDto(
    val familyId: String,
    val name: String? = null,
    val createdByUserId: String
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

@Serializable
data class InviteCodeResponseDto(
    val inviteCode: String
)
