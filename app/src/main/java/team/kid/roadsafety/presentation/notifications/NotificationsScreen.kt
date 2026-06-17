package team.kid.roadsafety.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import team.kid.roadsafety.data.dto.NotificationDto
import team.kid.roadsafety.data.dto.NotificationTypeDto
import team.kid.roadsafety.data.dto.RiskLevelDto

@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.loadError != null -> {
                    NotificationsMessageState(
                        title = "Notifications unavailable",
                        message = state.loadError ?: "Failed to load notifications",
                        actionLabel = "Retry",
                        onAction = { viewModel.loadNotifications() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                state.isEmpty -> {
                    NotificationsMessageState(
                        title = "No notifications",
                        message = "New safety updates will appear here.",
                        actionLabel = "Refresh",
                        onAction = { viewModel.loadNotifications(refresh = true) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.notifications,
                            key = { it.id }
                        ) { notification ->
                            NotificationItem(
                                notification = notification,
                                isMarkingRead = notification.id in state.markingReadIds,
                                onClick = { viewModel.markRead(notification.id) }
                            )
                        }
                    }
                }
            }
        }

        state.message?.let { message ->
            LaunchedEffect(message) {
                delay(3000)
                viewModel.clearMessage()
            }
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(message)
            }
        }
    }
}


@Composable
private fun NotificationsMessageState(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onAction,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationDto,
    isMarkingRead: Boolean,
    onClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(notification.risk == RiskLevelDto.Red) }

    val iconBackground = when (notification.risk) {
        RiskLevelDto.Red -> Color(0xFFFF4B2B)
        RiskLevelDto.Yellow -> Color(0xFFFFB800)
        RiskLevelDto.Green -> Color(0xFF00A36C)
        else -> Color(0xFF00A36C)
    }

    val iconVector = when (notification.risk) {
        RiskLevelDto.Red -> Icons.Default.PriorityHigh
        RiskLevelDto.Yellow -> Icons.Default.Notifications
        RiskLevelDto.Green -> Icons.Default.Check
        else -> Icons.Default.Check
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { isExpanded = !isExpanded }),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBEBEB)),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1A1C1E),
                    maxLines = if (isExpanded) 10 else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            // Right Action Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF00A36C), RoundedCornerShape(10.dp))
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isMarkingRead) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.NorthEast,
                        contentDescription = "Action",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

