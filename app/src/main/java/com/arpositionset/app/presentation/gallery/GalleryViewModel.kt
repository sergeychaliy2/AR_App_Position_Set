package com.arpositionset.app.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arpositionset.app.domain.usecase.ObserveGalleryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class GalleryViewModel @Inject constructor(
    observeGallery: ObserveGalleryUseCase,
) : ViewModel() {

    private val tab = MutableStateFlow(GalleryTab.Library)

    val uiState: StateFlow<GalleryUiState> = combine(observeGallery(), tab) { snap, activeTab ->
        GalleryUiState(
            tab = activeTab,
            library = snap.library,
            cloud = snap.cloud,
            imported = snap.imported,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GalleryUiState(),
    )

    fun selectTab(newTab: GalleryTab) {
        tab.update { newTab }
    }
}
