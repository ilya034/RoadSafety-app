package team.kid.roadsafety.domain.aggregates.user

import team.kid.roadsafety.domain.aggregates.user.Email

data class UserContacts(
    val email: Email,
    val phone: PhoneNumber,
)