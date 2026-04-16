package team.kid.roadsafety.presentation.auth

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import team.kid.roadsafety.domain.enums.FamilyRole

data class RegisterState(
    val login: String = "",
    val password: String = "",
    val role: FamilyRole = FamilyRole.PARENT,
    val isAgreed: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class RegisterViewModel : ViewModel() {
    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    fun onLoginChanged(login: String) {
        _state.update { it.copy(login = login) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun onRoleChanged(role: FamilyRole) {
        _state.update { it.copy(role = role) }
    }

    fun onAgreementChanged(isAgreed: Boolean) {
        _state.update { it.copy(isAgreed = isAgreed) }
    }

    fun onRegisterClicked() {
        // Implement registration logic
    }
}
