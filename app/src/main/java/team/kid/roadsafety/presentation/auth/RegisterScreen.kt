package team.kid.roadsafety.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import team.kid.roadsafety.domain.enums.FamilyRole
import team.kid.roadsafety.presentation.theme.RolePurple
import team.kid.roadsafety.presentation.theme.RoleRed
import team.kid.roadsafety.presentation.theme.TextGrey

@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
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
                isSelected = state.role == FamilyRole.PARENT,
                color = RoleRed,
                onClick = { viewModel.onRoleChanged(FamilyRole.PARENT) }
            )

            RoleButton(
                text = "Ребенок",
                isSelected = state.role == FamilyRole.CHILD,
                color = RolePurple,
                onClick = { viewModel.onRoleChanged(FamilyRole.CHILD) }
            )
        }

        AuthButton(
            text = "Зарегистрироваться",
            onClick = {
                viewModel.onRegisterClicked()
                onRegisterSuccess()
            },
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = state.isAgreed,
                onCheckedChange = viewModel::onAgreementChanged,
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
                .clickable { onBackToLogin() }
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
