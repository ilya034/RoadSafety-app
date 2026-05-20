package team.kid.roadsafety.domain

import java.util.UUID

@JvmInline
value class UserId(val value: UUID)

@JvmInline
value class FamilyId(val value: UUID)

@JvmInline
value class AreaId(val value: UUID)

@JvmInline
value class CityId(val value: String)

@JvmInline
value class SessionId(val value: UUID)
