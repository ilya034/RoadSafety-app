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
import team.kid.roadsafety.data.remote.dto.CreateFamilyRequest
import team.kid.roadsafety.data.remote.dto.JoinFamilyByInviteCodeRequest

@Composable
fun FamilySelectionScreen(sessionManager: SessionManager) {
    var inviteCode by remember { mutableStateOf("") }
    var familyName by remember { mutableStateOf("") }
    var intendedRole by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf("Choice") } // Choice, Join, Create

    val scope = rememberCoroutineScope()
    val api = remember { RoadSafetyApi(sessionManager) }

    LaunchedEffect(Unit) {
        intendedRole = sessionManager.intendedRole.first()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Семья",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        when (mode) {
            "Choice" -> {
                if (intendedRole == "Parent") {
                    Button(
                        onClick = { mode = "Create" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Создать семью")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                OutlinedButton(
                    onClick = { mode = "Join" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Войти в семью")
                }
            }
            "Join" -> {
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    label = { Text("Код приглашения") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isLoading = true
                        error = null
                        scope.launch {
                            try {
                                val response = api.joinFamily(JoinFamilyByInviteCodeRequest(inviteCode))
                                sessionManager.saveFamilyId(response.familyId)
                            } catch (e: Exception) {
                                error = e.message ?: "Ошибка"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Войти")
                }
                TextButton(onClick = { mode = "Choice" }) {
                    Text("Назад")
                }
            }
            "Create" -> {
                OutlinedTextField(
                    value = familyName,
                    onValueChange = { familyName = it },
                    label = { Text("Название семьи") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isLoading = true
                        error = null
                        scope.launch {
                            try {
                                val response = api.createFamily(CreateFamilyRequest(familyName.takeIf { it.isNotBlank() }))
                                sessionManager.saveFamilyId(response.familyId)
                            } catch (e: Exception) {
                                error = e.message ?: "Ошибка"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Создать")
                }
                TextButton(onClick = { mode = "Choice" }) {
                    Text("Назад")
                }
            }
        }

        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
