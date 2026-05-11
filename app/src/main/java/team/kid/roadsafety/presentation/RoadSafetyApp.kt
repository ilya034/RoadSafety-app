package team.kid.roadsafety.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import team.kid.roadsafety.R
import team.kid.roadsafety.presentation.auth.AuthNavigation
import team.kid.roadsafety.presentation.map.MapColoringScreen
import team.kid.roadsafety.presentation.theme.RoadSafetyTheme

@Composable
@Preview
fun RoadSafetyApp() {
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }

    if (!isAuthenticated) {
        AuthNavigation(onAuthSuccess = { isAuthenticated = true })
    } else {
        MainScreen()
    }
}

@Composable
fun MainScreen() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }

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
fun MapScreen(modifier: Modifier = Modifier) {
    val cameraState = rememberCameraState()
    MaplibreMap(
        modifier = modifier.fillMaxSize(),
        baseStyle = BaseStyle.Uri("https://demotiles.maplibre.org/style.json"),
        cameraState = cameraState
    )
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