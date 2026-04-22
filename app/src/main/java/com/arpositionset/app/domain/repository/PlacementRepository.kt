package com.arpositionset.app.domain.repository

import com.arpositionset.app.domain.model.PendingPlacement
import com.arpositionset.app.domain.model.PlacedObject
import com.arpositionset.app.domain.model.TransformState
import kotlinx.coroutines.flow.Flow

/**
 * Tracks what the user has placed in the scene and what is pending confirmation.
 * The actual AR anchors/nodes live in the AR layer — this interface only deals
 * with domain entities so it can be unit-tested without ARCore.
 */
interface PlacementRepository {
    fun observePlaced(): Flow<List<PlacedObject>>
    fun observePending(): Flow<PendingPlacement?>
    fun observeSelection(): Flow<String?>

    fun beginPending(pending: PendingPlacement)
    fun cancelPending()
    fun confirmPending(): PlacedObject?

    fun select(placementId: String?)
    fun updateTransform(placementId: String, transform: TransformState)
    fun reanchor(placementId: String, newAnchorHandle: String)
    fun remove(placementId: String)
    fun clear()
    /** Insert a fully-formed placement skipping the pending→confirm cycle.
     *  Used by the persistence-restore flow. */
    fun addRestored(placed: PlacedObject)
}
