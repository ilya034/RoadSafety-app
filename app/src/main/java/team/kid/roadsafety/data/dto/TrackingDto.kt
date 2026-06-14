package team.kid.roadsafety.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubmitLocationRequestDto(
    val childId: String? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val recordedAt: String? = null
)

@Serializable
data class SubmitLocationResponseDto(
    val childId: String,
    val currentRisk: RiskLevelDto,
    val matchedUserAreaId: String? = null,
    val matchedBaseAreaId: String? = null,
    val lastUpdatedAt: String
)

@Serializable
data class ChildLocationResponseDto(
    val childId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val currentRisk: RiskLevelDto,
    val lastUpdatedAt: String
)

@Serializable
data class ChildStatsResponseDto(
    val childId: String,
    val totalScore: Int,
    val rating: Int
)
