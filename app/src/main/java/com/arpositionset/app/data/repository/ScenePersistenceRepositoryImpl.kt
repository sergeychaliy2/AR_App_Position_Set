package com.arpositionset.app.data.repository

import com.arpositionset.app.data.local.dao.PlacementRecordDao
import com.arpositionset.app.data.local.entity.PlacementRecordEntity
import com.arpositionset.app.domain.model.RelativePose
import com.arpositionset.app.domain.model.TransformState
import com.arpositionset.app.domain.repository.PersistedPlacement
import com.arpositionset.app.domain.repository.ScenePersistenceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScenePersistenceRepositoryImpl @Inject constructor(
    private val dao: PlacementRecordDao,
) : ScenePersistenceRepository {

    override suspend fun loadPlacements(sceneId: String): List<PersistedPlacement> =
        dao.loadByScene(sceneId).map { it.toDomain() }

    override suspend fun save(sceneId: String, placement: PersistedPlacement) {
        dao.upsert(placement.toEntity(sceneId))
    }

    override suspend fun delete(placementId: String) {
        dao.delete(placementId)
    }

    override suspend fun clearScene(sceneId: String) {
        dao.clearScene(sceneId)
    }

    private fun PlacementRecordEntity.toDomain() = PersistedPlacement(
        placementId = placementId,
        objectId = objectId,
        relativePose = RelativePose(
            tx = relTx, ty = relTy, tz = relTz,
            qx = relQx, qy = relQy, qz = relQz, qw = relQw,
        ),
        userTransform = TransformState(
            scale = scale,
            rotationYDegrees = rotationYDegrees,
        ),
    )

    private fun PersistedPlacement.toEntity(sceneId: String) = PlacementRecordEntity(
        placementId = placementId,
        sceneId = sceneId,
        objectId = objectId,
        relTx = relativePose.tx,
        relTy = relativePose.ty,
        relTz = relativePose.tz,
        relQx = relativePose.qx,
        relQy = relativePose.qy,
        relQz = relativePose.qz,
        relQw = relativePose.qw,
        scale = userTransform.scale,
        rotationYDegrees = userTransform.rotationYDegrees,
        createdAt = System.currentTimeMillis(),
    )
}
