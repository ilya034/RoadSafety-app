package team.kid.roadsafety.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import team.kid.roadsafety.data.local.SessionManager
import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.data.remote.dto.LoginRequest
import team.kid.roadsafety.data.remote.dto.RegisterRequest

@Composable
fun AuthScreen(sessionManager: SessionManager) {
    var isLogin by remember { mutableStateOf(true) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Parent") } // UI only for now
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val api = remember { RoadSafetyApi(sessionManager) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLogin) "Вход" else "Регистрация",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (!isLogin) {
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Роль: ")
                RadioButton(
                    selected = role == "Parent",
                    onClick = { role = "Parent" }
                )
                Text("Родитель")
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(
                    selected = role == "Child",
                    onClick = { role = "Child" }
                )
                Text("Ребёнок")
            }
        }

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Почта / Логин") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isLoading = true
                error = null
                scope.launch {
                    try {
                        val response = if (isLogin) {
                            api.login(LoginRequest(login, password))
                        } else {
                            api.register(RegisterRequest(login, password))
                        }
                        sessionManager.saveSession(
                            userId = response.userId,
                            accessToken = response.accessToken,
                            refreshToken = response.refreshToken,
                            intendedRole = if (!isLogin) role else null
                        )
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isLogin) "Войти" else "Продолжить")
            }
        }

        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "Регистрация" else "Уже есть аккаунт? Войти")
        }
    }
}
