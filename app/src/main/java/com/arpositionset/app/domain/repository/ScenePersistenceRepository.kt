package com.arpositionset.app.domain.repository

import com.arpositionset.app.domain.model.RelativePose
import com.arpositionset.app.domain.model.TransformState

interface ScenePersistenceRepository {
    suspend fun loadPlacements(sceneId: String): List<PersistedPlacement>
    suspend fun save(sceneId: String, placement: PersistedPlacement)
    suspend fun delete(placementId: String)
    suspend fun clearScene(sceneId: String)
}

/** Domain-level persistence record. Maps to Room entity one-to-one. */
data class PersistedPlacement(
    val placementId: String,
    val objectId: String,
    val relativePose: RelativePose,
    val userTransform: TransformState,
)
