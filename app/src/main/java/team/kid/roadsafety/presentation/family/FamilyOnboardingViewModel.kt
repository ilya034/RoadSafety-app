package team.kid.roadsafety.presentation.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.kid.roadsafety.domain.aggregates.family.FamilyRepository
import team.kid.roadsafety.domain.aggregates.user.UserRole
import javax.inject.Inject

@HiltViewModel
class FamilyOnboardingViewModel @Inject constructor(
    private val familyRepository: FamilyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyOnboardingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val role = familyRepository.getSelectedRole() ?: UserRole.CHILD
        _uiState.update { it.copy(userRole = role) }
    }

    fun onFamilyNameChanged(name: String) {
        _uiState.update { it.copy(familyName = name) }
    }

    fun onInviteCodeChanged(code: String) {
        _uiState.update { it.copy(inviteCode = code) }
    }

    fun createFamily(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = familyRepository.createFamily(_uiState.value.familyName)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    fun joinFamily(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = familyRepository.joinFamily(_uiState.value.inviteCode, _uiState.value.userRole)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }
}

data class FamilyOnboardingUiState(
    val familyName: String = "",
    val inviteCode: String = "",
    val userRole: UserRole = UserRole.CHILD,
    val isLoading: Boolean = false,
    val error: String? = null
)
