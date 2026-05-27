package team.kid.roadsafety.domain.aggregates.session

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)