package team.kid.roadsafety.presentation.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import team.kid.roadsafety.domain.aggregates.user.UserRole
import team.kid.roadsafety.presentation.auth.AuthButton
import team.kid.roadsafety.presentation.auth.AuthTextField

@Composable
fun FamilyJoiningScreen(
    onSuccess: () -> Unit,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isJoined || state.currentFamily != null) {
        LaunchedEffect(Unit) {
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (state.userRole == UserRole.PARENT) "Создать семью" else "Присоединиться",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 30.dp)
        )

        if (state.userRole == UserRole.PARENT) {
            AuthTextField(
                value = state.newFamilyName,
                onValueChange = viewModel::onFamilyNameChanged,
                placeholder = "Название семьи",
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AuthButton(
                text = "Создать",
                isLoading = state.isLoading,
                enabled = state.newFamilyName.isNotBlank(),
                onClick = { viewModel.createFamily() },
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "ИЛИ",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        AuthTextField(
            value = state.inviteCode,
            onValueChange = viewModel::onInviteCodeChanged,
            placeholder = "6-значный код",
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AuthButton(
            text = "Присоединиться",
            isLoading = state.isLoading,
            enabled = state.inviteCode.length == 6,
            onClick = { viewModel.joinFamily() },
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = Color.Red,
                fontSize = 14.sp
            )
        }
    }
}
