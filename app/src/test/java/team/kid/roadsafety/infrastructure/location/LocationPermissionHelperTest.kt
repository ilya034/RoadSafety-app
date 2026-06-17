package team.kid.roadsafety.infrastructure.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionHelperTest {
    @Test
    fun foregroundPermissionAllowsFineOrCoarseLocation() {
        assertTrue(hasForegroundLocationPermission(fineGranted = true, coarseGranted = false))
        assertTrue(hasForegroundLocationPermission(fineGranted = false, coarseGranted = true))
        assertFalse(hasForegroundLocationPermission(fineGranted = false, coarseGranted = false))
    }
}
