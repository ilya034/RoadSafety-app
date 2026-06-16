package team.kid.roadsafety.domain.aggregates.tracking

import java.time.Instant

enum class WarningAlertType {
    YellowApproach,
    RedZone
}

data class WarningAlert(
    val type: WarningAlertType,
    val title: String,
    val message: String,
    val occurredAt: Instant,
    val offline: Boolean
)
