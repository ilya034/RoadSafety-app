package team.kid.roadsafety.domain.valueObjects

import java.time.LocalDate

data class UserProfile(
    var firstName: String?,
    var lastName: String?,
    var patronymic: String?,
    var birthdate: LocalDate?
)
