package team.kid.roadsafety.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class FamilyCreateRequestDto(
    val name: String
)

@Serializable
data class FamilyJoinRequestDto(
    val inviteCode: String
)

@Serializable
data class FamilyResponseDto(
    val id: String,
    val name: String,
    val createdAt: String,
    val createdByUserId: String
)

@Serializable
data class FamilyMemberResponseDto(
    val id: String,
    val familyId: String,
    val userId: String,
    val role: String,
    val joinedAt: String
)

@Serializable
data class InviteCodeResponseDto(
    val code: String,
    val deepLink: String,
    val expiresAt: String
)
