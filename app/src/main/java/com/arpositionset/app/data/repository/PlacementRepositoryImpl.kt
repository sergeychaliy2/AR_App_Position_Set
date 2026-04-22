package com.arpositionset.app.data.repository

import com.arpositionset.app.domain.model.PendingPlacement
import com.arpositionset.app.domain.model.PlacedObject
import com.arpositionset.app.domain.model.TransformState
import com.arpositionset.app.domain.repository.PlacementRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory placement bookkeeping. Kept as a Singleton so the scene can survive
 * short navigation without tearing down the ViewModel state. AR-side nodes are
 * managed separately in [com.arpositionset.app.ar.ArSceneController].
 */
@Singleton
class PlacementRepositoryImpl @Inject constructor() : PlacementRepository {

    private val _placed = MutableStateFlow<List<PlacedObject>>(emptyList())
    private val _pending = MutableStateFlow<PendingPlacement?>(null)
    private val _selected = MutableStateFlow<String?>(null)

    override fun observePlaced(): StateFlow<List<PlacedObject>> = _placed.asStateFlow()
    override fun observePending(): StateFlow<PendingPlacement?> = _pending.asStateFlow()
    override fun observeSelection(): StateFlow<String?> = _selected.asStateFlow()

    override fun beginPending(pending: PendingPlacement) {
        _pending.value = pending
    }

    override fun cancelPending() {
        _pending.value = null
    }

    override fun confirmPending(): PlacedObject? {
        val pending = _pending.value ?: return null
        val placed = PlacedObject(
            placementId = UUID.randomUUID().toString(),
            sourceObject = pending.candidateObject,
            transform = TransformState.Default,
            anchorHandle = pending.anchorHandle,
        )
        _placed.update { it + placed }
        _pending.value = null
        _selected.value = placed.placementId
        return placed
    }

    override fun reanchor(placementId: String, newAnchorHandle: String) {
        _placed.update { list ->
            list.map { item ->
                if (item.placementId == placementId) item.copy(anchorHandle = newAnchorHandle) else item
            }
        }
    }

    override fun addRestored(placed: PlacedObject) {
        _placed.update { list ->
            if (list.any { it.placementId == placed.placementId }) list
            else list + placed
        }
    }

    override fun select(placementId: String?) {
        _selected.value = placementId
        _placed.update { list ->
            list.map { it.copy(isSelected = it.placementId == placementId) }
        }
    }

    override fun updateTransform(placementId: String, transform: TransformState) {
        _placed.update { list ->
            list.map { item ->
                if (item.placementId == placementId) item.copy(transform = transform) else item
            }
        }
    }

    override fun remove(placementId: String) {
        _placed.update { list -> list.filterNot { it.placementId == placementId } }
        if (_selected.value == placementId) _selected.value = null
    }

    override fun clear() {
        _placed.value = emptyList()
        _pending.value = null
        _selected.value = null
    }
}
