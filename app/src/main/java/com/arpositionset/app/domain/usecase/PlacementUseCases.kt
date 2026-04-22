package com.arpositionset.app.domain.usecase

import com.arpositionset.app.domain.model.PendingPlacement
import com.arpositionset.app.domain.model.PlacedObject
import com.arpositionset.app.domain.model.TransformState
import com.arpositionset.app.domain.repository.PlacementRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class BeginPendingPlacementUseCase @Inject constructor(
    private val repo: PlacementRepository,
) {
    operator fun invoke(pending: PendingPlacement) = repo.beginPending(pending)
}

class ConfirmPlacementUseCase @Inject constructor(
    private val repo: PlacementRepository,
) {
    operator fun invoke(): PlacedObject? = repo.confirmPending()
}

class CancelPendingPlacementUseCase @Inject constructor(
    private val repo: PlacementRepository,
) {
    operator fun invoke() = repo.cancelPending()
}

class UpdatePlacementTransformUseCase @Inject constructor(
    private val repo: PlacementRepository,
) {
    operator fun invoke(placementId: String, transform: TransformState) =
        repo.updateTransform(placementId, transform)
}

class RemovePlacementUseCase @Inject constructor(
    private val repo: PlacementRepository,
) {
    operator fun invoke(placementId: String) = repo.remove(placementId)
}

class ClearSceneUseCase @Inject constructor(
    private val repo: PlacementRepository,
) {
    operator fun invoke() = repo.clear()
}

class SelectPlacementUseCase @Inject constructor(
    private val repo: PlacementRepository,
) {
    operator fun invoke(placementId: String?) = repo.select(placementId)
}

class ObservePlacementsUseCase @Inject constructor(
    private val repo: PlacementRepository,
) {
    fun placed(): Flow<List<PlacedObject>> = repo.observePlaced()
    fun pending(): Flow<PendingPlacement?> = repo.observePending()
    fun selection(): Flow<String?> = repo.observeSelection()
}

class ReanchorPlacementUseCase @Inject constructor(
    private val repo: PlacementRepository,
) {
    operator fun invoke(placementId: String, newAnchorHandle: String) =
        repo.reanchor(placementId, newAnchorHandle)
}
