package team.kid.roadsafety.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateFamilyRequest(
    val name: String? = null
)

@Serializable
data class CreateFamilyResponse(
    val familyId: String,
    val name: String? = null,
    val createdByUserId: String
)

@Serializable
data class GetFamilyMembersResponse(
    val members: List<MemberDto>
)

@Serializable
data class MemberDto(
    val id: String,
    val role: String
)

@Serializable
data class CreateInviteCodeRequest(
    val inviteCodeRole: String
)

@Serializable
data class CreateInviteCodeResponse(
    val inviteCode: String
)

@Serializable
data class JoinFamilyByInviteCodeRequest(
    val inviteCode: String
)

@Serializable
data class JoinFamilyByInviteCodeResponse(
    val userId: String,
    val familyId: String,
    val role: String
)
