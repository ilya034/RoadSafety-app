package team.kid.roadsafety.presentation.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import team.kid.roadsafety.domain.aggregates.family.FamilyRepository
import team.kid.roadsafety.domain.aggregates.map.MapCity
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
        loadCities()
    }

    fun onFamilyNameChanged(name: String) {
        _uiState.update { it.copy(familyName = name) }
    }

    fun onInviteCodeChanged(code: String) {
        _uiState.update { it.copy(inviteCode = code) }
    }

    fun onCityQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                cityQuery = query,
                selectedCity = state.cities.firstOrNull {
                    it.name.equals(query, ignoreCase = true) || it.cityId.equals(query, ignoreCase = true)
                }
            )
        }
    }

    fun onCitySelected(city: MapCity) {
        _uiState.update {
            it.copy(
                selectedCity = city,
                cityQuery = city.name
            )
        }
    }

    private fun loadCities() {
        viewModelScope.launch {
            val selectedCityId = familyRepository.getSelectedCityId()
            familyRepository.getSupportedCities()
                .fold(
                    onSuccess = { cities ->
                        val loadedCities = cities.ifEmpty { listOf(FallbackCity) }
                        val selectedCity = loadedCities.firstOrNull { it.cityId == selectedCityId }
                            ?: _uiState.value.selectedCity?.let { current ->
                                loadedCities.firstOrNull { it.cityId == current.cityId }
                            }
                            ?: loadedCities.first()

                        _uiState.update {
                            it.copy(
                                cities = loadedCities,
                                selectedCity = selectedCity,
                                cityQuery = selectedCity.name
                            )
                        }
                    },
                    onFailure = {
                        _uiState.update { state ->
                            state.copy(
                                cities = listOf(FallbackCity),
                                selectedCity = state.selectedCity ?: FallbackCity,
                                cityQuery = state.selectedCity?.name ?: FallbackCity.name
                            )
                        }
                    }
                )
        }
    }

    fun createFamily(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val selectedCity = _uiState.value.selectedCity
            if (selectedCity == null) {
                _uiState.update { it.copy(error = "Выберите город") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = familyRepository.createFamily(_uiState.value.familyName, selectedCity.cityId)
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
    val cities: List<MapCity> = listOf(FallbackCity),
    val selectedCity: MapCity? = FallbackCity,
    val cityQuery: String = FallbackCity.name,
    val isLoading: Boolean = false,
    val error: String? = null
)

fun filterCities(cities: List<MapCity>, query: String): List<MapCity> {
    return if (query.isBlank()) {
        cities
    } else {
        cities.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.cityId.contains(query, ignoreCase = true)
        }
    }
}

private val FallbackCity = MapCity(cityId = "ekb", name = "Екатеринбург")
