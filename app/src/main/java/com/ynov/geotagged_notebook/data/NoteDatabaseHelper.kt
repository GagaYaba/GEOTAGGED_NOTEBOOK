package com.ynov.geotagged_notebook.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

class NoteDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "geotagged_notebook.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_NOTES = "notes"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_IMAGE_URI = "image_uri"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_LOCATION_NAME = "location_name"
        private const val COLUMN_TAGS = "tags"
        private const val COLUMN_IS_FAVORITE = "is_favorite"
        private const val COLUMN_IS_ARCHIVED = "is_archived"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NOTES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_CONTENT TEXT NOT NULL,
                $COLUMN_IMAGE_URI TEXT,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_LOCATION_NAME TEXT,
                $COLUMN_TAGS TEXT NOT NULL DEFAULT '[]',
                $COLUMN_IS_FAVORITE INTEGER NOT NULL DEFAULT 0,
                $COLUMN_IS_ARCHIVED INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_CREATED_AT INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_UPDATED_AT INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_TAGS TEXT NOT NULL DEFAULT '[]'")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_IS_FAVORITE INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_IS_ARCHIVED INTEGER NOT NULL DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL(
                    "UPDATE $TABLE_NOTES SET $COLUMN_CREATED_AT = $COLUMN_TIMESTAMP, " +
                            "$COLUMN_UPDATED_AT = $COLUMN_TIMESTAMP"
                )
            } catch (_: Exception) {}
        }
    }

    fun getAllNotes(): List<Note> {
        val notesList = mutableListOf<Note>()
        try {
            val selectQuery = "SELECT * FROM $TABLE_NOTES ORDER BY $COLUMN_UPDATED_AT DESC"
            val db = readableDatabase
            val cursor = db.rawQuery(selectQuery, null)

            cursor.use { c ->
                if (c.moveToFirst()) {
                    val idIndex = c.getColumnIndexOrThrow(COLUMN_ID)
                    val titleIndex = c.getColumnIndexOrThrow(COLUMN_TITLE)
                    val contentIndex = c.getColumnIndexOrThrow(COLUMN_CONTENT)
                    val imageUriIndex = c.getColumnIndexOrThrow(COLUMN_IMAGE_URI)
                    val createdAtIndex = c.getColumnIndexOrThrow(COLUMN_CREATED_AT)
                    val updatedAtIndex = c.getColumnIndexOrThrow(COLUMN_UPDATED_AT)
                    val latitudeIndex = c.getColumnIndexOrThrow(COLUMN_LATITUDE)
                    val longitudeIndex = c.getColumnIndexOrThrow(COLUMN_LONGITUDE)
                    val locationNameIndex = c.getColumnIndexOrThrow(COLUMN_LOCATION_NAME)
                    val tagsIndex = c.getColumnIndexOrThrow(COLUMN_TAGS)
                    val favoriteIndex = c.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)
                    val archivedIndex = c.getColumnIndexOrThrow(COLUMN_IS_ARCHIVED)

                    do {
                        val id = c.getLong(idIndex)
                        val title = c.getString(titleIndex)
                        val content = c.getString(contentIndex)
                        val imageUri = if (!c.isNull(imageUriIndex)) c.getString(imageUriIndex) else null
                        val createdAt = c.getLong(createdAtIndex)
                        val updatedAt = c.getLong(updatedAtIndex)
                        val latitude = if (!c.isNull(latitudeIndex)) c.getDouble(latitudeIndex) else null
                        val longitude = if (!c.isNull(longitudeIndex)) c.getDouble(longitudeIndex) else null
                        val locationName = if (!c.isNull(locationNameIndex)) c.getString(locationNameIndex) else null

                        notesList.add(
                            Note(
                                id = id,
                                title = title,
                                content = content,
                                imageUri = imageUri,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                latitude = latitude,
                                longitude = longitude,
                                locationName = locationName,
                                tags = parseTags(c.getString(tagsIndex)),
                                isFavorite = c.getInt(favoriteIndex) == 1,
                                isArchived = c.getInt(archivedIndex) == 1
                            )
                        )
                    } while (c.moveToNext())
                }
            }
        } catch (_: Exception) {}
        return notesList
    }

    fun getNoteById(id: Long): Note? {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_NOTES,
                null,
                "$COLUMN_ID = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )

            cursor.use { c ->
                if (c.moveToFirst()) {
                    val idIndex = c.getColumnIndexOrThrow(COLUMN_ID)
                    val titleIndex = c.getColumnIndexOrThrow(COLUMN_TITLE)
                    val contentIndex = c.getColumnIndexOrThrow(COLUMN_CONTENT)
                    val imageUriIndex = c.getColumnIndexOrThrow(COLUMN_IMAGE_URI)
                    val createdAtIndex = c.getColumnIndexOrThrow(COLUMN_CREATED_AT)
                    val updatedAtIndex = c.getColumnIndexOrThrow(COLUMN_UPDATED_AT)
                    val latitudeIndex = c.getColumnIndexOrThrow(COLUMN_LATITUDE)
                    val longitudeIndex = c.getColumnIndexOrThrow(COLUMN_LONGITUDE)
                    val locationNameIndex = c.getColumnIndexOrThrow(COLUMN_LOCATION_NAME)
                    val tagsIndex = c.getColumnIndexOrThrow(COLUMN_TAGS)
                    val favoriteIndex = c.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)
                    val archivedIndex = c.getColumnIndexOrThrow(COLUMN_IS_ARCHIVED)

                    Note(
                        id = c.getLong(idIndex),
                        title = c.getString(titleIndex),
                        content = c.getString(contentIndex),
                        imageUri = if (!c.isNull(imageUriIndex)) c.getString(imageUriIndex) else null,
                        createdAt = c.getLong(createdAtIndex),
                        updatedAt = c.getLong(updatedAtIndex),
                        latitude = if (!c.isNull(latitudeIndex)) c.getDouble(latitudeIndex) else null,
                        longitude = if (!c.isNull(longitudeIndex)) c.getDouble(longitudeIndex) else null,
                        locationName = if (!c.isNull(locationNameIndex)) c.getString(locationNameIndex) else null,
                        tags = parseTags(c.getString(tagsIndex)),
                        isFavorite = c.getInt(favoriteIndex) == 1,
                        isArchived = c.getInt(archivedIndex) == 1
                    )
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun insertNote(note: Note): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.title)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_IMAGE_URI, note.imageUri)
            put(COLUMN_TIMESTAMP, note.createdAt)
            put(COLUMN_CREATED_AT, note.createdAt)
            put(COLUMN_UPDATED_AT, note.updatedAt)
            put(COLUMN_LATITUDE, note.latitude)
            put(COLUMN_LONGITUDE, note.longitude)
            put(COLUMN_LOCATION_NAME, note.locationName)
            put(COLUMN_TAGS, JSONArray(note.tags).toString())
            put(COLUMN_IS_FAVORITE, if (note.isFavorite) 1 else 0)
            put(COLUMN_IS_ARCHIVED, if (note.isArchived) 1 else 0)
        }
        return db.insert(TABLE_NOTES, null, values)
    }

    fun updateNote(note: Note): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.title)
            put(COLUMN_CONTENT, note.content)
            put(COLUMN_IMAGE_URI, note.imageUri)
            put(COLUMN_TIMESTAMP, note.createdAt)
            put(COLUMN_CREATED_AT, note.createdAt)
            put(COLUMN_UPDATED_AT, note.updatedAt)
            put(COLUMN_LATITUDE, note.latitude)
            put(COLUMN_LONGITUDE, note.longitude)
            put(COLUMN_LOCATION_NAME, note.locationName)
            put(COLUMN_TAGS, JSONArray(note.tags).toString())
            put(COLUMN_IS_FAVORITE, if (note.isFavorite) 1 else 0)
            put(COLUMN_IS_ARCHIVED, if (note.isArchived) 1 else 0)
        }
        return db.update(TABLE_NOTES, values, "$COLUMN_ID = ?", arrayOf(note.id.toString()))
    }

    fun deleteNote(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    private fun parseTags(rawTags: String?): List<String> {
        if (rawTags.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(rawTags)
            buildList {
                for (index in 0 until array.length()) {
                    val tag = array.optString(index).trim()
                    if (tag.isNotEmpty()) add(tag)
                }
            }
        }.getOrDefault(emptyList())
    }
}
