package team.kid.roadsafety.presentation.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.kid.roadsafety.domain.aggregates.family.FamilyEntity
import team.kid.roadsafety.domain.aggregates.family.FamilyRepository
import team.kid.roadsafety.domain.aggregates.user.UserRole
import javax.inject.Inject

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val familyRepository: FamilyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val role = familyRepository.getSelectedRole()
        _uiState.update { it.copy(userRole = role) }
    }

    fun onInviteCodeChanged(code: String) {
        _uiState.update { it.copy(inviteCode = code) }
    }

    fun onFamilyNameChanged(name: String) {
        _uiState.update { it.copy(newFamilyName = name) }
    }

    fun createFamily() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            familyRepository.createFamily(
                name = _uiState.value.newFamilyName,
                cityId = familyRepository.getSelectedCityId() ?: "ekb"
            )
                .fold(
                    onSuccess = { family ->
                        _uiState.update { it.copy(isLoading = false, currentFamily = family) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
        }
    }

    fun joinFamily() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val role = _uiState.value.userRole ?: UserRole.CHILD
            familyRepository.joinFamily(_uiState.value.inviteCode, role)
                .fold(
                    onSuccess = { member ->
                        _uiState.update { it.copy(isLoading = false, isJoined = true) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
        }
    }
}

data class FamilyUiState(
    val userRole: UserRole? = null,
    val inviteCode: String = "",
    val newFamilyName: String = "",
    val currentFamily: FamilyEntity? = null,
    val isJoined: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
