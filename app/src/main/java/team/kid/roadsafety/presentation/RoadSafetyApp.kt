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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.viewmodel.compose.viewModel
import team.kid.roadsafety.R
import team.kid.roadsafety.data.local.SessionManager
import team.kid.roadsafety.presentation.theme.RoadSafetyTheme

@Composable
fun RoadSafetyApp() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val viewModel: MainViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(sessionManager) as T
        }
    })

    val uiState by viewModel.uiState.collectAsState()

    RoadSafetyTheme {
        when (uiState) {
            MainUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            MainUiState.NotAuthenticated -> {
                AuthScreen(sessionManager)
            }
            MainUiState.AuthenticatedNoFamily -> {
                FamilySelectionScreen(sessionManager)
            }
            MainUiState.AuthenticatedWithFamily -> {
                MainAppContent(sessionManager)
            }
        }
    }
}

@Composable
fun MainAppContent(sessionManager: SessionManager) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.PROFILE) }

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
                AppDestinations.PROFILE -> ProfileScreen(sessionManager, Modifier.padding(innerPadding))
                else -> Greeting(
                    name = currentDestination.label,
                    modifier = Modifier.padding(innerPadding)
                )
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RoadSafetyTheme {
        Greeting("Android")
    }
}