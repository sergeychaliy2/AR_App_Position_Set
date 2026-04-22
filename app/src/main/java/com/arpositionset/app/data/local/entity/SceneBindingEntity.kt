package com.arpositionset.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scene_bindings")
data class SceneBindingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val markerAssetUri: String,
    val markerWidthMeters: Float,
    val objectId: String,
    val createdAt: Long,
)
