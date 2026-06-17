package team.kid.roadsafety.presentation.family

import org.junit.Assert.assertEquals
import org.junit.Test
import team.kid.roadsafety.domain.aggregates.map.MapCity

class FamilyOnboardingUiStateTest {
    @Test
    fun filterCitiesMatchesNameAndCityId() {
        val cities = listOf(
            MapCity(cityId = "ekb", name = "Екатеринбург"),
            MapCity(cityId = "salekhard", name = "Салехард")
        )

        assertEquals(listOf(cities[0]), filterCities(cities, "екат"))
        assertEquals(listOf(cities[1]), filterCities(cities, "sale"))
        assertEquals(cities, filterCities(cities, ""))
    }
}
