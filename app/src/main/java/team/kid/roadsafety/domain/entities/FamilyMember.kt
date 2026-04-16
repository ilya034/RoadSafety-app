package team.kid.roadsafety.domain.entities

import team.kid.roadsafety.domain.enums.FamilyRole
import team.kid.roadsafety.domain.valueObjects.UserId

class FamilyMember (
    val userId : UserId,
    var role: FamilyRole
)