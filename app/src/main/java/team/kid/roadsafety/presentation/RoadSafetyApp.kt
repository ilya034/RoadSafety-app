package team.kid.roadsafety.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import team.kid.roadsafety.R
import team.kid.roadsafety.presentation.auth.AuthNavigation
import team.kid.roadsafety.presentation.map.MapColoringScreen
import team.kid.roadsafety.presentation.theme.RoadSafetyTheme

@Composable
fun RoadSafetyApp(viewModel: MainViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsState()

    when (authState) {
        is AuthState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AuthState.Authenticated -> {
            MainScreen(onLogout = viewModel::logout)
        }
        is AuthState.Unauthenticated -> {
            AuthNavigation(onAuthSuccess = viewModel::onAuthSuccess)
        }
    }
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    var currentDestination by remember { 
        mutableStateOf(AppDestinations.MAP)
    }

    NavigationSuiteScaffold(
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
            when (currentDestination) {
                AppDestinations.MAP -> MapColoringScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.PROFILE -> {
                    ProfileScreen(
                        onLogout = onLogout,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                else -> {
                    Greeting(
                        name = currentDestination.label,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Button(onClick = onLogout) {
            Text("Logout")
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
