package com.arpositionset.app.presentation.ar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arpositionset.app.ar.ArSceneCoordinator
import com.arpositionset.app.ar.ArSceneEvent
import com.arpositionset.app.core.Outcome
import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.domain.model.PendingPlacement
import com.arpositionset.app.domain.model.PlacedObject
import com.arpositionset.app.domain.model.TransformState
import com.arpositionset.app.domain.usecase.BeginPendingPlacementUseCase
import com.arpositionset.app.domain.usecase.CancelPendingPlacementUseCase
import com.arpositionset.app.domain.usecase.ClearSceneUseCase
import com.arpositionset.app.domain.usecase.ConfirmPlacementUseCase
import com.arpositionset.app.domain.usecase.DownloadCloudObjectUseCase
import com.arpositionset.app.domain.usecase.ImportLocalObjectUseCase
import com.arpositionset.app.domain.usecase.ObservePlacementsUseCase
import com.arpositionset.app.domain.model.SceneBinding
import com.arpositionset.app.domain.usecase.AddRestoredPlacementUseCase
import com.arpositionset.app.domain.usecase.DeletePersistedPlacementUseCase
import com.arpositionset.app.domain.usecase.FindSceneBindingUseCase
import com.arpositionset.app.domain.usecase.LoadPersistedPlacementsUseCase
import com.arpositionset.app.domain.usecase.ObserveSceneBindingsUseCase
import com.arpositionset.app.domain.usecase.ObservePlanesVisibilityUseCase
import com.arpositionset.app.domain.usecase.PersistPlacementUseCase
import com.arpositionset.app.domain.usecase.ReanchorPlacementUseCase
import com.arpositionset.app.domain.usecase.RemovePlacementUseCase
import com.arpositionset.app.domain.usecase.SelectPlacementUseCase
import com.arpositionset.app.domain.usecase.TogglePlanesVisibilityUseCase
import com.arpositionset.app.domain.usecase.UpdatePlacementTransformUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ArViewModel @Inject constructor(
    private val coordinator: ArSceneCoordinator,
    placements: ObservePlacementsUseCase,
    observePlanes: ObservePlanesVisibilityUseCase,
    private val beginPendingUseCase: BeginPendingPlacementUseCase,
    private val confirmPendingUseCase: ConfirmPlacementUseCase,
    private val cancelPendingUseCase: CancelPendingPlacementUseCase,
    private val selectPlacementUseCase: SelectPlacementUseCase,
    private val updateTransformUseCase: UpdatePlacementTransformUseCase,
    private val removePlacementUseCase: RemovePlacementUseCase,
    private val clearSceneUseCase: ClearSceneUseCase,
    private val togglePlanesUseCase: TogglePlanesVisibilityUseCase,
    private val importLocalUseCase: ImportLocalObjectUseCase,
    private val downloadCloudUseCase: DownloadCloudObjectUseCase,
    private val reanchorUseCase: ReanchorPlacementUseCase,
    private val persistPlacementUseCase: PersistPlacementUseCase,
    private val deletePersistedUseCase: DeletePersistedPlacementUseCase,
    private val loadPersistedUseCase: LoadPersistedPlacementsUseCase,
    private val addRestoredUseCase: AddRestoredPlacementUseCase,
    private val findSceneBindingUseCase: FindSceneBindingUseCase,
    observeSceneBindings: ObserveSceneBindingsUseCase,
    private val objectRepository: com.arpositionset.app.domain.repository.ObjectRepository,
) : ViewModel() {

    val bindings: StateFlow<List<SceneBinding>> = observeSceneBindings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val ephemeral = MutableStateFlow(EphemeralState())

    private val sceneState = combine(
        coordinator.sessionState,
        observePlanes(),
        placements.placed(),
        placements.pending(),
        placements.selection(),
    ) { session, planesVisible, placed, pending, selection ->
        SceneSnapshot(session, planesVisible, placed, pending, selection)
    }

    val uiState: StateFlow<ArUiState> = combine(
        sceneState,
        ephemeral,
        coordinator.trackedMarker,
    ) { scene, eph, marker ->
        ArUiState(
            sessionState = scene.session,
            planesVisible = scene.planesVisible,
            placed = scene.placed,
            pending = scene.pending,
            selectedPlacementId = scene.selection,
            selectedGalleryObject = eph.selectedGallery,
            galleryOpen = eph.galleryOpen,
            snack = eph.snack,
            downloadProgress = eph.downloadProgress,
            moveRequestedPlacementId = eph.moveRequestedPlacementId,
            trackedMarker = marker,
            markerOrientation = eph.markerOrientation,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ArUiState(),
    )

    /** Scene IDs already restored in this session — avoids re-materialising
     *  the same objects every time a marker briefly loses + regains tracking. */
    private val restoredScenes = mutableSetOf<String>()

    /**
     * Per-placement debounce job for DB persistence. Slider drags fire up to
     * 60 change events per second — writing each to Room starves the main
     * thread and causes the slider to "glitch". We coalesce updates into a
     * single write 250 ms after the user stops touching the control.
     */
    private val persistJobs = mutableMapOf<String, Job>()

    init {
        coordinator.events.onEach(::onSceneEvent).launchIn(viewModelScope)
    }

    fun onAction(action: ArUserAction) {
        when (action) {
            ArUserAction.OpenGallery -> ephemeral.update { it.copy(galleryOpen = true) }
            ArUserAction.CloseGallery -> ephemeral.update { it.copy(galleryOpen = false) }
            is ArUserAction.ChoseObject -> ephemeral.update {
                it.copy(
                    selectedGallery = action.obj,
                    galleryOpen = false,
                    snack = "Выбран: ${action.obj.name}",
                )
            }
            is ArUserAction.ImportUri -> handleImport(action.uri, action.displayName)
            is ArUserAction.DownloadCloud -> handleDownload(action.obj)
            ArUserAction.ConfirmPlacement -> {
                val placed = confirmPendingUseCase()
                if (placed == null) {
                    ephemeral.update { it.copy(snack = "Нечего устанавливать") }
                } else {
                    ephemeral.update { it.copy(selectedGallery = null) }
                    persistIfMarkerTracked(placed)
                }
            }
            ArUserAction.CancelPlacement -> cancelPendingUseCase()
            ArUserAction.TogglePlanes -> viewModelScope.launch {
                togglePlanesUseCase(!uiState.value.planesVisible)
            }
            ArUserAction.ClearScene -> clearAllIncludingPersisted()
            ArUserAction.DeselectObject -> selectPlacementUseCase(null)
            is ArUserAction.SelectPlacement -> selectPlacementUseCase(action.placementId)
            is ArUserAction.RemovePlacement -> {
                removePlacementUseCase(action.placementId)
                viewModelScope.launch { deletePersistedUseCase(action.placementId) }
            }
            is ArUserAction.ChangeScale -> {
                changeScale(action.placementId, action.scale)
                syncPersistedTransform(action.placementId)
            }
            is ArUserAction.ChangeRotation -> {
                changeRotation(action.placementId, action.rotationY)
                syncPersistedTransform(action.placementId)
            }
            is ArUserAction.RequestMove -> ephemeral.update {
                it.copy(
                    moveRequestedPlacementId = action.placementId,
                    snack = "Коснитесь поверхности, куда переместить объект",
                )
            }
            ArUserAction.CancelMove -> ephemeral.update { it.copy(moveRequestedPlacementId = null) }
            is ArUserAction.NudgeAxis -> nudge(action.placementId, action.axis, action.signedMeters)
            is ArUserAction.SetOrientation -> ephemeral.update { it.copy(markerOrientation = action.orientation) }
            ArUserAction.SnackConsumed -> ephemeral.update { it.copy(snack = null) }
        }
    }

    private fun nudge(placementId: String, axis: Axis, signedMeters: Float) {
        val item = uiState.value.placed.firstOrNull { it.placementId == placementId } ?: return
        val (dx, dy, dz) = when (axis) {
            Axis.X -> Triple(signedMeters, 0f, 0f)
            Axis.Y -> Triple(0f, signedMeters, 0f)
            Axis.Z -> Triple(0f, 0f, signedMeters)
        }
        val newHandle = coordinator.nudgeAnchor(item.anchorHandle, dx, dy, dz) ?: run {
            ephemeral.update { it.copy(snack = "Не удалось переместить") }
            return
        }
        reanchorUseCase(placementId, newHandle)
        syncPersistedTransform(placementId)
    }

    private fun onSceneEvent(event: ArSceneEvent) {
        when (event) {
            is ArSceneEvent.SessionStateChanged -> Unit
            is ArSceneEvent.SurfaceTapped -> {
                // Move mode wins over normal placement: if user has requested
                // to move a placed object, the next surface tap re-anchors it.
                val moveId = ephemeral.value.moveRequestedPlacementId
                if (moveId != null) {
                    reanchorUseCase(moveId, event.anchorHandle)
                    ephemeral.update {
                        it.copy(
                            moveRequestedPlacementId = null,
                            snack = "Объект перемещён",
                        )
                    }
                    selectPlacementUseCase(moveId)
                    // Persist new position so a restart still puts it here.
                    syncPersistedTransform(moveId)
                    return
                }
                val obj = ephemeral.value.selectedGallery
                if (obj == null) {
                    ephemeral.update { it.copy(snack = "Сначала выберите объект в галерее") }
                } else {
                    beginPendingUseCase(PendingPlacement(event.anchorHandle, obj))
                }
            }
            is ArSceneEvent.EmptyTap -> ephemeral.update {
                it.copy(snack = "Нет обнаруженной поверхности в точке касания")
            }
            is ArSceneEvent.PlacedNodeTapped -> selectPlacementUseCase(event.placementId)
            is ArSceneEvent.LoadingFailed -> ephemeral.update {
                it.copy(snack = "Ошибка загрузки модели: ${event.message}")
            }
            is ArSceneEvent.MarkerAcquired -> restoreScene(event.markerName)
        }
    }

    private fun persistIfMarkerTracked(placed: PlacedObject) {
        val marker = coordinator.trackedMarker.value?.takeIf { it.isTracking } ?: return
        val relative = coordinator.relativeToMarker(placed.anchorHandle, marker.name) ?: return
        viewModelScope.launch {
            persistPlacementUseCase(
                sceneId = marker.name,
                placementId = placed.placementId,
                objectId = placed.sourceObject.id,
                relativePose = relative,
                transform = placed.transform,
            )
        }
    }

    /**
     * On first marker detection in the session:
     *  - Restore any persisted placements for this scene (prior user placements).
     *  - If a [SceneBinding] defines a bound object for this marker and nothing
     *    was restored, auto-place the bound object at the marker pose.
     * Subsequent detections are ignored — objects are already in the scene.
     */
    private fun restoreScene(sceneId: String) {
        if (!restoredScenes.add(sceneId)) return
        viewModelScope.launch {
            val candidates = runCatching { loadPersistedUseCase(sceneId) }
                .onFailure { Timber.e(it, "restore load failed") }
                .getOrNull().orEmpty()
            var restored = 0
            candidates.forEach { c ->
                val handle = coordinator.createAnchorAtRelative(sceneId, c.relativePose) ?: return@forEach
                addRestoredUseCase(
                    PlacedObject(
                        placementId = c.placementId,
                        sourceObject = c.arObject,
                        transform = c.transform,
                        anchorHandle = handle,
                    )
                )
                restored++
            }
            if (restored > 0) {
                ephemeral.update { it.copy(snack = "Восстановлено объектов: $restored") }
                return@launch
            }
            // No persisted placements — auto-place bound object if binding exists.
            val binding = runCatching { findSceneBindingUseCase(sceneId) }.getOrNull()
            if (binding != null) {
                val obj = objectRepository.findById(binding.objectId)
                if (obj != null) {
                    val zero = com.arpositionset.app.domain.model.RelativePose(
                        tx = 0f, ty = 0f, tz = 0f,
                        qx = 0f, qy = 0f, qz = 0f, qw = 1f,
                    )
                    val handle = coordinator.createAnchorAtRelative(sceneId, zero) ?: return@launch
                    addRestoredUseCase(
                        PlacedObject(
                            placementId = java.util.UUID.randomUUID().toString(),
                            sourceObject = obj,
                            transform = com.arpositionset.app.domain.model.TransformState.Default,
                            anchorHandle = handle,
                        )
                    )
                    ephemeral.update { it.copy(snack = "Привязка «${binding.title}» активна") }
                }
            }
        }
    }

    private fun syncPersistedTransform(placementId: String) {
        val marker = coordinator.trackedMarker.value?.takeIf { it.isTracking } ?: return
        persistJobs[placementId]?.cancel()
        persistJobs[placementId] = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            val item = uiState.value.placed.firstOrNull { it.placementId == placementId } ?: return@launch
            val relative = coordinator.relativeToMarker(item.anchorHandle, marker.name) ?: return@launch
            persistPlacementUseCase(
                sceneId = marker.name,
                placementId = item.placementId,
                objectId = item.sourceObject.id,
                relativePose = relative,
                transform = item.transform,
            )
        }
    }

    private fun clearAllIncludingPersisted() {
        val markerName = coordinator.trackedMarker.value?.name
        clearSceneUseCase()
        if (markerName != null) {
            viewModelScope.launch {
                runCatching { loadPersistedUseCase(markerName) }
                    .getOrNull()
                    ?.forEach { deletePersistedUseCase(it.placementId) }
            }
        }
        restoredScenes.clear()
    }

    private fun changeScale(placementId: String, scale: Float) {
        val current = uiState.value.placed.firstOrNull { it.placementId == placementId } ?: return
        val next = current.transform
            .copy(scale = scale)
            .withClampedScale(TransformState.MIN_SCALE, TransformState.MAX_SCALE)
        updateTransformUseCase(placementId, next)
    }

    private fun changeRotation(placementId: String, rotationY: Float) {
        val current = uiState.value.placed.firstOrNull { it.placementId == placementId } ?: return
        updateTransformUseCase(placementId, current.transform.copy(rotationYDegrees = rotationY))
    }

    private fun handleImport(uri: String, displayName: String) {
        viewModelScope.launch {
            when (val res = importLocalUseCase(uri, displayName)) {
                is Outcome.Success -> ephemeral.update {
                    it.copy(
                        selectedGallery = res.value,
                        galleryOpen = false,
                        snack = "Импортировано: ${res.value.name}",
                    )
                }
                is Outcome.Failure -> {
                    Timber.e(res.error, "import failed")
                    ephemeral.update { it.copy(snack = res.message ?: "Ошибка импорта") }
                }
                is Outcome.Progress -> Unit
            }
        }
    }

    private fun handleDownload(obj: ArObject) {
        downloadCloudUseCase(obj).onEach { outcome ->
            when (outcome) {
                is Outcome.Progress -> ephemeral.update { it.copy(downloadProgress = outcome.percent) }
                is Outcome.Success -> ephemeral.update {
                    it.copy(
                        downloadProgress = null,
                        selectedGallery = outcome.value,
                        galleryOpen = false,
                        snack = "Модель загружена: ${outcome.value.name}",
                    )
                }
                is Outcome.Failure -> {
                    Timber.e(outcome.error, "download failed")
                    ephemeral.update {
                        it.copy(
                            downloadProgress = null,
                            snack = outcome.message ?: "Ошибка загрузки",
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private data class SceneSnapshot(
        val session: com.arpositionset.app.domain.model.ArSessionState,
        val planesVisible: Boolean,
        val placed: List<PlacedObject>,
        val pending: PendingPlacement?,
        val selection: String?,
    )

    private data class EphemeralState(
        val galleryOpen: Boolean = false,
        val selectedGallery: ArObject? = null,
        val snack: String? = null,
        val downloadProgress: Float? = null,
        val moveRequestedPlacementId: String? = null,
        val markerOrientation: MarkerOrientation = MarkerOrientation.Wall,
    )

    private companion object {
        const val PERSIST_DEBOUNCE_MS = 250L
    }
}
