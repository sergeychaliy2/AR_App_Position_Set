package com.arpositionset.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.arpositionset.app.domain.repository.ScenePreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ScenePreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ScenePreferencesRepository {

    override fun observePlanesVisible(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[PLANES_VISIBLE] ?: true }

    override suspend fun setPlanesVisible(visible: Boolean) {
        dataStore.edit { it[PLANES_VISIBLE] = visible }
    }

    private companion object {
        val PLANES_VISIBLE = booleanPreferencesKey("planes_visible")
    }
}
