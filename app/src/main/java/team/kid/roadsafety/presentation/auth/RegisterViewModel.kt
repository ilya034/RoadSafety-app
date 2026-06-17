package team.kid.roadsafety.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.kid.roadsafety.data.dto.UserResponseDto
import team.kid.roadsafety.domain.aggregates.family.FamilyRepository
import team.kid.roadsafety.domain.aggregates.user.AuthRepository
import team.kid.roadsafety.domain.aggregates.user.UserRole
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyRepository: FamilyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onLoginChanged(login: String) {
        _uiState.update { it.copy(login = login) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onFirstNameChanged(firstName: String) {
        _uiState.update { it.copy(firstName = firstName) }
    }

    fun onLastNameChanged(lastName: String) {
        _uiState.update { it.copy(lastName = lastName) }
    }

    fun onBirthDateChanged(birthDate: LocalDate?) {
        _uiState.update { it.copy(birthDate = birthDate) }
    }

    fun onRoleChanged(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun register(onSuccess: (UserResponseDto?) -> Unit) {
        viewModelScope.launch {
            Log.d("RegisterViewModel", "Registration initiated for: ${_uiState.value.login}")
            _uiState.update { it.copy(isLoading = true, error = null) }
            val state = _uiState.value
            
            // Save the role before registration
            familyRepository.setSelectedRole(state.selectedRole)
            
            val result = authRepository.register(
                login = state.login,
                password = state.password
            )
            result.fold(
                onSuccess = { response ->
                    Log.d("RegisterViewModel", "Registration successful")
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(null)
                },
                onFailure = { error ->
                    Log.e("RegisterViewModel", "Registration failed", error)
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }
}

data class RegisterUiState(
    val login: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: LocalDate? = null,
    val selectedRole: UserRole = UserRole.PARENT,
    val isLoading: Boolean = false,
    val error: String? = null
)
