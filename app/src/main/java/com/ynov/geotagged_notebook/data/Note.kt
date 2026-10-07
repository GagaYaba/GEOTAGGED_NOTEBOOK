package com.ynov.geotagged_notebook.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false
) {
    val formattedCreatedDate: String
        get() = formatDate(createdAt)

    val formattedUpdatedDate: String
        get() = formatDate(updatedAt)

    val displayLocation: String
        get() {
            if (!locationName.isNull_or_blank_or_coords()) {
                return locationName!!
            }
            if (latitude != null && longitude != null) {
                return String.format(Locale.FRANCE, "%.5f, %.5f", latitude, longitude)
            }
            return ""
        }

    private fun String?.isNull_or_blank_or_coords(): Boolean {
        return this.isNullOrBlank() || this == "Position inconnue"
    }

    private fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd MMMM yyyy 'à' HH:mm", Locale.FRANCE)
        return formatter.format(Date(timestamp))
    }
}
