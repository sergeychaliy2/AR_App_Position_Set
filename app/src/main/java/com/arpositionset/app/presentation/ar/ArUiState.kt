package com.arpositionset.app.presentation.ar

import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.domain.model.ArSessionState
import com.arpositionset.app.domain.model.PendingPlacement
import com.arpositionset.app.domain.model.PlacedObject
import com.arpositionset.app.domain.model.TrackedMarker

data class ArUiState(
    val sessionState: ArSessionState = ArSessionState.Initializing,
    val planesVisible: Boolean = true,
    val placed: List<PlacedObject> = emptyList(),
    val pending: PendingPlacement? = null,
    val selectedPlacementId: String? = null,
    val selectedGalleryObject: ArObject? = null,
    val galleryOpen: Boolean = false,
    val snack: String? = null,
    val downloadProgress: Float? = null,
    /** When non-null, the next surface tap re-anchors this placement instead of opening the placement prompt. */
    val moveRequestedPlacementId: String? = null,
    /** Current AugmentedImage being tracked (if any). Drives persistence. */
    val trackedMarker: TrackedMarker? = null,
    /** User-specified hint about where the marker is mounted. Pure UX — does
     *  not change ARCore detection, only the instruction text shown. */
    val markerOrientation: MarkerOrientation = MarkerOrientation.Wall,
) {
    val showPlacementPrompt: Boolean get() = pending != null && moveRequestedPlacementId == null
    val isMoving: Boolean get() = moveRequestedPlacementId != null
    val selected: PlacedObject? get() = placed.firstOrNull { it.placementId == selectedPlacementId }
    val showScannerOverlay: Boolean get() = sessionState == ArSessionState.Ready && trackedMarker?.isTracking != true
}

enum class MarkerOrientation(val hint: String) {
    Wall("Маркер на стене — держите камеру перпендикулярно"),
    Floor("Маркер на полу — смотрите сверху под углом 30-60°"),
}

sealed interface ArUserAction {
    data object OpenGallery : ArUserAction
    data object CloseGallery : ArUserAction
    data class ChoseObject(val obj: ArObject) : ArUserAction
    data class ImportUri(val uri: String, val displayName: String) : ArUserAction
    data class DownloadCloud(val obj: ArObject) : ArUserAction
    data object ConfirmPlacement : ArUserAction
    data object CancelPlacement : ArUserAction
    data object TogglePlanes : ArUserAction
    data object ClearScene : ArUserAction
    data object DeselectObject : ArUserAction
    data class SelectPlacement(val placementId: String) : ArUserAction
    data class RemovePlacement(val placementId: String) : ArUserAction
    data class ChangeScale(val placementId: String, val scale: Float) : ArUserAction
    data class ChangeRotation(val placementId: String, val rotationY: Float) : ArUserAction
    data class RequestMove(val placementId: String) : ArUserAction
    data object CancelMove : ArUserAction
    data class NudgeAxis(val placementId: String, val axis: Axis, val signedMeters: Float) : ArUserAction
    data class SetOrientation(val orientation: MarkerOrientation) : ArUserAction
    data object SnackConsumed : ArUserAction
}

enum class Axis { X, Y, Z }
