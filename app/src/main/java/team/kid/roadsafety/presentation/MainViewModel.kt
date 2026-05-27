package team.kid.roadsafety.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import team.kid.roadsafety.data.local.SessionManager

class MainViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        sessionManager.accessToken,
        sessionManager.familyId
    ) { token, familyId ->
        when {
            token == null -> MainUiState.NotAuthenticated
            familyId == null -> MainUiState.AuthenticatedNoFamily
            else -> MainUiState.AuthenticatedWithFamily
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState.Loading
    )
}

sealed interface MainUiState {
    data object Loading : MainUiState
    data object NotAuthenticated : MainUiState
    data object AuthenticatedNoFamily : MainUiState
    data object AuthenticatedWithFamily : MainUiState
}
