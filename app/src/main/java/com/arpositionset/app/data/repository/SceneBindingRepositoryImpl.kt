package com.arpositionset.app.data.repository

import com.arpositionset.app.data.local.dao.SceneBindingDao
import com.arpositionset.app.data.local.entity.SceneBindingEntity
import com.arpositionset.app.domain.model.SceneBinding
import com.arpositionset.app.domain.repository.SceneBindingRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SceneBindingRepositoryImpl @Inject constructor(
    private val dao: SceneBindingDao,
) : SceneBindingRepository {

    override fun observeAll(): Flow<List<SceneBinding>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<SceneBinding> =
        dao.getAll().map { it.toDomain() }

    override suspend fun findById(id: String): SceneBinding? =
        dao.findById(id)?.toDomain()

    override suspend fun save(binding: SceneBinding) {
        dao.upsert(binding.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }

    private fun SceneBindingEntity.toDomain() = SceneBinding(
        id = id, title = title,
        markerAssetUri = markerAssetUri,
        markerWidthMeters = markerWidthMeters,
        objectId = objectId, createdAt = createdAt,
    )

    private fun SceneBinding.toEntity() = SceneBindingEntity(
        id = id, title = title,
        markerAssetUri = markerAssetUri,
        markerWidthMeters = markerWidthMeters,
        objectId = objectId, createdAt = createdAt,
    )
}
