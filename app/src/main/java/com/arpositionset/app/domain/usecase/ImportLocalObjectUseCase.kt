package com.arpositionset.app.domain.usecase

import com.arpositionset.app.core.Outcome
import com.arpositionset.app.domain.model.ArObject
import com.arpositionset.app.domain.repository.ObjectRepository
import javax.inject.Inject

class ImportLocalObjectUseCase @Inject constructor(
    private val objectRepository: ObjectRepository,
) {
    suspend operator fun invoke(uri: String, displayName: String): Outcome<ArObject> =
        objectRepository.importFromUri(uri, displayName)
}
