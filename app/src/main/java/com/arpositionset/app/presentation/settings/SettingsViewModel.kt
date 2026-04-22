package com.arpositionset.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arpositionset.app.domain.model.SceneBinding
import com.arpositionset.app.domain.usecase.DeleteSceneBindingUseCase
import com.arpositionset.app.domain.usecase.FindSceneBindingUseCase
import com.arpositionset.app.domain.usecase.ObserveGalleryUseCase
import com.arpositionset.app.domain.usecase.ObserveSceneBindingsUseCase
import com.arpositionset.app.domain.usecase.SaveSceneBindingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeBindings: ObserveSceneBindingsUseCase,
    observeGallery: ObserveGalleryUseCase,
    private val saveBinding: SaveSceneBindingUseCase,
    private val deleteBinding: DeleteSceneBindingUseCase,
    private val findBinding: FindSceneBindingUseCase,
) : ViewModel() {

    private val ephemeral = MutableStateFlow(Ephemeral())
    private var editingId: String? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        observeBindings(),
        observeGallery(),
        ephemeral,
    ) { bindings, gallery, eph ->
        SettingsUiState(
            bindings = bindings,
            galleryObjects = (gallery.library + gallery.cloud + gallery.imported),
            editor = eph.editor,
            snack = eph.snack,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.AddNew -> {
                editingId = null
                ephemeral.update { it.copy(editor = BindingEditor()) }
            }
            is SettingsAction.SelectForEdit -> viewModelScope.launch {
                val existing = findBinding(action.id) ?: return@launch
                editingId = existing.id
                ephemeral.update {
                    it.copy(
                        editor = BindingEditor(
                            title = existing.title,
                            markerLocalPath = existing.markerAssetUri.removePrefix("file://"),
                            widthCm = (existing.markerWidthMeters * 100).toString(),
                            objectId = existing.objectId,
                        )
                    )
                }
            }
            SettingsAction.DismissEditor -> ephemeral.update { it.copy(editor = null) }
            is SettingsAction.EditorTitle -> updateEditor { it.copy(title = action.value) }
            is SettingsAction.EditorMarkerPath -> updateEditor { it.copy(markerLocalPath = action.path) }
            is SettingsAction.EditorWidth -> updateEditor { it.copy(widthCm = action.cm) }
            is SettingsAction.EditorObject -> updateEditor { it.copy(objectId = action.objectId) }
            SettingsAction.SaveEditor -> commitEditor()
            is SettingsAction.DeleteBinding -> viewModelScope.launch {
                deleteBinding(action.id)
                ephemeral.update { it.copy(snack = "Привязка удалена") }
            }
            SettingsAction.SnackConsumed -> ephemeral.update { it.copy(snack = null) }
        }
    }

    private fun updateEditor(block: (BindingEditor) -> BindingEditor) {
        ephemeral.update { st ->
            val e = st.editor ?: return@update st
            st.copy(editor = block(e))
        }
    }

    private fun commitEditor() {
        val e = ephemeral.value.editor ?: return
        if (!e.canSave) {
            ephemeral.update { it.copy(snack = "Заполните все поля") }
            return
        }
        val widthMeters = (e.widthCm.toFloatOrNull() ?: return) / 100f
        val binding = SceneBinding(
            id = editingId ?: UUID.randomUUID().toString(),
            title = e.title.trim(),
            markerAssetUri = "file://${e.markerLocalPath}",
            markerWidthMeters = widthMeters,
            objectId = e.objectId ?: return,
            createdAt = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            saveBinding(binding)
            ephemeral.update { it.copy(editor = null, snack = "Сохранено") }
            editingId = null
        }
    }

    private data class Ephemeral(
        val editor: BindingEditor? = null,
        val snack: String? = null,
    )
}
