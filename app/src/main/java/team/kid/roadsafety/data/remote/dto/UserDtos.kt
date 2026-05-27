package team.kid.roadsafety.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val patronymic: String? = null,
    val birthDate: String? = null,
    val familyId: String? = null
)
