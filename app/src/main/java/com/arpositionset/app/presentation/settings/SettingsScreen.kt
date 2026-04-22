package com.arpositionset.app.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arpositionset.app.data.remote.MarkerImageImporter
import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.domain.model.SceneBinding
import com.arpositionset.app.presentation.theme.ArColors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface MarkerImporterEntryPoint {
    fun importer(): MarkerImageImporter
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snack) {
        state.snack?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(SettingsAction.SnackConsumed)
        }
    }

    Scaffold(
        containerColor = ArColors.BackgroundDark,
        contentColor = ArColors.OnSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Привязки маркеров") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = ArColors.OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ArColors.SurfaceDark,
                    titleContentColor = ArColors.OnSurface,
                ),
            )
        },
        floatingActionButton = {
            if (state.editor == null) {
                FloatingActionButton(
                    onClick = { viewModel.onAction(SettingsAction.AddNew) },
                    containerColor = ArColors.BrandGreen,
                    contentColor = ArColors.OnBrandGreen,
                ) {
                    Icon(Icons.Rounded.Add, null)
                }
            }
        },
    ) { paddings ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .background(
                    Brush.verticalGradient(
                        listOf(ArColors.BackgroundDark, ArColors.SurfaceDark),
                    )
                ),
        ) {
            if (state.editor != null) {
                EditorForm(
                    editor = state.editor!!,
                    galleryObjects = state.galleryObjects,
                    onAction = viewModel::onAction,
                )
            } else {
                BindingsList(
                    bindings = state.bindings,
                    galleryObjects = state.galleryObjects,
                    onAction = viewModel::onAction,
                )
            }
        }
    }
}

@Composable
private fun BindingsList(
    bindings: List<SceneBinding>,
    galleryObjects: List<ArObject>,
    onAction: (SettingsAction) -> Unit,
) {
    if (bindings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Пока нет привязок",
                    style = MaterialTheme.typography.titleMedium,
                    color = ArColors.OnSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Добавьте картинку-маркер, укажите её физическую ширину и модель, которая будет появляться при сканировании.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArColors.OnSurfaceMuted,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(bindings, key = SceneBinding::id) { binding ->
            val obj = galleryObjects.firstOrNull { it.id == binding.objectId }
            BindingCard(
                binding = binding,
                objectName = obj?.name ?: "—",
                objectEmoji = obj?.previewEmoji ?: "📦",
                onEdit = { onAction(SettingsAction.SelectForEdit(binding.id)) },
                onDelete = { onAction(SettingsAction.DeleteBinding(binding.id)) },
            )
        }
    }
}

@Composable
private fun BindingCard(
    binding: SceneBinding,
    objectName: String,
    objectEmoji: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ArColors.SurfaceElevated,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onEdit),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(binding.markerAssetUri)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ArColors.SurfaceDark),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = binding.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ArColors.OnSurface,
                )
                Text(
                    text = "$objectEmoji $objectName",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArColors.Turquoise,
                )
                Text(
                    text = "Ширина маркера: ${"%.1f".format(binding.markerWidthMeters * 100)} см",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArColors.OnSurfaceMuted,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, null, tint = ArColors.OnSurfaceMuted)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = ArColors.BrandRed)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorForm(
    editor: BindingEditor,
    galleryObjects: List<ArObject>,
    onAction: (SettingsAction) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importer = remember {
        EntryPointAccessors
            .fromApplication(context.applicationContext, MarkerImporterEntryPoint::class.java)
            .importer()
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching { importer.import(uri) }
                .onSuccess { path -> onAction(SettingsAction.EditorMarkerPath(path)) }
        }
    }

    var objectMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = editor.title,
            onValueChange = { onAction(SettingsAction.EditorTitle(it)) },
            label = { Text("Название (комната, участок)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ArColors.SurfaceElevated,
            modifier = Modifier.fillMaxWidth().clickable { picker.launch(arrayOf("image/*")) },
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (editor.markerLocalPath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data("file://${editor.markerLocalPath}").build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ArColors.SurfaceDark),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.FileUpload, null, tint = ArColors.Turquoise)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Картинка-маркер", style = MaterialTheme.typography.titleSmall, color = ArColors.OnSurface)
                    Text(
                        text = if (editor.markerLocalPath != null) "Выбрано. Тап чтобы заменить" else "Нажмите чтобы выбрать из галереи",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArColors.OnSurfaceMuted,
                    )
                }
            }
        }

        OutlinedTextField(
            value = editor.widthCm,
            onValueChange = { onAction(SettingsAction.EditorWidth(it.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.'))) },
            label = { Text("Физическая ширина маркера, см") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        val selectedObject = galleryObjects.firstOrNull { it.id == editor.objectId }
        Box {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ArColors.SurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { objectMenuOpen = true },
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Объект для привязки",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArColors.OnSurfaceMuted,
                        )
                        Text(
                            text = selectedObject?.let { "${it.previewEmoji} ${it.name}" } ?: "Выберите…",
                            style = MaterialTheme.typography.titleMedium,
                            color = ArColors.OnSurface,
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = objectMenuOpen,
                onDismissRequest = { objectMenuOpen = false },
            ) {
                galleryObjects.forEach { obj ->
                    DropdownMenuItem(
                        text = { Text("${obj.previewEmoji} ${obj.name}") },
                        onClick = {
                            onAction(SettingsAction.EditorObject(obj.id))
                            objectMenuOpen = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { onAction(SettingsAction.DismissEditor) }) { Text("Отмена") }
            androidx.compose.material3.Button(
                onClick = { onAction(SettingsAction.SaveEditor) },
                enabled = editor.canSave,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ArColors.BrandGreen,
                    contentColor = ArColors.OnBrandGreen,
                ),
            ) { Text("Сохранить") }
        }
    }
}
