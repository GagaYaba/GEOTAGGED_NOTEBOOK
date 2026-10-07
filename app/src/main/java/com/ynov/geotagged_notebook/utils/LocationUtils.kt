package com.ynov.geotagged_notebook.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object LocationUtils {

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    if (continuation.isActive) continuation.resume(location)
                } else {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLocation ->
                            if (continuation.isActive) continuation.resume(lastLocation)
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) continuation.resume(null)
                        }
                }
            }.addOnFailureListener {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastLocation ->
                        if (continuation.isActive) continuation.resume(lastLocation)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.FRANCE)
            val fallback = "Latitude : ${String.format(Locale.FRANCE, "%.4f", latitude)}, " +
                    "longitude : ${String.format(Locale.FRANCE, "%.4f", longitude)}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var addressResult = fallback
                suspendCancellableCoroutine<Unit> { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            addressResult = formatFrenchAddress(addresses[0], fallback)
                        }
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
                return@withContext addressResult
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    return@withContext formatFrenchAddress(addresses[0], fallback)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext String.format(
            Locale.FRANCE,
            "Latitude : %.4f, longitude : %.4f",
            latitude,
            longitude
        )
    }

    private fun formatFrenchAddress(address: Address, fallback: String): String {
        val street = listOfNotNull(address.subThoroughfare, address.thoroughfare)
            .joinToString(" ")
            .ifBlank { null }
        val city = address.locality ?: address.subAdminArea ?: address.adminArea
        val cityLine = listOfNotNull(address.postalCode, city)
            .joinToString(" ")
            .ifBlank { null }

        return listOfNotNull(street, cityLine, address.countryName)
            .distinct()
            .joinToString(", ")
            .ifBlank { fallback }
    }
}
