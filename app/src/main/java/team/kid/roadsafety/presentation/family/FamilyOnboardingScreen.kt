package team.kid.roadsafety.presentation.family

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import team.kid.roadsafety.domain.aggregates.map.MapCity
import team.kid.roadsafety.domain.aggregates.user.UserRole
import team.kid.roadsafety.presentation.auth.AuthButton
import team.kid.roadsafety.presentation.auth.AuthTextField

@Composable
fun FamilyOnboardingScreen(
    onSuccess: () -> Unit,
    viewModel: FamilyOnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var isJoining by remember { mutableStateOf(false) }

    if (isJoining) {
        JoinFamilyContent(
            inviteCode = state.inviteCode,
            onInviteCodeChange = viewModel::onInviteCodeChanged,
            onJoinClick = { viewModel.joinFamily(onSuccess) },
            onBackClick = { isJoining = false },
            isLoading = state.isLoading,
            error = state.error
        )
    } else {
        CreateFamilyContent(
            familyName = state.familyName,
            onFamilyNameChange = viewModel::onFamilyNameChanged,
            cities = state.cities,
            cityQuery = state.cityQuery,
            selectedCity = state.selectedCity,
            onCityQueryChange = viewModel::onCityQueryChanged,
            onCitySelected = viewModel::onCitySelected,
            onCreateClick = { viewModel.createFamily(onSuccess) },
            onJoinClick = { isJoining = true },
            userRole = state.userRole,
            isLoading = state.isLoading,
            error = state.error
        )
    }
}

@Composable
fun CreateFamilyContent(
    familyName: String,
    onFamilyNameChange: (String) -> Unit,
    cities: List<MapCity>,
    cityQuery: String,
    selectedCity: MapCity?,
    onCityQueryChange: (String) -> Unit,
    onCitySelected: (MapCity) -> Unit,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    userRole: UserRole,
    isLoading: Boolean,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "РЎРµРјСЊСЏ",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (userRole == UserRole.PARENT) {
            AuthTextField(
                value = familyName,
                onValueChange = onFamilyNameChange,
                placeholder = "РќР°Р·РІР°РЅРёРµ СЃРµРјСЊРё",
                modifier = Modifier.padding(bottom = 24.dp)
            )

            CitySearchDropdown(
                cities = cities,
                query = cityQuery,
                selectedCity = selectedCity,
                onQueryChange = onCityQueryChange,
                onCitySelected = onCitySelected,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            AuthButton(
                text = "РЎРѕР·РґР°С‚СЊ СЃРµРјСЊСЋ",
                isLoading = isLoading,
                enabled = familyName.isNotBlank() && selectedCity != null,
                onClick = onCreateClick,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        Text(
            text = "Р’РѕР№С‚Рё РІ СЃРµРјСЊСЋ",
            color = Color.Gray,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(top = 16.dp)
                .clickable { onJoinClick() }
        )

        if (error != null) {
            Text(
                text = error,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun CitySearchDropdown(
    cities: List<MapCity>,
    query: String,
    selectedCity: MapCity?,
    onQueryChange: (String) -> Unit,
    onCitySelected: (MapCity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredCities = remember(cities, query) {
        if (query.isBlank()) {
            cities
        } else {
            cities.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.cityId.contains(query, ignoreCase = true)
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        AuthTextField(
            value = query,
            onValueChange = {
                expanded = true
                onQueryChange(it)
            },
            placeholder = "Город",
            modifier = Modifier.clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded && filteredCities.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            filteredCities.forEach { city ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = city.name,
                            color = if (city == selectedCity) Color.Gray else Color.Unspecified
                        )
                    },
                    onClick = {
                        onCitySelected(city)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun JoinFamilyContent(
    inviteCode: String,
    onInviteCodeChange: (String) -> Unit,
    onJoinClick: () -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Р’РІРµРґРёС‚Рµ РєРѕРґ",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        AuthTextField(
            value = inviteCode,
            onValueChange = onInviteCodeChange,
            placeholder = "РљРѕРґ РїСЂРёРіР»Р°С€РµРЅРёСЏ",
            modifier = Modifier.padding(bottom = 24.dp)
        )

        AuthButton(
            text = "OK",
            isLoading = isLoading,
            enabled = inviteCode.isNotBlank(),
            onClick = onJoinClick,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Text(
            text = "РќР°Р·Р°Рґ",
            color = Color.Gray,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(top = 16.dp)
                .clickable { onBackClick() }
        )

        if (error != null) {
            Text(
                text = error,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
