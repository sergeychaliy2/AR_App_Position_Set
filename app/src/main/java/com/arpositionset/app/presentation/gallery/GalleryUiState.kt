package com.arpositionset.app.presentation.gallery

import com.arpositionset.app.domain.model.ArObject

data class GalleryUiState(
    val tab: GalleryTab = GalleryTab.Library,
    val library: List<ArObject> = emptyList(),
    val cloud: List<ArObject> = emptyList(),
    val imported: List<ArObject> = emptyList(),
    val downloadingId: String? = null,
    val downloadProgress: Float = 0f,
) {
    val currentItems: List<ArObject>
        get() = when (tab) {
            GalleryTab.Library -> library
            GalleryTab.Cloud -> cloud
            GalleryTab.Imported -> imported
        }
}

enum class GalleryTab(val title: String) {
    Library("Библиотека"),
    Cloud("Облако"),
    Imported("Импорт"),
}
