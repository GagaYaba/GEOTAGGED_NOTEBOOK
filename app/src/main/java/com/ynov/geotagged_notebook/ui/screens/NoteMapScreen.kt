package com.ynov.geotagged_notebook.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.ynov.geotagged_notebook.R
import com.ynov.geotagged_notebook.data.Note
import com.ynov.geotagged_notebook.ui.theme.YouNotesBackground
import com.ynov.geotagged_notebook.ui.theme.YouNotesSurface
import com.ynov.geotagged_notebook.ui.theme.YouNotesText
import com.ynov.geotagged_notebook.ui.theme.YouNotesTextMuted
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteMapScreen(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    val locationUnavailable = stringResource(R.string.location_unavailable)

    // Initialize osmdroid configuration
    remember {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_notes), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = YouNotesBackground,
                    titleContentColor = YouNotesText
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(13.0)
                        // Centre géographique de la France métropolitaine.
                        val defaultPoint = GeoPoint(46.603354, 1.888334)
                        controller.setZoom(6.0)
                        controller.setCenter(defaultPoint)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    mapView.overlays.removeAll { it is Marker }

                    val notesWithLocation = notes.filter { it.latitude != null && it.longitude != null }
                    
                    if (notesWithLocation.isNotEmpty()) {
                        val firstLoc = notesWithLocation.first()
                        mapView.controller.setCenter(GeoPoint(firstLoc.latitude!!, firstLoc.longitude!!))

                        for (note in notesWithLocation) {
                            val marker = Marker(mapView).apply {
                                position = GeoPoint(note.latitude!!, note.longitude!!)
                                title = note.title
                                snippet = note.displayLocation.ifBlank { locationUnavailable }
                                setOnMarkerClickListener { m, _ ->
                                    selectedNote = note
                                    m.showInfoWindow()
                                    true
                                }
                            }
                            mapView.overlays.add(marker)
                        }
                    }
                    mapView.invalidate()
                }
            )

            // Selected Note preview card at bottom
            selectedNote?.let { note ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clickable { onNoteClick(note.id) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = YouNotesSurface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val imgUri = note.imageUri
                        if (!imgUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = if (imgUri.startsWith("/")) File(imgUri) else imgUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = note.displayLocation.ifBlank { locationUnavailable },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.open_note_details),
                                style = MaterialTheme.typography.labelSmall,
                                color = YouNotesTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
