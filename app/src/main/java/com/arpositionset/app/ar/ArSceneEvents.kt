package com.arpositionset.app.ar

import com.arpositionset.app.domain.model.ArSessionState

/**
 * Events emitted from the AR layer back to the presentation layer.
 * Keeps presentation free of ARCore / Filament types.
 */
sealed interface ArSceneEvent {
    data class SessionStateChanged(val state: ArSessionState) : ArSceneEvent
    data class SurfaceTapped(val anchorHandle: String) : ArSceneEvent
    data class EmptyTap(val x: Float, val y: Float) : ArSceneEvent
    data class PlacedNodeTapped(val placementId: String) : ArSceneEvent
    data class LoadingFailed(val message: String) : ArSceneEvent
    /** Fires once when an AugmentedImage is first recognised (transition → tracking). */
    data class MarkerAcquired(val markerName: String) : ArSceneEvent
}
