package com.ynov.geotagged_notebook.ui.screens

import android.Manifest
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ynov.geotagged_notebook.R
import com.ynov.geotagged_notebook.data.Note
import com.ynov.geotagged_notebook.ui.theme.YouNotesBackground
import com.ynov.geotagged_notebook.ui.theme.YouNotesBlue
import com.ynov.geotagged_notebook.ui.theme.YouNotesGreen
import com.ynov.geotagged_notebook.ui.theme.YouNotesOutline
import com.ynov.geotagged_notebook.ui.theme.YouNotesPurple
import com.ynov.geotagged_notebook.ui.theme.YouNotesRose
import com.ynov.geotagged_notebook.ui.theme.YouNotesSurface
import com.ynov.geotagged_notebook.ui.theme.YouNotesSurfaceHigh
import com.ynov.geotagged_notebook.ui.theme.YouNotesText
import com.ynov.geotagged_notebook.ui.theme.YouNotesTextMuted
import com.ynov.geotagged_notebook.utils.LocationUtils
import kotlinx.coroutines.launch
import java.io.File

private enum class NoteSortMode { CREATION, MODIFICATION, PROXIMITY }
private enum class NoteSection { ALL, FAVORITES, ARCHIVES }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    viewModel: com.ynov.geotagged_notebook.ui.viewmodel.NoteListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNoteClick: (Long) -> Unit,
    onAddNoteClick: () -> Unit,
    onOpenMapClick: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val onToggleFavorite: (Note) -> Unit = { note -> viewModel.toggleFavorite(note) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var query by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf(NoteSection.ALL) }
    var sortMode by rememberSaveable { mutableStateOf(NoteSortMode.MODIFICATION) }
    var currentLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var currentLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    fun fetchLocationForSorting() {
        scope.launch {
            val location = LocationUtils.getCurrentLocation(context)
            if (location != null) {
                currentLatitude = location.latitude
                currentLongitude = location.longitude
            } else {
                Toast.makeText(context, context.getString(R.string.location_sort_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchLocationForSorting()
        else Toast.makeText(
            context,
            context.getString(R.string.location_permission_denied),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun selectSort(mode: NoteSortMode) {
        sortMode = mode
        if (mode == NoteSortMode.PROXIMITY && currentLatitude == null) {
            if (LocationUtils.hasLocationPermission(context)) fetchLocationForSorting()
            else locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val filteredNotes = remember(
        notes,
        query,
        section,
        sortMode,
        currentLatitude,
        currentLongitude
    ) {
        notes.asSequence()
            .filter {
                when (section) {
                    NoteSection.ALL -> !it.isArchived
                    NoteSection.FAVORITES -> it.isFavorite && !it.isArchived
                    NoteSection.ARCHIVES -> it.isArchived
                }
            }
            .filter {
                query.isBlank() ||
                        it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true) ||
                        it.locationName?.contains(query, ignoreCase = true) == true ||
                        it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
            .let { sequence ->
                when (sortMode) {
                    NoteSortMode.CREATION -> sequence.sortedByDescending { it.createdAt }
                    NoteSortMode.MODIFICATION -> sequence.sortedByDescending { it.updatedAt }
                    NoteSortMode.PROXIMITY -> sequence.sortedBy {
                        distanceFromCurrentPosition(it, currentLatitude, currentLongitude)
                            ?: Float.MAX_VALUE
                    }
                }
            }
            .toList()
    }

    val pinnedNotes = if (section == NoteSection.ALL) filteredNotes.filter { it.isFavorite } else emptyList()
    val regularNotes = if (section == NoteSection.ALL) filteredNotes.filterNot { it.isFavorite } else filteredNotes

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_younotes)) },
            text = { Text(stringResource(R.string.about_description)) },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = YouNotesSurface,
                drawerContentColor = YouNotesText
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.my_notes)) },
                    selected = section == NoteSection.ALL,
                    onClick = {
                        section = NoteSection.ALL
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.favorite_notes)) },
                    selected = section == NoteSection.FAVORITES,
                    onClick = {
                        section = NoteSection.FAVORITES
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.archived_notes)) },
                    selected = section == NoteSection.ARCHIVES,
                    onClick = {
                        section = NoteSection.ARCHIVES
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.map_notes)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenMapClick()
                    },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    color = YouNotesOutline
                )
                Text(
                    text = stringResource(R.string.sort_notes),
                    color = YouNotesTextMuted,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )
                NoteSortMode.entries.forEach { mode ->
                    NavigationDrawerItem(
                        label = { Text(sortModeLabel(mode)) },
                        selected = sortMode == mode,
                        onClick = { selectSort(mode) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = YouNotesBackground,
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(YouNotesBlue, YouNotesPurple))
                        )
                        .clickable(onClick = onAddNoteClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = stringResource(R.string.add_note),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                YouNotesSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onProfileClick = { showAboutDialog = true }
                )

                if (filteredNotes.isEmpty()) {
                    YouNotesEmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 72.dp),
                        title = when {
                            query.isNotBlank() -> stringResource(R.string.no_search_results)
                            section == NoteSection.FAVORITES -> stringResource(R.string.no_favorite_notes)
                            section == NoteSection.ARCHIVES -> stringResource(R.string.no_archived_notes)
                            else -> stringResource(R.string.no_notes_title)
                        },
                        showAction = query.isBlank() && section == NoteSection.ALL,
                        onCreateClick = onAddNoteClick
                    )
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 92.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp
                    ) {
                        if (pinnedNotes.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SectionHeader(stringResource(R.string.pinned_notes))
                            }
                            items(pinnedNotes, key = { "pinned-${it.id}" }) { note ->
                                YouNotesCard(note, onNoteClick, onToggleFavorite)
                            }
                            if (regularNotes.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    SectionHeader(stringResource(R.string.all_notes), topPadding = 24.dp)
                                }
                            }
                        } else if (section != NoteSection.ALL) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SectionHeader(
                                    if (section == NoteSection.FAVORITES) {
                                        stringResource(R.string.favorite_notes)
                                    } else {
                                        stringResource(R.string.archived_notes)
                                    }
                                )
                            }
                        } else {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionHeader(stringResource(R.string.all_notes))
                                    Text(
                                        pluralStringResource(R.plurals.note_count, filteredNotes.size, filteredNotes.size),
                                        color = YouNotesTextMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                        items(regularNotes, key = { it.id }) { note ->
                            YouNotesCard(note, onNoteClick, onToggleFavorite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YouNotesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 18.dp),
        shape = RoundedCornerShape(30.dp),
        color = YouNotesSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(R.string.menu),
                    tint = YouNotesTextMuted
                )
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, YouNotesOutline.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = YouNotesText,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize
                        ),
                        cursorBrush = SolidColor(YouNotesBlue),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    stringResource(R.string.search_note),
                                    color = YouNotesTextMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = YouNotesTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = YouNotesTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            IconButton(onClick = onProfileClick) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = stringResource(R.string.profile),
                    tint = Color.White,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
    }
}

@Composable
private fun YouNotesEmptyState(
    modifier: Modifier,
    title: String,
    showAction: Boolean,
    onCreateClick: () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                repeat(3) { index ->
                    Surface(
                        modifier = Modifier
                            .size(width = 78.dp, height = 62.dp)
                            .padding(start = (index * 5).dp, top = (index * 6).dp),
                        shape = RoundedCornerShape(5.dp),
                        color = YouNotesBackground,
                        border = BorderStroke(1.dp, YouNotesTextMuted.copy(alpha = 0.55f))
                    ) {}
                }
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = YouNotesTextMuted.copy(alpha = 0.75f),
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = YouNotesText)
            if (showAction) {
                TextButton(onClick = onCreateClick) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.no_notes_action))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = YouNotesText,
        modifier = Modifier.padding(top = topPadding, bottom = 2.dp, start = 2.dp)
    )
}

@Composable
private fun YouNotesCard(
    note: Note,
    onNoteClick: (Long) -> Unit,
    onToggleFavorite: (Note) -> Unit
) {
    val brush = remember(note.id) {
        when ((note.id % 6).toInt()) {
            3 -> Brush.linearGradient(listOf(YouNotesRose, Color(0xFF9F314E)))
            5 -> Brush.linearGradient(listOf(YouNotesGreen, YouNotesPurple))
            else -> Brush.linearGradient(listOf(YouNotesSurfaceHigh, Color(0xFF151515)))
        }
    }
    val hasImage = !note.imageUri.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (hasImage) 196.dp else 132.dp)
            .clickable { onNoteClick(note.id) },
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, YouNotesOutline.copy(alpha = 0.8f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        ) {
            if (hasImage) {
                val image = note.imageUri.orEmpty()
                AsyncImage(
                    model = if (image.startsWith("/")) File(image) else image,
                    contentDescription = stringResource(R.string.note_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = if (hasImage) 102.dp else 12.dp, bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        note.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onToggleFavorite(note) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            if (note.isFavorite) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = stringResource(
                                if (note.isFavorite) R.string.unpin_note else R.string.pin_note
                            ),
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                if (note.content.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = if (hasImage) 3 else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        note.formattedCreatedDate.substringBefore(" à"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Default.Tag,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        note.tags.firstOrNull()
                            ?: note.locationName
                            ?: stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun sortModeLabel(mode: NoteSortMode): String = stringResource(
    when (mode) {
        NoteSortMode.CREATION -> R.string.sort_creation
        NoteSortMode.MODIFICATION -> R.string.sort_modification
        NoteSortMode.PROXIMITY -> R.string.sort_proximity
    }
)

private fun distanceFromCurrentPosition(
    note: Note,
    currentLatitude: Double?,
    currentLongitude: Double?
): Float? {
    val noteLatitude = note.latitude ?: return null
    val noteLongitude = note.longitude ?: return null
    if (currentLatitude == null || currentLongitude == null) return null
    val results = FloatArray(1)
    Location.distanceBetween(
        currentLatitude,
        currentLongitude,
        noteLatitude,
        noteLongitude,
        results
    )
    return results[0]
}
