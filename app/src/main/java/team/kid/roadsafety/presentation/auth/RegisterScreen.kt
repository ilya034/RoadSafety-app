package team.kid.roadsafety.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import team.kid.roadsafety.data.dto.UserResponseDto
import team.kid.roadsafety.domain.aggregates.user.UserRole
import team.kid.roadsafety.presentation.theme.RolePurple
import team.kid.roadsafety.presentation.theme.RoleRed
import team.kid.roadsafety.presentation.theme.TextGrey

@Composable
fun RegisterScreen(
    onLoginClick: () -> Unit,
    onRegisterSuccess: (UserResponseDto?) -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var isAgreed by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val isFormValid = state.login.isNotBlank() && 
                      state.password.length >= 8 && 
                      isAgreed

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
            modifier = Modifier.padding(bottom = 20.dp)
        )

        AuthTextField(
            value = state.login,
            onValueChange = viewModel::onLoginChanged,
            placeholder = "Логин/телефон/почта",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        AuthTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChanged,
            placeholder = "Пароль (мин. 8 симв.)",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Роль:",
                color = Color.Gray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(15.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            RoleButton(
                text = "Родитель",
                isSelected = state.selectedRole == UserRole.PARENT,
                color = RoleRed,
                onClick = { viewModel.onRoleChanged(UserRole.PARENT) }
            )

            RoleButton(
                text = "Ребенок",
                isSelected = state.selectedRole == UserRole.CHILD,
                color = RolePurple,
                onClick = { viewModel.onRoleChanged(UserRole.CHILD) }
            )
        }

        AuthTextField(
            value = state.firstName,
            onValueChange = viewModel::onFirstNameChanged,
            placeholder = "Имя",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        AuthTextField(
            value = state.lastName,
            onValueChange = viewModel::onLastNameChanged,
            placeholder = "Фамилия",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        AuthButton(
            text = "Зарегистрироваться",
            isLoading = state.isLoading,
            enabled = isFormValid,
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

@Composable
fun RoleButton(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) color else color.copy(alpha = 0.3f),
                RoundedCornerShape(15.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
