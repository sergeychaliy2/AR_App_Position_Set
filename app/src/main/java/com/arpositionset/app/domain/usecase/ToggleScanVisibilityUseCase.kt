package com.arpositionset.app.domain.usecase

import com.arpositionset.app.domain.repository.ScenePreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservePlanesVisibilityUseCase @Inject constructor(
    private val prefs: ScenePreferencesRepository,
) {
    operator fun invoke(): Flow<Boolean> = prefs.observePlanesVisible()
}

class TogglePlanesVisibilityUseCase @Inject constructor(
    private val prefs: ScenePreferencesRepository,
) {
    suspend operator fun invoke(visible: Boolean) = prefs.setPlanesVisible(visible)
}
