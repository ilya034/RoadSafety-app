package team.kid.roadsafety.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.kid.roadsafety.data.dto.UserResponseDto
import team.kid.roadsafety.domain.FamilyId
import team.kid.roadsafety.domain.aggregates.family.FamilyMemberEntity
import team.kid.roadsafety.domain.aggregates.family.FamilyRepository
import team.kid.roadsafety.domain.aggregates.user.AuthRepository
import team.kid.roadsafety.domain.aggregates.user.UserRole
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyRepository: FamilyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userResult = authRepository.getCurrentUser()
            userResult.onSuccess { user ->
                _uiState.update { it.copy(user = user) }
                if (user.familyId != null) {
                    loadFamily(FamilyId(UUID.fromString(user.familyId)))
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun generateInviteCode(role: UserRole) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            familyRepository.generateInviteCode(role)
                .onSuccess { code ->
                    _uiState.update { it.copy(isLoading = false, inviteCode = code) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun clearInviteCode() {
        _uiState.update { it.copy(inviteCode = null) }
    }

    private suspend fun loadFamily(familyId: FamilyId) {
        val membersResult = familyRepository.getFamilyMembers(familyId)
        membersResult.onSuccess { members ->
            _uiState.update { it.copy(members = members, isLoading = false) }
        }.onFailure { error ->
            _uiState.update { it.copy(isLoading = false, error = error.message) }
        }
    }
}

data class ProfileUiState(
    val user: UserResponseDto? = null,
    val members: List<FamilyMemberEntity> = emptyList(),
    val inviteCode: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
