package team.kid.roadsafety.domain.valueObjects

import androidx.compose.ui.text.intl.Locale

data class UserProfile(
    var firstName: String?,
    var lastName: String?,
    var patronymic: String?,
    var birthdate: Locale?
)
