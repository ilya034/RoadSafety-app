package team.kid.roadsafety.infrastructure.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import team.kid.roadsafety.domain.aggregates.tracking.WarningAlert

object WarningAlertEvents {
    private val _current = MutableStateFlow<WarningAlert?>(null)
    val current: StateFlow<WarningAlert?> = _current

    fun show(alert: WarningAlert) {
        _current.value = alert
    }

    fun dismiss() {
        _current.value = null
    }
}
