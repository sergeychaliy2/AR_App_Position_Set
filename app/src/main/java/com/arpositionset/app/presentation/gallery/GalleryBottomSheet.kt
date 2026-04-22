package com.arpositionset.app.presentation.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.domain.model.ObjectSource
import com.arpositionset.app.presentation.theme.ArColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryBottomSheet(
    onDismiss: () -> Unit,
    onSelect: (ArObject) -> Unit,
    onDownload: (ArObject) -> Unit,
    onImport: (String, String) -> Unit,
    downloadProgress: Float?,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) { /* already a content URI, ignore */ }
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "model.glb"
            onImport(uri.toString(), name)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ArColors.SurfaceDark,
        dragHandle = null,
        contentColor = ArColors.OnSurface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Header(activeTab = state.tab, onTabChange = viewModel::selectTab)
            Spacer(Modifier.height(16.dp))
            if (state.tab == GalleryTab.Imported) {
                ImportActionCard(onClick = { picker.launch(arrayOf("*/*")) })
                Spacer(Modifier.height(12.dp))
            }
            if (state.tab == GalleryTab.Cloud && downloadProgress != null) {
                DownloadBanner(progress = downloadProgress)
                Spacer(Modifier.height(12.dp))
            }
            val items = state.currentItems
            if (items.isEmpty()) {
                EmptyState(tab = state.tab, onImportClick = { picker.launch(arrayOf("*/*")) })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .heightIn(min = 240.dp),
                ) {
                    items(items, key = ArObject::id) { obj ->
                        ObjectCard(
                            obj = obj,
                            onClick = {
                                when (obj.source) {
                                    is ObjectSource.Cloud -> {
                                        val cached = (obj.source as ObjectSource.Cloud).cachedPath
                                        if (cached != null) onSelect(obj) else onDownload(obj)
                                    }
                                    else -> onSelect(obj)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(activeTab: GalleryTab, onTabChange: (GalleryTab) -> Unit) {
    Column {
        Text(
            text = "Библиотека объектов",
            style = MaterialTheme.typography.titleLarge,
            color = ArColors.OnSurface,
        )
        Text(
            text = "Выберите модель для размещения в AR",
            style = MaterialTheme.typography.bodyMedium,
            color = ArColors.OnSurfaceMuted,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GalleryTab.entries.forEach { tab ->
                TabChip(
                    text = tab.title,
                    selected = tab == activeTab,
                    onClick = { onTabChange(tab) },
                )
            }
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) ArColors.GradientAccent else Brush.horizontalGradient(listOf(ArColors.SurfaceElevated, ArColors.SurfaceElevated))
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) ArColors.OnPrimary else ArColors.OnSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ImportActionCard(onClick: () -> Unit) {
    Surface(
        color = ArColors.PrimaryViolet.copy(alpha = 0.12f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.FileUpload,
                contentDescription = null,
                tint = ArColors.PrimaryViolet,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Импорт из памяти",
                    style = MaterialTheme.typography.titleMedium,
                    color = ArColors.OnSurface,
                )
                Text(
                    text = "Выберите GLB / GLTF файл с устройства",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArColors.OnSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun DownloadBanner(progress: Float) {
    Surface(
        color = ArColors.SecondaryCyan.copy(alpha = 0.12f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudDownload,
                contentDescription = null,
                tint = ArColors.SecondaryCyan,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Загрузка модели",
                    style = MaterialTheme.typography.titleSmall,
                    color = ArColors.OnSurface,
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = ArColors.SecondaryCyan,
                    trackColor = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = ArColors.SecondaryCyan,
            )
        }
    }
}

@Composable
private fun EmptyState(tab: GalleryTab, onImportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = when (tab) {
                GalleryTab.Cloud -> Icons.Rounded.CloudDone
                GalleryTab.Imported -> Icons.Rounded.FileUpload
                GalleryTab.Library -> Icons.Rounded.CloudDone
            },
            contentDescription = null,
            tint = ArColors.OnSurfaceMuted,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (tab) {
                GalleryTab.Cloud -> "Облачных моделей пока нет"
                GalleryTab.Imported -> "Импортируйте первую модель"
                GalleryTab.Library -> "Библиотека пуста"
            },
            textAlign = TextAlign.Center,
            color = ArColors.OnSurfaceMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (tab == GalleryTab.Imported) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ArColors.PrimaryViolet,
                modifier = Modifier.clickable(onClick = onImportClick),
            ) {
                Text(
                    text = "Выбрать файл",
                    style = MaterialTheme.typography.labelLarge,
                    color = ArColors.OnPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ObjectCard(obj: ArObject, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ArColors.SurfaceElevated,
        tonalElevation = 4.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(Brush.linearGradient(listOf(ArColors.PrimaryDeep, ArColors.SecondaryDeep))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = obj.previewEmoji,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 52.sp),
                    color = Color.White,
                )
                val isCached = (obj.source as? ObjectSource.Cloud)?.cachedPath != null
                if (obj.source is ObjectSource.Cloud) {
                    Surface(
                        color = if (isCached) ArColors.Success.copy(alpha = 0.22f) else ArColors.SurfaceDark.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(bottomStart = 12.dp),
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (isCached) Icons.Rounded.CloudDone else Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                tint = if (isCached) ArColors.Success else ArColors.SecondaryCyan,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isCached) "готово" else "облако",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCached) ArColors.Success else ArColors.SecondaryCyan,
                            )
                        }
                    }
                }
                Text(
                    text = obj.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = obj.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = ArColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = obj.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ArColors.OnSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
