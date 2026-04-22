package com.arpositionset.app.domain.usecase

import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.domain.model.ObjectCategory
import com.arpositionset.app.domain.repository.ObjectRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Streams every object currently available to the user, regardless of source.
 * Presentation can filter this by tab (library / cloud / imported).
 */
class ObserveGalleryUseCase @Inject constructor(
    private val objectRepository: ObjectRepository,
) {
    operator fun invoke(): Flow<GallerySnapshot> = combine(
        objectRepository.observeGallery(),
        objectRepository.observeCloudCatalog(),
        objectRepository.observeImported(),
    ) { library, cloud, imported ->
        GallerySnapshot(
            library = library.sortedBy(ArObject::name),
            cloud = cloud.sortedBy(ArObject::name),
            imported = imported.sortedByDescending { it.id },
        )
    }

    data class GallerySnapshot(
        val library: List<ArObject>,
        val cloud: List<ArObject>,
        val imported: List<ArObject>,
    ) {
        fun byCategory(category: ObjectCategory?): List<ArObject> =
            (library + cloud + imported).let { all ->
                if (category == null) all else all.filter { it.category == category }
            }
    }
}
