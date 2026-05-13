package team.kid.roadsafety.domain.aggregates.family

import team.kid.roadsafety.domain.FamilyId
import team.kid.roadsafety.domain.UserId

class Family (
    val id: FamilyId,
    members: List<FamilyMember>
) {
    private val _members = members.toMutableList()
    val members: List<FamilyMember> get() = _members

    fun addMember(member: FamilyMember) {
        _members.add(member)
    }

    fun removeMember(userId: UserId) {
        _members.removeAll { it.userId == userId }
    }
}