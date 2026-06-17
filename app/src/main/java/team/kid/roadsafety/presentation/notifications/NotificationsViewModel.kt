package team.kid.roadsafety.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.kid.roadsafety.data.dto.NotificationDto
import team.kid.roadsafety.domain.aggregates.tracking.TrackingRepository
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadNotifications(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.notifications.isEmpty(),
                    isRefreshing = refresh,
                    loadError = null,
                    message = null
                )
            }

            trackingRepository.getNotifications(unreadOnly = false)
                .fold(
                    onSuccess = { response ->
                        _uiState.update {
                            it.copy(
                                notifications = response.notifications,
                                isLoading = false,
                                isRefreshing = false,
                                loadError = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                loadError = if (it.notifications.isEmpty()) {
                                    error.message ?: "Failed to load notifications"
                                } else {
                                    null
                                },
                                message = if (it.notifications.isNotEmpty()) {
                                    error.message ?: "Failed to refresh notifications"
                                } else {
                                    null
                                }
                            )
                        }
                    }
                )
        }
    }

    fun markRead(notificationId: String) {
        val notification = _uiState.value.notifications.firstOrNull { it.id == notificationId }
        if (notification?.readAt != null || notificationId in _uiState.value.markingReadIds) return

        val optimisticReadAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        _uiState.update {
            it.copy(
                notifications = it.notifications.markRead(setOf(notificationId), optimisticReadAt),
                markingReadIds = it.markingReadIds + notificationId,
                message = null
            )
        }

        viewModelScope.launch {
            trackingRepository.markNotificationRead(notificationId)
                .fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(markingReadIds = it.markingReadIds - notificationId)
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                markingReadIds = it.markingReadIds - notificationId,
                                message = error.message ?: "Failed to mark notification read"
                            )
                        }
                    }
                )
        }
    }

    fun markAllRead() {
        val state = _uiState.value
        if (state.isMarkingAllRead) return

        val unreadIds = state.notifications
            .filter { it.readAt == null && it.id !in state.markingReadIds }
            .map { it.id }
            .toSet()
        if (unreadIds.isEmpty()) return

        val optimisticReadAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        _uiState.update {
            it.copy(
                notifications = it.notifications.markRead(unreadIds, optimisticReadAt),
                markingReadIds = it.markingReadIds + unreadIds,
                isMarkingAllRead = true,
                message = null
            )
        }

        viewModelScope.launch {
            var failedCount = 0
            unreadIds.forEach { notificationId ->
                if (trackingRepository.markNotificationRead(notificationId).isFailure) {
                    failedCount++
                }
            }

            _uiState.update {
                it.copy(
                    markingReadIds = it.markingReadIds - unreadIds,
                    isMarkingAllRead = false,
                    message = if (failedCount > 0) {
                        "Failed to mark $failedCount notifications read"
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

data class NotificationsUiState(
    val notifications: List<NotificationDto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadError: String? = null,
    val message: String? = null,
    val markingReadIds: Set<String> = emptySet(),
    val isMarkingAllRead: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && loadError == null && notifications.isEmpty()

    val unreadCount: Int
        get() = notifications.count { it.readAt == null }
}

private fun List<NotificationDto>.markRead(
    notificationIds: Set<String>,
    readAt: String
): List<NotificationDto> {
    return map { notification ->
        if (notification.id in notificationIds) {
            notification.copy(readAt = readAt)
        } else {
            notification
        }
    }
}
