package team.kid.roadsafety.presentation.family

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
