package team.kid.roadsafety.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import team.kid.roadsafety.R
import team.kid.roadsafety.infrastructure.location.LocationPermissionHelper
import team.kid.roadsafety.infrastructure.location.WarningAlertEvents
import team.kid.roadsafety.presentation.auth.AuthNavigation
import team.kid.roadsafety.presentation.family.FamilyOnboardingScreen
import team.kid.roadsafety.presentation.map.MapColoringScreen
import team.kid.roadsafety.presentation.notifications.NotificationsScreen
import team.kid.roadsafety.presentation.profile.ProfileScreen

@Composable
fun RoadSafetyApp(viewModel: MainViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsState()
    val warningAlert by WarningAlertEvents.current.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = authState) {
            is AuthState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AuthState.Authenticated -> {
                val isChild by viewModel.isChild.collectAsState()
                val context = LocalContext.current

                if (isChild) {
                    LocationPermissionHelper(
                        onPermissionsGranted = {
                            viewModel.startLocationService(context)
                        }
                    )
                }

                val sessionKey = "${state.user.id}:${state.user.familyId}:${state.user.familyRole}"
                key(sessionKey) {
                    MainScreen(
                        sessionKey = sessionKey,
                        onLogout = { viewModel.logout(context) }
                    )
                }
            }
            is AuthState.AuthenticatedButNoFamily -> {
                FamilyOnboardingScreen(onSuccess = viewModel::onAuthSuccess)
            }
            is AuthState.Unauthenticated -> {
                AuthNavigation(onAuthSuccess = viewModel::onAuthSuccess)
            }
        }

        warningAlert?.let { alert ->
            SafetyWarningBanner(
                title = alert.title,
                message = if (alert.offline) "${alert.message} (offline)" else alert.message,
                onDismiss = WarningAlertEvents::dismiss,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun SafetyWarningBanner(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        onClick = onDismiss
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MainScreen(
    sessionKey: String = "",
    onLogout: () -> Unit
) {
    var currentDestination by remember { 
        mutableStateOf(AppDestinations.MAP)
    }

    NavigationSuiteScaffold(
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = Color.White,
            navigationBarContentColor = Color.Black
        ),
        containerColor = Color.White,
        contentColor = Color.Black,
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = if (currentDestination == AppDestinations.MAP) {
                        Modifier.fillMaxSize().padding(innerPadding)
                    } else {
                        Modifier.size(0.dp)
                    }
                ) {
                    MapColoringScreen(
                        sessionKey = sessionKey,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                when (currentDestination) {
                    AppDestinations.PROFILE -> {
                        ProfileScreen(
                            onLogout = onLogout,
                            modifier = Modifier.fillMaxSize().padding(innerPadding)
                        )
                    }
                    AppDestinations.NOTIFICATIONS -> {
                        NotificationsScreen(
                            modifier = Modifier.fillMaxSize().padding(innerPadding)
                        )
                    }
                    AppDestinations.MAP -> {
                        // Handled separately above
                    }
                    else -> {
                        Greeting(
                            name = currentDestination.label,
                            modifier = Modifier.fillMaxSize().padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    MAP("Map", R.drawable.ic_map),
    CHATS("Chats", R.drawable.ic_chats),
    NOTIFICATIONS("Notifications", R.drawable.ic_notifications),
    PROFILE("Profile", R.drawable.ic_profile),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
