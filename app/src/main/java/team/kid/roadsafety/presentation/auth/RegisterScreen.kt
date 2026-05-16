package team.kid.roadsafety.presentation.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import team.kid.roadsafety.presentation.theme.TextGrey

@Composable
fun RegisterScreen(
    onLoginClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var isAgreed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Регистрация",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 30.dp)
        )

        AuthTextField(
            value = state.login,
            onValueChange = viewModel::onLoginChanged,
            placeholder = "Логин/телефон/почта",
            modifier = Modifier.padding(bottom = 12.dp)
        )

        AuthTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChanged,
            placeholder = "Пароль",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        AuthTextField(
            value = state.firstName,
            onValueChange = viewModel::onFirstNameChanged,
            placeholder = "Имя",
            modifier = Modifier.padding(bottom = 12.dp)
        )

        AuthTextField(
            value = state.lastName,
            onValueChange = viewModel::onLastNameChanged,
            placeholder = "Фамилия",
            modifier = Modifier.padding(bottom = 24.dp)
        )

        AuthButton(
            text = "Зарегистрироваться",
            isLoading = state.isLoading,
            enabled = isAgreed && state.login.isNotBlank() && state.password.length >= 8,
            onClick = {
                viewModel.register(onSuccess = onRegisterSuccess)
            },
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isAgreed,
                onCheckedChange = { isAgreed = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.Gray,
                    uncheckedColor = Color.Gray
                )
            )
            Text(
                text = "Принимаете политику по обработке персональных данных в соответствии с требованиями ФЗ от 27.07.2006. № 152-ФЗ «О персональных данных»",
                fontSize = 10.sp,
                color = TextGrey,
                lineHeight = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        
        Text(
            text = "Уже есть аккаунт? Войти",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(top = 16.dp)
                .clickable { onLoginClick() }
        )
    }
}
