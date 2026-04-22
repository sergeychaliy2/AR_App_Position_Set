package com.arpositionset.app.presentation.settings

import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.domain.model.SceneBinding

data class SettingsUiState(
    val bindings: List<SceneBinding> = emptyList(),
    val galleryObjects: List<ArObject> = emptyList(),
    val editor: BindingEditor? = null,
    val snack: String? = null,
)

data class BindingEditor(
    val title: String = "",
    val markerLocalPath: String? = null,
    val widthCm: String = "10",
    val objectId: String? = null,
) {
    val canSave: Boolean
        get() = title.isNotBlank() &&
                markerLocalPath != null &&
                widthCm.toFloatOrNull()?.takeIf { it > 0 } != null &&
                objectId != null
}

sealed interface SettingsAction {
    data object AddNew : SettingsAction
    data class SelectForEdit(val id: String) : SettingsAction
    data object DismissEditor : SettingsAction
    data class EditorTitle(val value: String) : SettingsAction
    data class EditorMarkerPath(val path: String) : SettingsAction
    data class EditorWidth(val cm: String) : SettingsAction
    data class EditorObject(val objectId: String) : SettingsAction
    data object SaveEditor : SettingsAction
    data class DeleteBinding(val id: String) : SettingsAction
    data object SnackConsumed : SettingsAction
}
