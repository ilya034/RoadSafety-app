package team.kid.roadsafety.domain.aggregates.family

import team.kid.roadsafety.domain.UserId

class FamilyMember (
    val userId : UserId,
    var role: FamilyRole
)