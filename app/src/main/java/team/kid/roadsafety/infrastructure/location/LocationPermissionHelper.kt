package team.kid.roadsafety.infrastructure.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat

@Composable
fun LocationPermissionHelper(onPermissionsGranted: () -> Unit) {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = result[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (hasForegroundLocationPermission(fineGranted, coarseGranted)) {
            onPermissionsGranted()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissions.toTypedArray())
    }
}

fun hasForegroundLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return hasForegroundLocationPermission(fineGranted, coarseGranted)
}

fun hasForegroundLocationPermission(fineGranted: Boolean, coarseGranted: Boolean): Boolean {
    return fineGranted || coarseGranted
}
