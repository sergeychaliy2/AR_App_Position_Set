package com.arpositionset.app.domain.repository

import com.arpositionset.app.domain.model.SceneBinding
import kotlinx.coroutines.flow.Flow

interface SceneBindingRepository {
    fun observeAll(): Flow<List<SceneBinding>>
    suspend fun getAll(): List<SceneBinding>
    suspend fun findById(id: String): SceneBinding?
    suspend fun save(binding: SceneBinding)
    suspend fun delete(id: String)
}
