package team.kid.roadsafety.domain.aggregates.user

data class UserContacts(
    val email: Email,
    val phone: PhoneNumber,
)