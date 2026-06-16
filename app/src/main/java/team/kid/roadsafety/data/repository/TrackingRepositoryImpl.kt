package team.kid.roadsafety.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import team.kid.roadsafety.data.dto.SubmitLocationRequestDto
import team.kid.roadsafety.data.dto.SubmitLocationResponseDto
import team.kid.roadsafety.data.local.PendingLocationLocalDataSource
import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.domain.aggregates.tracking.TrackingRepository
import team.kid.roadsafety.infrastructure.parseErrorMessage
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class TrackingRepositoryImpl @Inject constructor(
    private val api: RoadSafetyApi,
    private val pendingLocationLocalDataSource: PendingLocationLocalDataSource
) : TrackingRepository {

    private val submitMutex = Mutex()

    override suspend fun submitChildLocation(latitude: Double, longitude: Double, accuracyMeters: Double?): Result<SubmitLocationResponseDto> {
        val request = SubmitLocationRequestDto(
            childId = null, // Defaults to the authenticated user (child)
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            recordedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        )

        return submitMutex.withLock {
            val flushResult = flushPendingLocations()
            if (flushResult.isFailure) {
                pendingLocationLocalDataSource.enqueue(request)
                return@withLock Result.failure(flushResult.exceptionOrNull() ?: Exception("Failed to submit pending locations"))
            }

            val currentResult = submitLocation(request)
            if (currentResult.isFailure) {
                pendingLocationLocalDataSource.enqueue(request)
            }
            currentResult
        }
    }

    private suspend fun flushPendingLocations(): Result<Unit> {
        val pendingLocations = pendingLocationLocalDataSource.getPendingLocations()
        var submittedCount = 0

        for (location in pendingLocations) {
            val result = submitLocation(location)
            if (result.isFailure) {
                pendingLocationLocalDataSource.removeFirst(submittedCount)
                return Result.failure(result.exceptionOrNull() ?: Exception("Failed to submit pending location"))
            }
            submittedCount++
        }

        pendingLocationLocalDataSource.removeFirst(submittedCount)
        return Result.success(Unit)
    }

    private suspend fun submitLocation(request: SubmitLocationRequestDto): Result<SubmitLocationResponseDto> {
        return try {
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

    override suspend fun getChildrenLocations() = try {
        val response = api.getChildrenLocations()
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception(response.parseErrorMessage("Failed to load children locations")))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getNotifications(unreadOnly: Boolean) = try {
        val response = api.getNotifications(unreadOnly)
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception(response.parseErrorMessage("Failed to load notifications")))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun markNotificationRead(id: String) = try {
        val response = api.markNotificationRead(id)
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(response.parseErrorMessage("Failed to mark notification read")))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
