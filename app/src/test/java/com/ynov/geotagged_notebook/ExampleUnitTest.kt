package com.ynov.geotagged_notebook

import com.ynov.geotagged_notebook.data.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteTest {
    @Test
    fun creationAndModificationDatesAreDistinct() {
        val note = Note(
            title = "Une note",
            content = "Contenu",
            createdAt = 0L,
            updatedAt = 86_400_000L
        )

        assertNotEquals(note.formattedCreatedDate, note.formattedUpdatedDate)
    }

    @Test
    fun noteKeepsTagsFavoriteAndArchiveStatuses() {
        val note = Note(
            title = "Randonnée",
            content = "",
            tags = listOf("nature", "Alpes"),
            isFavorite = true,
            isArchived = true
        )

        assertEquals(listOf("nature", "Alpes"), note.tags)
        assertTrue(note.isFavorite)
        assertTrue(note.isArchived)
    }

    @Test
    fun missingLocationLetsTheInterfaceUseItsLocalizedResource() {
        val note = Note(title = "Sans position", content = "")

        assertEquals("", note.displayLocation)
    }
}
