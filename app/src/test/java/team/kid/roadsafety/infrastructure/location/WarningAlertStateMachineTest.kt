package team.kid.roadsafety.infrastructure.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor
import team.kid.roadsafety.domain.aggregates.tracking.WarningAlertType
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class WarningAlertStateMachineTest {
    @Test
    fun cooldownSuppressesRepeatedSameWarning() {
        val stateMachine = WarningAlertStateMachine(cooldown = 2.minutes)
        val now = Instant.parse("2026-06-16T00:00:00Z")

        val first = stateMachine.handleRisk(MapAreaColor.YELLOW, now, offline = false)
        val second = stateMachine.handleRisk(MapAreaColor.YELLOW, now.plusSeconds(30), offline = false)
        val third = stateMachine.handleRisk(MapAreaColor.YELLOW, now.plusSeconds(130), offline = false)

        assertEquals(WarningAlertType.YellowApproach, first?.type)
        assertNull(second)
        assertEquals(WarningAlertType.YellowApproach, third?.type)
    }

    @Test
    fun greenResetsWarningState() {
        val stateMachine = WarningAlertStateMachine(cooldown = 2.minutes)
        val now = Instant.parse("2026-06-16T00:00:00Z")

        assertNotNull(stateMachine.handleRisk(MapAreaColor.RED, now, offline = true))
        assertNull(stateMachine.handleRisk(MapAreaColor.GREEN, now.plusSeconds(10), offline = true))
        assertEquals(
            WarningAlertType.RedZone,
            stateMachine.handleRisk(MapAreaColor.RED, now.plusSeconds(20), offline = true)?.type
        )
    }
}
