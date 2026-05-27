package team.kid.roadsafety.presentation.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class AuthScreen(val route: String) {
    object Login : AuthScreen("login")
    object Register : AuthScreen("register")
}

@Composable
fun AuthNavigation(onAuthSuccess: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AuthScreen.Login.route) {
        composable(AuthScreen.Login.route) {
            LoginScreen(
                onRegisterClick = { navController.navigate(AuthScreen.Register.route) },
                onLoginSuccess = onAuthSuccess
            )
        }
        composable(AuthScreen.Register.route) {
            RegisterScreen(
                onLoginClick = { navController.popBackStack() },
                onRegisterSuccess = onAuthSuccess
            )
        }
    }
}
