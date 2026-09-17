package com.mawaqit.app.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GPS location via FusedLocationProvider (API_REFERENCE.md).
 * Uses getCurrentLocation() for a FRESH fix — lastLocation can be hours old
 * or belong to another app's session, which would silently put prayers an
 * hour off. Falls back to lastLocation only if the fresh fix fails.
 *
 * Callers MUST have ACCESS_FINE_LOCATION granted first (PERMISSIONS.md);
 * a SecurityException is caught and surfaced as null.
 */
@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @SuppressLint("MissingPermission") // callers check permission; we catch SecurityException anyway
    suspend fun getCurrentLocation(): android.location.Location? {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        return try {
            try {
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()
            } catch (e: Exception) {
                // fresh fix failed (timeout, settings) → try the last-known position
                fusedClient.lastLocation.await()
            }
        } catch (e: SecurityException) {
            null // permission not granted
        } catch (e: Exception) {
            null // play services unavailable etc.
        }
    }
}
