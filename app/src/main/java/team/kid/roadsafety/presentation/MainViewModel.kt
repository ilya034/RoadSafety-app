package team.kid.roadsafety.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.kid.roadsafety.data.dto.UserResponseDto
import team.kid.roadsafety.domain.aggregates.user.AuthRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyRepository: team.kid.roadsafety.domain.aggregates.family.FamilyRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    init {
        Log.d("MainViewModel", "Initializing MainViewModel")
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            val tokens = authRepository.getTokens()
            Log.d("MainViewModel", "Checking auth, tokens found: ${tokens != null}")
            if (tokens != null) {
                fetchUserProfile()
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    private suspend fun fetchUserProfile() {
        Log.d("MainViewModel", "Fetching user profile")
        val result = authRepository.getCurrentUser()
        result.onSuccess { user ->
            Log.d("MainViewModel", "User profile fetched: $user")
            updateStateWithUser(user)
        }.onFailure { error ->
            Log.e("MainViewModel", "Failed to fetch user profile", error)
            // Handle 401 and refresh logic if needed, but for now just unauth
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun onAuthSuccess(user: UserResponseDto? = null) {
        Log.d("MainViewModel", "onAuthSuccess called with user: $user")
        viewModelScope.launch {
            if (user != null) {
                updateStateWithUser(user)
            } else {
                fetchUserProfile()
            }
        }
    }

    private fun updateStateWithUser(user: UserResponseDto) {
        val newState = if (user.familyId == null) {
            AuthState.AuthenticatedButNoFamily(user)
        } else {
            AuthState.Authenticated(user)
        }
        Log.d("MainViewModel", "Updating state to: $newState")
        _authState.update { newState }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            familyRepository.clearData()
            _authState.value = AuthState.Unauthenticated
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: UserResponseDto) : AuthState()
    data class AuthenticatedButNoFamily(val user: UserResponseDto) : AuthState()
    object Unauthenticated : AuthState()
}
