package team.kid.roadsafety.infrastructure.location

import java.time.Instant
import kotlin.time.Duration
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.tracking.WarningAlert
import team.kid.roadsafety.domain.aggregates.tracking.WarningAlertType

class WarningAlertStateMachine(
    private val cooldown: Duration
) {
    private var lastRisk: MapAreaColor = MapAreaColor.GREEN
    private var lastAlertType: WarningAlertType? = null
    private var lastAlertAt: Instant? = null

    fun handleRisk(risk: MapAreaColor, now: Instant, offline: Boolean): WarningAlert? {
        if (risk == MapAreaColor.GREEN || risk == MapAreaColor.NONE) {
            lastRisk = MapAreaColor.GREEN
            lastAlertType = null
            lastAlertAt = null
            return null
        }

        val type = when (risk) {
            MapAreaColor.YELLOW -> WarningAlertType.YellowApproach
            MapAreaColor.RED -> WarningAlertType.RedZone
            else -> return null
        }
        val lastAt = lastAlertAt
        val isSameRiskWithoutCooldown = lastRisk == risk &&
            lastAlertType == type &&
            lastAt != null &&
            now.toEpochMilli() - lastAt.toEpochMilli() < cooldown.inWholeMilliseconds

        lastRisk = risk

        if (isSameRiskWithoutCooldown) {
            return null
        }

        lastAlertType = type
        lastAlertAt = now
        return createAlert(type, now, offline)
    }

    private fun createAlert(type: WarningAlertType, now: Instant, offline: Boolean): WarningAlert {
        return when (type) {
            WarningAlertType.YellowApproach -> WarningAlert(
                type = type,
                title = "Предупреждение",
                message = "Впереди пешеходный переход или опасный участок",
                occurredAt = now,
                offline = offline
            )
            WarningAlertType.RedZone -> WarningAlert(
                type = type,
                title = "Опасная зона",
                message = "Вы вошли в опасную зону",
                occurredAt = now,
                offline = offline
            )
        }
    }
}
