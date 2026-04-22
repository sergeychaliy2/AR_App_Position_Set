package com.arpositionset.app.domain.usecase

import com.arpositionset.app.domain.model.PlacedObject
import com.arpositionset.app.domain.model.TransformState
import com.arpositionset.app.domain.repository.ObjectRepository
import com.arpositionset.app.domain.repository.PersistedPlacement
import com.arpositionset.app.domain.repository.PlacementRepository
import com.arpositionset.app.domain.repository.ScenePersistenceRepository
import javax.inject.Inject

/**
 * Factory-like helper exposing the scene persistence ops as a tight VM-facing
 * API. Injects whatever AR/pose helpers are needed (provided by the caller)
 * and delegates storage to the persistence repo.
 */
class PersistPlacementUseCase @Inject constructor(
    private val persistence: ScenePersistenceRepository,
) {
    suspend operator fun invoke(
        sceneId: String,
        placementId: String,
        objectId: String,
        relativePose: com.arpositionset.app.domain.model.RelativePose,
        transform: TransformState,
    ) {
        persistence.save(
            sceneId = sceneId,
            placement = PersistedPlacement(
                placementId = placementId,
                objectId = objectId,
                relativePose = relativePose,
                userTransform = transform,
            ),
        )
    }
}

class DeletePersistedPlacementUseCase @Inject constructor(
    private val persistence: ScenePersistenceRepository,
) {
    suspend operator fun invoke(placementId: String) = persistence.delete(placementId)
}

class LoadPersistedPlacementsUseCase @Inject constructor(
    private val persistence: ScenePersistenceRepository,
    private val objects: ObjectRepository,
) {
    suspend operator fun invoke(sceneId: String): List<RestoreCandidate> =
        persistence.loadPlacements(sceneId).mapNotNull { record ->
            val arObject = objects.findById(record.objectId) ?: return@mapNotNull null
            RestoreCandidate(
                placementId = record.placementId,
                arObject = arObject,
                relativePose = record.relativePose,
                transform = record.userTransform,
            )
        }

    data class RestoreCandidate(
        val placementId: String,
        val arObject: com.arpositionset.app.domain.model.ArObject,
        val relativePose: com.arpositionset.app.domain.model.RelativePose,
        val transform: TransformState,
    )
}

class AddRestoredPlacementUseCase @Inject constructor(
    private val repo: PlacementRepository,
) {
    operator fun invoke(placed: PlacedObject) = repo.addRestored(placed)
}
