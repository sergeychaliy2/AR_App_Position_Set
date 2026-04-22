package com.arpositionset.app.domain.usecase

import com.arpositionset.app.core.Outcome
import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.domain.repository.ObjectRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DownloadCloudObjectUseCase @Inject constructor(
    private val objectRepository: ObjectRepository,
) {
    operator fun invoke(obj: ArObject): Flow<Outcome<ArObject>> =
        objectRepository.downloadFromCloud(obj)
}
