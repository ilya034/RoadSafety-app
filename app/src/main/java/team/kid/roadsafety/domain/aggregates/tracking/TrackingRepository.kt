package team.kid.roadsafety.domain.aggregates.tracking

import team.kid.roadsafety.data.dto.RiskLevelDto
import team.kid.roadsafety.data.dto.SubmitLocationResponseDto

interface TrackingRepository {
    suspend fun submitChildLocation(latitude: Double, longitude: Double, accuracyMeters: Double? = null): Result<SubmitLocationResponseDto>
}
