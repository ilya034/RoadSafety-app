package team.kid.roadsafety.domain.aggregates.user

import team.kid.roadsafety.domain.UserId

class User (
    val id: UserId,
    val profile: UserProfile,
    val contacts: UserContacts
)