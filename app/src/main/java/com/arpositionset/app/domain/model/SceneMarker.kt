package com.arpositionset.app.domain.model

/**
 * Reference image used as a stable world-origin for scene persistence.
 * The physical width is critical — ARCore uses it to establish real-world
 * scale. A declared width that doesn't match the printed image will skew
 * all object placements proportionally.
 */
data class SceneMarker(
    val name: String,
    val assetPath: String,
    val physicalWidthMeters: Float,
)

/** Runtime state of a marker currently seen (or recently seen) by the camera. */
data class TrackedMarker(
    val name: String,
    val isTracking: Boolean,
)

/**
 * Pose of an anchor expressed in the marker's coordinate frame. Stored in DB
 * so that on the next session — with a potentially completely different
 * ARCore world origin — we can re-materialise the anchor by composing the
 * currently observed marker pose with this relative offset.
 */
data class RelativePose(
    val tx: Float, val ty: Float, val tz: Float,
    val qx: Float, val qy: Float, val qz: Float, val qw: Float,
)
