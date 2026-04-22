package com.arpositionset.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "objects")
data class ObjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val sourceType: String,
    val remoteUrl: String?,
    val cachedPath: String?,
    val localPath: String?,
    val previewUri: String?,
    val category: String,
    val defaultScale: Float,
    val sizeBytes: Long?,
    val createdAt: Long,
)
