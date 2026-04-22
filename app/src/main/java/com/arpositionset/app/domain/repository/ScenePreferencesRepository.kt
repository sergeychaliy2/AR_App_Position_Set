package com.arpositionset.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface ScenePreferencesRepository {
    fun observePlanesVisible(): Flow<Boolean>
    suspend fun setPlanesVisible(visible: Boolean)
}
