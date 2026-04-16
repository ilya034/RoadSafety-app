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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onRegisterClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

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
            onClick = {
                viewModel.onLoginClicked()
                // For now, let's just trigger success to see navigation
                onLoginSuccess()
            },
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Ещё нет аккаунта? Зарегистрироваться",
            color = Color.Gray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onRegisterClick() }
        )
    }
}
