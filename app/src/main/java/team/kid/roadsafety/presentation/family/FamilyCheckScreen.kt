package team.kid.roadsafety.presentation.family

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import team.kid.roadsafety.presentation.MainScreen

@Composable
fun FamilyCheckScreen(
    onLogout: () -> Unit,
    viewModel: FamilyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var isChecking by remember { mutableStateOf(true) }

    // Logic to check if user already belongs to a family (should be in ViewModel/Repository)
    // For now, if currentFamily is null and we are not joined, show Joining screen
    
    if (state.currentFamily != null || state.isJoined) {
        MainScreen(onLogout = onLogout)
    } else {
        FamilyJoiningScreen(
            onSuccess = { /* ViewModel should update state to trigger re-composition */ }
        )
    }
}
