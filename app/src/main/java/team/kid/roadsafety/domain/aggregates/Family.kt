package team.kid.roadsafety.domain.aggregates

import team.kid.roadsafety.domain.entities.FamilyMember
import team.kid.roadsafety.domain.valueObjects.FamilyId
import team.kid.roadsafety.domain.valueObjects.UserId

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