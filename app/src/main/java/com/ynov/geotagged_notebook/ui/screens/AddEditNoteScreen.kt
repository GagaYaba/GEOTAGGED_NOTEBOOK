package com.ynov.geotagged_notebook.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ynov.geotagged_notebook.R
import com.ynov.geotagged_notebook.data.Note
import com.ynov.geotagged_notebook.data.NoteDraftStore
import com.ynov.geotagged_notebook.ui.theme.YouNotesBackground
import com.ynov.geotagged_notebook.ui.theme.YouNotesField
import com.ynov.geotagged_notebook.ui.theme.YouNotesOutline
import com.ynov.geotagged_notebook.ui.theme.YouNotesSurface
import com.ynov.geotagged_notebook.ui.theme.YouNotesSurfaceHigh
import com.ynov.geotagged_notebook.ui.theme.YouNotesText
import com.ynov.geotagged_notebook.ui.theme.YouNotesTextMuted
import com.ynov.geotagged_notebook.utils.ImageUtils
import com.ynov.geotagged_notebook.utils.LocationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddEditNoteScreen(
    noteId: Long,
    viewModel: com.ynov.geotagged_notebook.ui.viewmodel.AddEditNoteViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val note by viewModel.note.collectAsState()
    val restoredDraft by viewModel.restoredDraft.collectAsState()

    LaunchedEffect(noteId) {
        viewModel.loadNoteAndDraft(noteId)
    }

    val initialNote = restoredDraft ?: note
    val effectiveNoteId = note?.id ?: if (noteId > 0L) noteId else 0L
    val createdAt = remember(effectiveNoteId, initialNote) { initialNote?.createdAt ?: System.currentTimeMillis() }
    val displayedUpdatedAt = remember(effectiveNoteId, initialNote) { initialNote?.updatedAt ?: createdAt }

    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var imageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var locationName by rememberSaveable { mutableStateOf("") }
    var tagsText by rememberSaveable { mutableStateOf("") }
    var isFavorite by rememberSaveable { mutableStateOf(false) }
    var isArchived by rememberSaveable { mutableStateOf(false) }

    var isInitialized by remember(noteId) { mutableStateOf(false) }
    LaunchedEffect(initialNote) {
        if (initialNote != null && !isInitialized) {
            title = initialNote.title
            content = initialNote.content
            imageUriString = initialNote.imageUri
            latitude = initialNote.latitude
            longitude = initialNote.longitude
            locationName = initialNote.locationName.orEmpty()
            tagsText = initialNote.tags.joinToString(", ")
            isFavorite = initialNote.isFavorite
            isArchived = initialNote.isArchived
            isInitialized = true
        } else if (initialNote == null && noteId == 0L && !isInitialized) {
            isInitialized = true
        }
    }

    var tempCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.FRANCE) }
    val dateTimeFormatter = remember { SimpleDateFormat("d MMM yyyy 'à' HH:mm", Locale.FRANCE) }
    val createdDate = remember(createdAt) { dateFormatter.format(Date(createdAt)) }
    val updatedDate = remember(displayedUpdatedAt) { dateTimeFormatter.format(Date(displayedUpdatedAt)) }

    fun normalizedTags(): List<String> = tagsText
        .split(',')
        .map { it.trim().removePrefix("#") }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase(Locale.FRANCE) }

    fun currentNote(updatedAt: Long = System.currentTimeMillis()) = Note(
        id = effectiveNoteId,
        title = title.trim(),
        content = content.trim(),
        imageUri = imageUriString,
        createdAt = createdAt,
        updatedAt = updatedAt,
        latitude = latitude,
        longitude = longitude,
        locationName = locationName.ifBlank { null },
        tags = normalizedTags(),
        isFavorite = isFavorite,
        isArchived = isArchived
    )

    val original = initialNote
    val hasChanges = title != original?.title.orEmpty() ||
            content != original?.content.orEmpty() ||
            imageUriString != original?.imageUri ||
            latitude != original?.latitude ||
            longitude != original?.longitude ||
            locationName != original?.locationName.orEmpty() ||
            normalizedTags() != original?.tags.orEmpty() ||
            isFavorite != (original?.isFavorite ?: false) ||
            isArchived != (original?.isArchived ?: false)

    LaunchedEffect(
        title,
        content,
        imageUriString,
        latitude,
        longitude,
        locationName,
        tagsText,
        isFavorite,
        isArchived
    ) {
        delay(500)
        if (hasChanges) viewModel.saveDraft(currentNote()) else viewModel.clearDraft(effectiveNoteId)
    }

    fun requestExit() {
        if (hasChanges) showExitDialog = true else onBackClick()
    }

    fun saveNote() {
        if (title.isBlank()) {
            Toast.makeText(context, context.getString(R.string.title_required), Toast.LENGTH_SHORT).show()
            return
        }
        if (isSaving) return
        isSaving = true
        scope.launch {
            val saved = viewModel.saveNote(currentNote())
            isSaving = false
            if (saved) {
                viewModel.clearDraft(effectiveNoteId)
                onBackClick()
            } else {
                Toast.makeText(context, context.getString(R.string.save_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteNote() {
        if (effectiveNoteId == 0L || isDeleting) return
        isDeleting = true
        scope.launch {
            val deleted = viewModel.deleteNote(effectiveNoteId)
            isDeleting = false
            if (deleted) {
                viewModel.clearDraft(effectiveNoteId)
                onBackClick()
            } else {
                Toast.makeText(context, context.getString(R.string.delete_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleSelectedImage(uri: Uri) {
        scope.launch {
            val path = ImageUtils.saveImageToInternalStorage(context, uri)
            if (path != null) imageUriString = path
            else Toast.makeText(
                context,
                context.getString(R.string.image_save_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun fetchLocation() {
        if (!LocationUtils.hasLocationPermission(context)) return
        isLocating = true
        scope.launch {
            val location = LocationUtils.getCurrentLocation(context)
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                locationName = LocationUtils.getAddressFromCoordinates(
                    context,
                    location.latitude,
                    location.longitude
                )
            } else {
                Toast.makeText(context, context.getString(R.string.location_error), Toast.LENGTH_SHORT).show()
            }
            isLocating = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = tempCameraUriString?.let(Uri::parse)
        if (success && uri != null) handleSelectedImage(uri)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = ImageUtils.createTempImageUri(context)
            tempCameraUriString = uri.toString()
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.camera_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let(::handleSelectedImage)
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchLocation()
        else Toast.makeText(
            context,
            context.getString(R.string.location_permission_denied),
            Toast.LENGTH_SHORT
        ).show()
    }
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                content = listOf(content.trim(), spokenText).filter { it.isNotBlank() }.joinToString(" ")
            }
        }
    }

    fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRANCE.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.speech_prompt))
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.speech_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(onBack = ::requestExit)

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.leave_edit_title)) },
            text = { Text(stringResource(R.string.leave_edit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveDraft(currentNote())
                    showExitDialog = false
                    onBackClick()
                }) { Text(stringResource(R.string.keep_draft_leave)) }
            },
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        viewModel.clearDraft(effectiveNoteId)
                        showExitDialog = false
                        onBackClick()
                    }) { Text(stringResource(R.string.discard_changes)) }
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(stringResource(R.string.continue_editing))
                    }
                }
            }
        )
    }

    if (showDeleteDialog && effectiveNoteId != 0L) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_note)) },
            text = { Text(stringResource(R.string.delete_note_confirmation, note?.title.orEmpty())) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deleteNote()
                    },
                    enabled = !isDeleting
                ) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }



    Scaffold(
        containerColor = YouNotesBackground,
        bottomBar = {
            Surface(color = YouNotesBackground) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .height(72.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorAction(
                        icon = { Icon(Icons.Default.Image, contentDescription = null) },
                        description = stringResource(R.string.gallery),
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                    EditorAction(
                        icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                        description = stringResource(R.string.camera),
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                    EditorAction(
                        icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                        description = stringResource(R.string.dictate_note),
                        onClick = ::startSpeechRecognition
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.created_on, createdDate),
                        color = YouNotesTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = ::requestExit) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = YouNotesText
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        if (isFavorite) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = stringResource(
                            if (isFavorite) R.string.unpin_note else R.string.pin_note
                        ),
                        tint = YouNotesText
                    )
                }
            }

            if (restoredDraft != null) {
                Text(
                    stringResource(R.string.draft_restored),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            EditorTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = stringResource(R.string.note_title_hint),
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                color = YouNotesField,
                shape = RoundedCornerShape(4.dp)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    BasicTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 26.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = YouNotesText),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                        decorationBox = { innerTextField ->
                            if (content.isEmpty()) {
                                Text(
                                    stringResource(R.string.note_content_hint),
                                    color = YouNotesTextMuted,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
                        }
                    )
                    Text(
                        stringResource(R.string.last_modified, updatedDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = YouNotesTextMuted.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = YouNotesSurface,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.gps_location),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = YouNotesText
                        )
                    }
                    if (latitude != null && longitude != null) {
                        Text(
                            text = locationName.ifBlank { stringResource(R.string.saved_position) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = YouNotesText
                        )
                        Text(
                            text = stringResource(
                                R.string.coordinates,
                                String.format(Locale.FRANCE, "%.5f", latitude),
                                String.format(Locale.FRANCE, "%.5f", longitude)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = YouNotesTextMuted
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.no_gps_position),
                            style = MaterialTheme.typography.bodyMedium,
                            color = YouNotesTextMuted
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            if (LocationUtils.hasLocationPermission(context)) fetchLocation()
                            else locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLocating
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.locating))
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(
                                    if (latitude == null) R.string.get_position else R.string.refresh_position
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = YouNotesSurface,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.add_photo_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = YouNotesText
                        )
                    }
                    Text(
                        text = stringResource(R.string.add_photo_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = YouNotesTextMuted
                    )

                    val image = imageUriString
                    if (!image.isNullOrBlank()) {
                        AsyncImage(
                            model = if (image.startsWith("/")) File(image) else image,
                            contentDescription = stringResource(R.string.attached_photo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        OutlinedButton(
                            onClick = { imageUriString = null },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.remove_photo), color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.gallery))
                            }
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.take_photo))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            var optionsExpanded by rememberSaveable(noteId) { mutableStateOf(false) }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = YouNotesSurface,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { optionsExpanded = !optionsExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.note_options),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = YouNotesText,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { optionsExpanded = !optionsExpanded }) {
                            Icon(
                                imageVector = if (optionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = YouNotesText
                            )
                        }
                    }
                    AnimatedVisibility(visible = optionsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = tagsText,
                                onValueChange = { tagsText = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.tags)) },
                                supportingText = { Text(stringResource(R.string.tags_hint)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            OptionSwitchRow(
                                icon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                                text = stringResource(R.string.add_favorite),
                                checked = isFavorite,
                                onCheckedChange = { isFavorite = it }
                            )
                            OptionSwitchRow(
                                icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                text = stringResource(R.string.archive_note),
                                checked = isArchived,
                                onCheckedChange = { isArchived = it }
                            )
                        }
                    }
                }
            }

            if (effectiveNoteId != 0L) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_note), color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = ::saveNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.save_note_action),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    singleLine: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = YouNotesField, shape = RoundedCornerShape(4.dp)) {
        Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = textStyle.copy(color = YouNotesText),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                singleLine = singleLine,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = YouNotesTextMuted, style = textStyle)
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun EditorAction(
    icon: @Composable () -> Unit,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description }
    ) {
        icon()
    }
}

@Composable
private fun OptionSwitchRow(
    icon: @Composable () -> Unit,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(color = YouNotesSurfaceHigh, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.width(12.dp))
            Text(text, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
