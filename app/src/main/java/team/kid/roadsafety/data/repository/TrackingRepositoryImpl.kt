package team.kid.roadsafety.data.repository

import team.kid.roadsafety.data.dto.SubmitLocationRequestDto
import team.kid.roadsafety.data.dto.SubmitLocationResponseDto
import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.domain.aggregates.tracking.TrackingRepository
import team.kid.roadsafety.infrastructure.parseErrorMessage
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class TrackingRepositoryImpl @Inject constructor(
    private val api: RoadSafetyApi
) : TrackingRepository {

    override suspend fun submitChildLocation(latitude: Double, longitude: Double, accuracyMeters: Double?): Result<SubmitLocationResponseDto> {
        return try {
            val request = SubmitLocationRequestDto(
                childId = null, // Defaults to the authenticated user (child)
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracyMeters,
                recordedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            )
            val response = api.submitChildLocation(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.parseErrorMessage("Failed to submit location")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
