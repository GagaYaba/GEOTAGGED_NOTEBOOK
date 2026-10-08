package com.ynov.geotagged_notebook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ynov.geotagged_notebook.ui.screens.AddEditNoteScreen
import com.ynov.geotagged_notebook.ui.screens.NoteDetailScreen
import com.ynov.geotagged_notebook.ui.screens.NoteListScreen
import com.ynov.geotagged_notebook.ui.screens.NoteMapScreen
import com.ynov.geotagged_notebook.ui.theme.GEOTAGGED_NOTEBOOKTheme
import com.ynov.geotagged_notebook.ui.viewmodel.AddEditNoteViewModel
import com.ynov.geotagged_notebook.ui.viewmodel.NoteListViewModel
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
    val navController = rememberNavController()
    val listViewModel: NoteListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val notes by listViewModel.notes.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = "list",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("list") {
            NoteListScreen(
                viewModel = listViewModel,
                onNoteClick = { noteId ->
                    navController.navigate("detail/$noteId")
                },
                onAddNoteClick = {
                    navController.navigate("add_edit")
                },
                onOpenMapClick = {
                    navController.navigate("map")
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
            val editViewModel: AddEditNoteViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

            NoteDetailScreen(
                noteId = noteId,
                onBackClick = {
                    navController.popBackStack()
                },
                onEditClick = { id ->
                    navController.navigate("add_edit?noteId=$id")
                },
                onDeleteClick = { id ->
                    coroutineScope.launch {
                        editViewModel.deleteNote(id)
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

            AddEditNoteScreen(
                noteId = if (noteId != -1L) noteId else 0L,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
