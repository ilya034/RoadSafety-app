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
import team.kid.roadsafety.domain.aggregates.map.MapCity
import team.kid.roadsafety.domain.aggregates.user.AuthRepository
import team.kid.roadsafety.domain.aggregates.user.UserRole
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyRepository: FamilyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Removed loadProfile() to avoid double loading with LaunchedEffect in Screen
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
        try {
            val citiesResult = familyRepository.getSupportedCities()
            val cities = citiesResult.getOrNull().orEmpty()
            _uiState.update { it.copy(supportedCities = cities) }

            coroutineScope {
                launch {
                    familyRepository.getFamily(familyId).onSuccess { family ->
                        val city = cities.firstOrNull { it.cityId == family.cityId }
                        val cityName = city?.name ?: family.cityId
                        _uiState.update { it.copy(cityName = cityName, familyId = familyId) }
                    }
                }

                launch {
                    familyRepository.getFamilyMembers(familyId).onSuccess { members ->
                        _uiState.update { it.copy(members = members) }
                    }.onFailure { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                }
            }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateFamilyCity(cityId: String) {
        val familyId = _uiState.value.familyId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            familyRepository.updateFamilyCity(familyId, cityId)
                .onSuccess {
                    val cityName = _uiState.value.supportedCities
                        .firstOrNull { it.cityId == cityId }?.name ?: cityId
                    _uiState.update { it.copy(isLoading = false, cityName = cityName) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}

data class ProfileUiState(
    val user: UserResponseDto? = null,
    val familyId: FamilyId? = null,
    val members: List<FamilyMemberEntity> = emptyList(),
    val cityName: String? = null,
    val supportedCities: List<MapCity> = emptyList(),
    val inviteCode: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
