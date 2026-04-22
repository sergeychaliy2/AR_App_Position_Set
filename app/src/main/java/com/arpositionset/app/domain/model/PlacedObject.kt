package com.arpositionset.app.domain.model

/**
 * A single object the user has committed to the scene. The AR anchor itself is
 * held inside the AR layer — we only keep a stable id here so ViewModels stay
 * free of Filament/ARCore types.
 */
data class PlacedObject(
    val placementId: String,
    val sourceObject: ArObject,
    val transform: TransformState,
    val isSelected: Boolean = false,
    /**
     * Opaque handle of the ARCore anchor currently backing this placement.
     * Exposed at domain level so that "move" can change it without changing
     * placementId — the AR layer observes the change and re-parents the model.
     */
    val anchorHandle: String,
)

/**
 * Pending placement that is waiting for user confirmation.
 * Holds the hit pose as an opaque anchor handle resolved by the AR layer.
 */
data class PendingPlacement(
    val anchorHandle: String,
    val candidateObject: ArObject,
)
