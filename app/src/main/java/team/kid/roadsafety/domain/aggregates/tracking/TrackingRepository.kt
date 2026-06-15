package team.kid.roadsafety.domain.aggregates.tracking

import team.kid.roadsafety.data.dto.ChildLocationsResponseDto
import team.kid.roadsafety.data.dto.NotificationsResponseDto
import team.kid.roadsafety.data.dto.SubmitLocationResponseDto

interface TrackingRepository {
    suspend fun submitChildLocation(latitude: Double, longitude: Double, accuracyMeters: Double? = null): Result<SubmitLocationResponseDto>
    suspend fun getChildrenLocations(): Result<ChildLocationsResponseDto>
    suspend fun getNotifications(unreadOnly: Boolean = false): Result<NotificationsResponseDto>
    suspend fun markNotificationRead(id: String): Result<Unit>
}
