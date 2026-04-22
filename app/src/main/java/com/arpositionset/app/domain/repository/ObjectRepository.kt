package com.arpositionset.app.domain.repository

import com.arpositionset.app.core.Outcome
import com.arpositionset.app.domain.model.ArObject
import kotlinx.coroutines.flow.Flow

/**
 * Aggregates objects from all available sources: bundled gallery,
 * user imports (SAF) and the remote catalog.
 */
interface ObjectRepository {
    fun observeGallery(): Flow<List<ArObject>>
    fun observeCloudCatalog(): Flow<List<ArObject>>
    fun observeImported(): Flow<List<ArObject>>

    suspend fun importFromUri(uri: String, displayName: String): Outcome<ArObject>
    fun downloadFromCloud(obj: ArObject): Flow<Outcome<ArObject>>
    suspend fun findById(id: String): ArObject?
}
