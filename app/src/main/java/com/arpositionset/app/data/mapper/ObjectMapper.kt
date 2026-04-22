package com.arpositionset.app.data.mapper

import com.arpositionset.app.data.local.entity.ObjectEntity
import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.domain.model.ObjectCategory
import com.arpositionset.app.domain.model.ObjectSource

private const val SOURCE_BUILTIN = "builtin"
private const val SOURCE_IMPORTED = "imported"
private const val SOURCE_CLOUD = "cloud"

fun ObjectEntity.toDomain(): ArObject {
    val source: ObjectSource = when (sourceType) {
        SOURCE_BUILTIN -> ObjectSource.BuiltIn
        SOURCE_IMPORTED -> ObjectSource.Imported(localPath.orEmpty())
        SOURCE_CLOUD -> ObjectSource.Cloud(remoteUrl.orEmpty(), cachedPath)
        else -> ObjectSource.BuiltIn
    }
    val modelUri = when (source) {
        is ObjectSource.BuiltIn -> localPath.orEmpty()
        is ObjectSource.Imported -> source.localPath
        is ObjectSource.Cloud -> source.cachedPath ?: source.remoteUrl
    }
    return ArObject(
        id = id,
        name = name,
        description = description,
        source = source,
        modelUri = modelUri,
        previewUri = previewUri,
        category = ObjectCategory.fromStorageKey(category),
        defaultScale = defaultScale,
        sizeBytes = sizeBytes,
    )
}

fun ArObject.toEntity(createdAt: Long = System.currentTimeMillis()): ObjectEntity {
    val (sourceType, remote, cached, local) = when (val s = source) {
        is ObjectSource.BuiltIn -> Quad(SOURCE_BUILTIN, null, null, modelUri)
        is ObjectSource.Imported -> Quad(SOURCE_IMPORTED, null, null, s.localPath)
        is ObjectSource.Cloud -> Quad(SOURCE_CLOUD, s.remoteUrl, s.cachedPath, null)
    }
    return ObjectEntity(
        id = id,
        name = name,
        description = description,
        sourceType = sourceType,
        remoteUrl = remote,
        cachedPath = cached,
        localPath = local,
        previewUri = previewUri,
        category = category.name,
        defaultScale = defaultScale,
        sizeBytes = sizeBytes,
        createdAt = createdAt,
    )
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
