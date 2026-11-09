package com.rozkaam.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import java.util.Locale

/**
 * Utility for external map navigation using HTTPS browser directions.
 *
 * SPEC.md & Task Details Requirements:
 * 1. Constructs HTTPS Google Maps directions URLs: https://www.google.com/maps/dir/?api=1&destination=LATITUDE,LONGITUDE
 * 2. Explicitly targets Chrome (Custom Tabs / Intent) to avoid the outdated Google Maps Android app.
 * 3. Provides graceful fallback to alternative installed web browsers (explicitly excluding the Google Maps app).
 * 4. Validates coordinates and shows user-friendly toast on invalid or missing data.
 */
object NavigationUtils {

    const val CHROME_PACKAGE = "com.android.chrome"
    const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    const val OLD_BURGUNDY_COLOR = 0xFF43302E.toInt()

    /**
     * Validates that latitude and longitude are valid geographical numbers.
     * Excludes NaN, Infinite, out-of-range, and Null Island (0.0, 0.0) coordinates.
     */
    fun areValidCoordinates(lat: Double?, lng: Double?): Boolean {
        if (lat == null || lng == null) return false
        if (lat.isNaN() || lng.isNaN() || lat.isInfinite() || lng.isInfinite()) return false
        if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return false
        if (lat == 0.0 && lng == 0.0) return false
        return true
    }

    /**
     * Builds the Google Maps directions HTTPS URL using destination coordinates.
     * Uses Locale.US to ensure dot decimal separators regardless of device locale.
     */
    fun buildGoogleMapsDirectionsUrl(lat: Double, lng: Double): String {
        return String.format(Locale.US, "https://www.google.com/maps/dir/?api=1&destination=%f,%f", lat, lng)
    }

    /**
     * Opens task destination directions in Chrome or an alternative web browser.
     * Explicitly prevents launching the outdated Google Maps application in the emulator.
     */
    fun openDirectionsInBrowser(
        context: Context,
        lat: Double?,
        lng: Double?,
        onInvalidCoordinates: (() -> Unit)? = null
    ) {
        if (!areValidCoordinates(lat, lng)) {
            Toast.makeText(
                context,
                "Cannot open directions: Invalid or missing task location coordinates.",
                Toast.LENGTH_SHORT
            ).show()
            onInvalidCoordinates?.invoke()
            return
        }

        val url = buildGoogleMapsDirectionsUrl(lat!!, lng!!)
        val uri = Uri.parse(url)

        // 1. Primary path: Launch in Chrome using Chrome Custom Tabs with ROZKAAM Old Burgundy toolbar
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(OLD_BURGUNDY_COLOR)
                        .build()
                )
                .build().apply {
                    intent.setPackage(CHROME_PACKAGE)
                    if (context !is Activity) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            customTabsIntent.launchUrl(context, uri)
            return
        } catch (_: Exception) {
            // Chrome Custom Tabs failed or Chrome not default for custom tabs
        }

        // 2. Secondary path: Direct Chrome browser Intent
        try {
            val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(CHROME_PACKAGE)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chromeIntent)
            return
        } catch (_: Exception) {
            // Chrome not installed
        }

        // 3. Tertiary path: Graceful fallback to any other installed web browser (strictly excluding Google Maps app)
        try {
            val browsableIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            val resolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    browsableIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(browsableIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            val alternativeBrowser = resolveList.firstOrNull {
                val pkg = it.activityInfo.packageName
                pkg != GOOGLE_MAPS_PACKAGE && !pkg.contains("com.google.android.apps.maps")
            }

            if (alternativeBrowser != null) {
                val altIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(alternativeBrowser.activityInfo.packageName)
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(altIntent)
                return
            }
        } catch (_: Exception) {
            // Browser query failed
        }

        // 4. Final attempt: Generic Custom Tabs without package
        try {
            val fallbackCustomTabs = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build().apply {
                    if (context !is Activity) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            fallbackCustomTabs.launchUrl(context, uri)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "No supported web browser found to open map directions.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

