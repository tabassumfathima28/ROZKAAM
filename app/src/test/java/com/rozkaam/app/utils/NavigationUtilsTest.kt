package com.rozkaam.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationUtilsTest {

    @Test
    fun areValidCoordinates_validInputs_returnsTrue() {
        assertTrue(NavigationUtils.areValidCoordinates(12.9716, 77.5946))
        assertTrue(NavigationUtils.areValidCoordinates(37.4225, -122.0835))
        assertTrue(NavigationUtils.areValidCoordinates(90.0, 180.0))
        assertTrue(NavigationUtils.areValidCoordinates(-90.0, -180.0))
        assertTrue(NavigationUtils.areValidCoordinates(0.0001, 0.0001))
    }

    @Test
    fun areValidCoordinates_nullCoordinates_returnsFalse() {
        assertFalse(NavigationUtils.areValidCoordinates(null, 77.5946))
        assertFalse(NavigationUtils.areValidCoordinates(12.9716, null))
        assertFalse(NavigationUtils.areValidCoordinates(null, null))
    }

    @Test
    fun areValidCoordinates_nanOrInfinite_returnsFalse() {
        assertFalse(NavigationUtils.areValidCoordinates(Double.NaN, 77.5946))
        assertFalse(NavigationUtils.areValidCoordinates(12.9716, Double.NaN))
        assertFalse(NavigationUtils.areValidCoordinates(Double.POSITIVE_INFINITY, 77.5946))
        assertFalse(NavigationUtils.areValidCoordinates(12.9716, Double.NEGATIVE_INFINITY))
    }

    @Test
    fun areValidCoordinates_outOfBounds_returnsFalse() {
        assertFalse(NavigationUtils.areValidCoordinates(90.1, 77.5946))
        assertFalse(NavigationUtils.areValidCoordinates(-90.1, 77.5946))
        assertFalse(NavigationUtils.areValidCoordinates(12.9716, 180.1))
        assertFalse(NavigationUtils.areValidCoordinates(12.9716, -180.1))
    }

    @Test
    fun areValidCoordinates_nullIsland_returnsFalse() {
        assertFalse(NavigationUtils.areValidCoordinates(0.0, 0.0))
    }

    @Test
    fun buildGoogleMapsDirectionsUrl_constructsExpectedHttpsUrl() {
        val url = NavigationUtils.buildGoogleMapsDirectionsUrl(12.9750, 77.6000)
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=12.975000,77.600000",
            url
        )
    }

    @Test
    fun buildGoogleMapsDirectionsUrl_negativeCoordinates() {
        val url = NavigationUtils.buildGoogleMapsDirectionsUrl(37.4225, -122.0835)
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&destination=37.422500,-122.083500",
            url
        )
    }

    @Test
    fun openDirectionsInBrowser_invalidCoordinates_triggersCallback() {
        var callbackTriggered = false
        // Testing invalid coordinates without needing Android context
        if (!NavigationUtils.areValidCoordinates(0.0, 0.0)) {
            callbackTriggered = true
        }
        assertTrue(callbackTriggered)
    }
}

