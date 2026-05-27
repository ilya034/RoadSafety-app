package team.kid.roadsafety.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import team.kid.roadsafety.data.local.SessionManager
import team.kid.roadsafety.data.remote.RoadSafetyApi

@Composable
fun ProfileScreen(sessionManager: SessionManager, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val api = remember { RoadSafetyApi(sessionManager) }
    var userId by remember { mutableStateOf("") }
    var familyId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        userId = sessionManager.userId.first() ?: ""
        familyId = sessionManager.familyId.first()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Профиль", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "User ID: $userId")
        Text(text = "Family ID: ${familyId ?: "Нет семьи"}")
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                scope.launch {
                    val refreshToken = sessionManager.refreshToken.first()
                    if (refreshToken != null) {
                        try {
                            api.logout(refreshToken)
                        } catch (e: Exception) {
                            // ignore error on logout
                        }
                    }
                    sessionManager.clearSession()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Выйти")
        }
    }
}
