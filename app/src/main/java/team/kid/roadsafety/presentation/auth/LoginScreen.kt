package team.kid.roadsafety.presentation.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

import team.kid.roadsafety.data.dto.UserResponseDto

@Composable
fun LoginScreen(
    onRegisterClick: () -> Unit,
    onLoginSuccess: (UserResponseDto?) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Вход",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        AuthTextField(
            value = state.login,
            onValueChange = viewModel::onLoginChanged,
            placeholder = "Логин/телефон/почта",
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AuthTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChanged,
            placeholder = "Пароль",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.padding(bottom = 40.dp)
        )

        AuthButton(
            text = "Войти",
            isLoading = state.isLoading,
            onClick = {
                viewModel.login(onSuccess = onLoginSuccess)
            },
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Text(
            text = "  Ещё нет аккаунта?\nЗарегистрироваться",
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onRegisterClick() }
        )
    }
}
