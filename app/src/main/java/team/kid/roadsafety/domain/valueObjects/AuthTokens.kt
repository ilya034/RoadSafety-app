package team.kid.roadsafety.domain.valueObjects

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)
