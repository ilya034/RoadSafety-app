package team.kid.roadsafety.domain.aggregates

import team.kid.roadsafety.domain.valueObjects.UserContacts
import team.kid.roadsafety.domain.valueObjects.UserId
import team.kid.roadsafety.domain.valueObjects.UserProfile

class User (
    val id: UserId,
    val profile: UserProfile,
    val contacts: UserContacts
)