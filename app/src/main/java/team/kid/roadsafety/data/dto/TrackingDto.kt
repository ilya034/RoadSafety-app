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
    val displayName: String = "",
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val currentRisk: RiskLevelDto,
    val lastUpdatedAt: String
)

@Serializable
data class ChildLocationsResponseDto(
    val children: List<ChildLocationResponseDto>
)

@Serializable
data class ChildStatsResponseDto(
    val childId: String,
    val totalScore: Int,
    val rating: Int
)

@Serializable
data class NotificationsResponseDto(
    val notifications: List<NotificationDto>
)

@Serializable
data class NotificationDto(
    val id: String,
    val recipientUserId: String,
    val childId: String? = null,
    val type: NotificationTypeDto,
    val title: String,
    val body: String,
    val risk: RiskLevelDto? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: String,
    val readAt: String? = null
)

@Serializable
enum class NotificationTypeDto {
    ChildEnteredRedZone
}

@Serializable
data class RegisterDeviceTokenRequestDto(
    val token: String,
    val platform: DevicePlatformDto = DevicePlatformDto.Android
)

@Serializable
enum class DevicePlatformDto {
    Android
}
