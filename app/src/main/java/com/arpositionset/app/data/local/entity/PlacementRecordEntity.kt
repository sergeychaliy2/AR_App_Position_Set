package com.arpositionset.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent record of an object placement, anchored relative to a scene
 * marker. On the next app launch the row is re-materialised into a
 * [com.arpositionset.app.domain.model.PlacedObject] once the matching
 * AugmentedImage is recognised by the camera.
 */
@Entity(tableName = "placement_records")
data class PlacementRecordEntity(
    @PrimaryKey val placementId: String,
    val sceneId: String,
    val objectId: String,
    val relTx: Float, val relTy: Float, val relTz: Float,
    val relQx: Float, val relQy: Float, val relQz: Float, val relQw: Float,
    val scale: Float,
    val rotationYDegrees: Float,
    val createdAt: Long,
)
