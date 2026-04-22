package com.arpositionset.app.data.repository

import com.arpositionset.app.core.IoDispatcher
import com.arpositionset.app.core.Outcome
import com.arpositionset.app.core.map
import com.arpositionset.app.data.local.GalleryCatalog
import com.arpositionset.app.data.local.dao.ObjectDao
import com.arpositionset.app.data.mapper.toDomain
import com.arpositionset.app.data.mapper.toEntity
import com.arpositionset.app.data.remote.ModelDownloader
import com.arpositionset.app.data.remote.SafImporter
import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.domain.model.ObjectCategory
import com.arpositionset.app.domain.model.ObjectSource
import com.arpositionset.app.domain.repository.ObjectRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
class ObjectRepositoryImpl @Inject constructor(
    private val catalog: GalleryCatalog,
    private val dao: ObjectDao,
    private val downloader: ModelDownloader,
    private val safImporter: SafImporter,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ObjectRepository {

    override fun observeGallery(): Flow<List<ArObject>> =
        flowOf(catalog.builtInObjects)

    /**
     * Cloud list = static catalog merged with DAO-cached versions (so users see
     * the "downloaded" badge persistently). Recombines whenever cache changes.
     */
    override fun observeCloudCatalog(): Flow<List<ArObject>> =
        combine(
            flowOf(catalog.cloudCatalog),
            dao.observeBySource("cloud"),
        ) { remote, cached ->
            val cachedById = cached.associateBy { it.id }
            remote.map { original -> cachedById[original.id]?.toDomain() ?: original }
        }.flowOn(io)

    override fun observeImported(): Flow<List<ArObject>> =
        dao.observeBySource("imported").map { list -> list.map { it.toDomain() } }

    override suspend fun importFromUri(uri: String, displayName: String): Outcome<ArObject> =
        runCatching {
            val imported = safImporter.import(android.net.Uri.parse(uri))
            val obj = ArObject(
                id = "imported:${System.currentTimeMillis()}",
                name = displayName.substringBeforeLast('.'),
                description = "Локальный импорт",
                source = ObjectSource.Imported(imported.path),
                modelUri = imported.path,
                previewUri = "drawable://ic_preview_imported",
                category = ObjectCategory.Imported,
                defaultScale = 1f,
                sizeBytes = imported.sizeBytes,
            )
            dao.upsert(obj.toEntity())
            obj
        }.fold(
            onSuccess = { Outcome.Success(it) },
            onFailure = {
                Timber.e(it, "Import failed")
                Outcome.Failure(it, "Не удалось импортировать файл")
            },
        )

    override fun downloadFromCloud(obj: ArObject): Flow<Outcome<ArObject>> {
        val remoteUrl = (obj.source as? ObjectSource.Cloud)?.remoteUrl
            ?: return flowOf(Outcome.Failure(IllegalStateException("Not a cloud object")))
        val name = remoteUrl.substringAfterLast('/').ifBlank { "${obj.id}.glb" }
        return downloader.download(remoteUrl, name).map { outcome ->
            outcome.map { path ->
                val cached = obj.copy(
                    source = ObjectSource.Cloud(remoteUrl = remoteUrl, cachedPath = path),
                    modelUri = path,
                )
                dao.upsert(cached.toEntity())
                cached
            }
        }
    }

    override suspend fun findById(id: String): ArObject? {
        dao.findById(id)?.let { return it.toDomain() }
        return (catalog.builtInObjects + catalog.cloudCatalog).firstOrNull { it.id == id }
    }
}
