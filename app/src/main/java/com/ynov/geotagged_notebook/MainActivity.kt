package com.ynov.geotagged_notebook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ynov.geotagged_notebook.data.NoteRepository
import com.ynov.geotagged_notebook.ui.screens.AddEditNoteScreen
import com.ynov.geotagged_notebook.ui.screens.NoteDetailScreen
import com.ynov.geotagged_notebook.ui.screens.NoteListScreen
import com.ynov.geotagged_notebook.ui.screens.NoteMapScreen
import com.ynov.geotagged_notebook.ui.theme.GEOTAGGED_NOTEBOOKTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GEOTAGGED_NOTEBOOKTheme {
                GeotaggedNotebookApp()
            }
        }
    }
}

@Composable
fun GeotaggedNotebookApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { NoteRepository.getInstance(context) }
    val notes by repository.notes.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        repository.refreshNotes()
    }

    NavHost(
        navController = navController,
        startDestination = "list",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("list") {
            NoteListScreen(
                notes = notes,
                onNoteClick = { noteId ->
                    navController.navigate("detail/$noteId")
                },
                onAddNoteClick = {
                    navController.navigate("add_edit")
                },
                onOpenMapClick = {
                    navController.navigate("map")
                },
                onToggleFavorite = { note ->
                    coroutineScope.launch {
                        repository.updateNote(
                            note.copy(
                                isFavorite = !note.isFavorite,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            )
        }

        composable("map") {
            NoteMapScreen(
                notes = notes.filterNot { it.isArchived },
                onNoteClick = { noteId ->
                    navController.navigate("detail/$noteId")
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "detail/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            val note = notes.find { it.id == noteId }

            NoteDetailScreen(
                note = note,
                onBackClick = {
                    navController.popBackStack()
                },
                onEditClick = { id ->
                    navController.navigate("add_edit?noteId=$id")
                },
                onDeleteClick = { id ->
                    coroutineScope.launch {
                        repository.deleteNote(id)
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(
            route = "add_edit?noteId={noteId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            val noteToEdit = if (noteId != -1L) notes.find { it.id == noteId } else null

            AddEditNoteScreen(
                noteToEdit = noteToEdit,
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveClick = { note ->
                    try {
                        if (note.id == 0L) {
                            repository.insertNote(note)
                        } else {
                            repository.updateNote(note)
                        }
                        true
                    } catch (_: Exception) {
                        false
                    }
                },
                onDeleteClick = { id ->
                    try {
                        repository.deleteNote(id)
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
            )
        }
    }
}
