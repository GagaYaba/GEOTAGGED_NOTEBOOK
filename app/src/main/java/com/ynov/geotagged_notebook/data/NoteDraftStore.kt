package com.ynov.geotagged_notebook.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class NoteDraftStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "brouillons_notes",
        Context.MODE_PRIVATE
    )

    fun load(noteId: Long): Note? {
        val rawDraft = preferences.getString(keyFor(noteId), null) ?: return null
        return runCatching {
            val json = JSONObject(rawDraft)
            Note(
                id = json.optLong("id", noteId),
                title = json.optString("titre"),
                content = json.optString("contenu"),
                imageUri = json.nullableString("image"),
                createdAt = json.optLong("creeLe", System.currentTimeMillis()),
                updatedAt = json.optLong("modifieLe", System.currentTimeMillis()),
                latitude = json.nullableDouble("latitude"),
                longitude = json.nullableDouble("longitude"),
                locationName = json.nullableString("lieu"),
                tags = json.optJSONArray("tags").toStringList(),
                isFavorite = json.optBoolean("favori"),
                isArchived = json.optBoolean("archive")
            )
        }.getOrNull()
    }

    fun save(note: Note) {
        val json = JSONObject().apply {
            put("id", note.id)
            put("titre", note.title)
            put("contenu", note.content)
            put("image", note.imageUri ?: JSONObject.NULL)
            put("creeLe", note.createdAt)
            put("modifieLe", note.updatedAt)
            put("latitude", note.latitude ?: JSONObject.NULL)
            put("longitude", note.longitude ?: JSONObject.NULL)
            put("lieu", note.locationName ?: JSONObject.NULL)
            put("tags", JSONArray(note.tags))
            put("favori", note.isFavorite)
            put("archive", note.isArchived)
        }
        preferences.edit { putString(keyFor(note.id), json.toString()) }
    }

    fun clear(noteId: Long) {
        preferences.edit { remove(keyFor(noteId)) }
    }

    private fun keyFor(noteId: Long): String = if (noteId == 0L) {
        "nouvelle_note"
    } else {
        "note_$noteId"
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.nullableDouble(key: String): Double? =
        if (isNull(key) || !has(key)) null else optDouble(key)

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val tag = optString(index).trim()
                if (tag.isNotEmpty()) add(tag)
            }
        }
    }
}
