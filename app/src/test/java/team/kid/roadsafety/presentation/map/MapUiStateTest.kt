package team.kid.roadsafety.presentation.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import team.kid.roadsafety.domain.aggregates.map.GeoPoint
import team.kid.roadsafety.domain.aggregates.map.MapAreaColor

class MapUiStateTest {
    @Test
    fun customPolygonSaveRequiresThreeUniquePointsAndEditableMap() {
        val editableState = MapUiState(
            activeMapCityId = "ekb",
            familyCityId = "ekb",
            familyId = "family",
            isParent = true
        )

        assertFalse(editableState.copy(draftPoints = emptyList()).canSaveCustomArea)
        assertFalse(
            editableState.copy(
                draftPoints = listOf(
                    GeoPoint(56.0, 60.0),
                    GeoPoint(57.0, 61.0),
                    GeoPoint(56.0, 60.0)
                )
            ).canSaveCustomArea
        )
        assertTrue(
            editableState.copy(
                draftPoints = listOf(
                    GeoPoint(56.0, 60.0),
                    GeoPoint(57.0, 61.0),
                    GeoPoint(58.0, 62.0)
                )
            ).canSaveCustomArea
        )
        assertFalse(
            editableState.copy(
                activeMapCityId = "other",
                draftPoints = listOf(
                    GeoPoint(56.0, 60.0),
                    GeoPoint(57.0, 61.0),
                    GeoPoint(58.0, 62.0)
                )
            ).canSaveCustomArea
        )
        assertFalse(
            editableState.copy(
                isOnline = false,
                draftPoints = listOf(
                    GeoPoint(56.0, 60.0),
                    GeoPoint(57.0, 61.0),
                    GeoPoint(58.0, 62.0)
                )
            ).canSaveCustomArea
        )
    }

    @Test
    fun visibleChildrenFollowSelectedZoneTarget() {
        val firstChild = ChildMapLocation(
            childId = "child-1",
            displayName = "First",
            point = GeoPoint(56.0, 60.0),
            currentRisk = "Green",
            lastUpdatedAt = "now"
        )
        val secondChild = ChildMapLocation(
            childId = "child-2",
            displayName = "Second",
            point = GeoPoint(57.0, 61.0),
            currentRisk = "Red",
            lastUpdatedAt = "now"
        )
        val state = MapUiState(
            childLocations = listOf(firstChild, secondChild)
        )

        assertEquals(listOf(firstChild, secondChild), state.visibleChildLocations)
        assertEquals(
            listOf(secondChild),
            state.copy(selectedZoneTarget = ZoneTarget.Child("child-2", "Second")).visibleChildLocations
        )
    }

    @Test
    fun visibleChildrenUseLatestLocationPerChild() {
        val oldLocation = ChildMapLocation(
            childId = "child-1",
            displayName = "First",
            point = GeoPoint(56.0, 60.0),
            currentRisk = "Green",
            lastUpdatedAt = "2026-06-17T10:00:00Z"
        )
        val newLocation = oldLocation.copy(
            point = GeoPoint(57.0, 61.0),
            lastUpdatedAt = "2026-06-17T10:01:00Z"
        )
        val state = MapUiState(
            childLocations = listOf(oldLocation, newLocation)
        )

        assertEquals(listOf(newLocation), state.visibleChildLocations)
    }

    @Test
    fun changingViewedCityDisablesEditingUntilFamilyCityIsActiveAgain() {
        val state = MapUiState(
            activeMapCityId = "salekhard",
            familyCityId = "ekb",
            familyId = "family",
            isParent = true
        )

        assertFalse(state.canEditMap)
        assertTrue(state.copy(activeMapCityId = "ekb").canEditMap)
    }

    @Test
    fun activeCityCanChangeWithoutGlobalLoadingState() {
        val state = MapUiState(
            activeMapCityId = "ekb",
            familyCityId = "ekb",
            isLoading = false
        )

        val viewedCityState = state.copy(activeMapCityId = "salekhard")

        assertEquals("salekhard", viewedCityState.activeMapCityId)
        assertFalse(viewedCityState.isLoading)
    }

    @Test
    fun eraseModeIsAvailableOnlyWhenMapCanBeEdited() {
        val editableState = MapUiState(
            activeMapCityId = "ekb",
            familyCityId = "ekb",
            familyId = "family",
            isParent = true
        )

        assertTrue(editableState.canEraseAreas)
        assertFalse(editableState.copy(activeMapCityId = "salekhard").canEraseAreas)
        assertFalse(editableState.copy(familyId = null).canEraseAreas)
        assertFalse(editableState.copy(isParent = false).canEraseAreas)
    }

    @Test
    fun colorSelectionAndCustomDraftStateClearEraseMode() {
        val eraseState = MapUiState(
            activeMapCityId = "ekb",
            familyCityId = "ekb",
            familyId = "family",
            isParent = true,
            isEraseMode = true,
            activePaintColor = null,
            isCreatingCustomArea = false
        )

        val colorSelectionState = eraseState.copy(
            activePaintColor = MapAreaColor.RED,
            isEraseMode = false,
            isCreatingCustomArea = false,
            draftPoints = emptyList()
        )
        assertEquals(MapAreaColor.RED, colorSelectionState.activePaintColor)
        assertFalse(colorSelectionState.isEraseMode)

        val draftState = eraseState.copy(
            isCreatingCustomArea = true,
            activePaintColor = null,
            isEraseMode = false,
            draftPoints = emptyList()
        )
        assertTrue(draftState.isCreatingCustomArea)
        assertNull(draftState.activePaintColor)
        assertFalse(draftState.isEraseMode)
    }

    @Test
    fun currentRequestChildIdResolvesCorrectlyDependingOnRole() {
        val childUuid = java.util.UUID.randomUUID()
        val childId = team.kid.roadsafety.domain.UserId(childUuid)

        // For a parent, it should return selectedZoneTarget.childUserId()
        val parentStateAll = MapUiState(
            isParent = true,
            selectedZoneTarget = ZoneTarget.AllFamily,
            currentChildId = childId
        )
        assertNull(parentStateAll.currentRequestChildId)

        val parentStateSpecific = MapUiState(
            isParent = true,
            selectedZoneTarget = ZoneTarget.Child(childUuid.toString(), "Child label"),
            currentChildId = childId
        )
        assertEquals(childId, parentStateSpecific.currentRequestChildId)

        // For a child, it should always return currentChildId
        val childStateAll = MapUiState(
            isParent = false,
            selectedZoneTarget = ZoneTarget.AllFamily,
            currentChildId = childId
        )
        assertEquals(childId, childStateAll.currentRequestChildId)
    }
}
