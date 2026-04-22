package com.arpositionset.app.presentation.ar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arpositionset.app.ar.ArSceneHost
import com.arpositionset.app.ar.ArSceneCoordinator
import com.arpositionset.app.presentation.ar.components.ArBottomBar
import com.arpositionset.app.presentation.ar.components.ArTopBar
import com.arpositionset.app.presentation.ar.components.MarkerScannerOverlay
import com.arpositionset.app.presentation.ar.components.PlacementPrompt
import com.arpositionset.app.presentation.ar.components.SessionStatusChip
import com.arpositionset.app.presentation.ar.components.TransformSheet
import com.arpositionset.app.presentation.gallery.GalleryBottomSheet
import dagger.hilt.android.EntryPointAccessors

@Composable
fun ArScreen(
    onOpenSettings: () -> Unit,
    viewModel: ArViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bindings by viewModel.bindings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snack) {
        val msg = state.snack ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
        viewModel.onAction(ArUserAction.SnackConsumed)
    }

    val coordinator = rememberCoordinator()

    Scaffold(
        containerColor = Color.Black,
        contentColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ArTopBar(
                planesVisible = state.planesVisible,
                onTogglePlanes = { viewModel.onAction(ArUserAction.TogglePlanes) },
                onClear = { viewModel.onAction(ArUserAction.ClearScene) },
                onSettings = onOpenSettings,
            )
        },
        bottomBar = {
            ArBottomBar(
                selectedObject = state.selectedGalleryObject,
                onOpenGallery = { viewModel.onAction(ArUserAction.OpenGallery) },
            )
        },
    ) { paddings ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            ArSceneHost(
                coordinator = coordinator,
                placed = state.placed,
                pending = state.pending,
                planesVisible = state.planesVisible,
                selectedPlacementId = state.selectedPlacementId,
                bindings = bindings,
                modifier = Modifier.fillMaxSize(),
            )

            SessionStatusChip(
                state = state.sessionState,
                downloadProgress = state.downloadProgress,
                trackedMarker = state.trackedMarker,
                modifier = Modifier.padding(paddings),
            )

            MarkerScannerOverlay(
                visible = state.showScannerOverlay,
                orientationHint = state.markerOrientation.hint,
                modifier = Modifier.padding(paddings),
            )

            PlacementPrompt(
                visible = state.showPlacementPrompt,
                candidateName = state.pending?.candidateObject?.name.orEmpty(),
                onConfirm = { viewModel.onAction(ArUserAction.ConfirmPlacement) },
                onCancel = { viewModel.onAction(ArUserAction.CancelPlacement) },
                modifier = Modifier.padding(paddings),
            )

            TransformSheet(
                placed = state.selected.takeUnless { state.isMoving },
                onScaleChange = { id, s -> viewModel.onAction(ArUserAction.ChangeScale(id, s)) },
                onRotationChange = { id, r -> viewModel.onAction(ArUserAction.ChangeRotation(id, r)) },
                onRemove = { id -> viewModel.onAction(ArUserAction.RemovePlacement(id)) },
                onMove = { id -> viewModel.onAction(ArUserAction.RequestMove(id)) },
                onNudge = { id, axis, meters -> viewModel.onAction(ArUserAction.NudgeAxis(id, axis, meters)) },
                onDismiss = { viewModel.onAction(ArUserAction.DeselectObject) },
                modifier = Modifier.padding(paddings),
            )

            if (state.galleryOpen) {
                GalleryBottomSheet(
                    onDismiss = { viewModel.onAction(ArUserAction.CloseGallery) },
                    onSelect = { viewModel.onAction(ArUserAction.ChoseObject(it)) },
                    onDownload = { viewModel.onAction(ArUserAction.DownloadCloud(it)) },
                    onImport = { uri, name -> viewModel.onAction(ArUserAction.ImportUri(uri, name)) },
                    downloadProgress = state.downloadProgress,
                )
            }
        }
    }
}

@Composable
private fun rememberCoordinator(): ArSceneCoordinator {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return remember {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            CoordinatorEntryPoint::class.java,
        ).coordinator()
    }
}

/** Hilt entry point so the scene Composable can reach the singleton coordinator. */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface CoordinatorEntryPoint {
    fun coordinator(): ArSceneCoordinator
}
