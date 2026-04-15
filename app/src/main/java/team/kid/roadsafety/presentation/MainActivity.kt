package team.kid.roadsafety.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import team.kid.roadsafety.presentation.theme.RoadSafetyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoadSafetyTheme {
                RoadSafetyApp()
            }
        }
    }
}