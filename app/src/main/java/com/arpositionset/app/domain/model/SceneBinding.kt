package com.arpositionset.app.domain.model

/**
 * A user-defined pairing between a reference image (marker) and a 3D object.
 * When ARCore recognises [markerAssetUri] with its declared [markerWidthMeters],
 * the [objectId] is materialised at the marker's pose.
 *
 * Real-world proportions live here — [markerWidthMeters] MUST match the
 * physical print/display width of the marker, or the whole scale of the scene
 * is off.
 */
data class SceneBinding(
    val id: String,
    val title: String,
    val markerAssetUri: String,
    val markerWidthMeters: Float,
    val objectId: String,
    val createdAt: Long,
)
