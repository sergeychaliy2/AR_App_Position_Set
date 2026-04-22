package com.arpositionset.app.domain.usecase

import com.arpositionset.app.domain.model.SceneBinding
import com.arpositionset.app.domain.repository.SceneBindingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSceneBindingsUseCase @Inject constructor(
    private val repo: SceneBindingRepository,
) {
    operator fun invoke(): Flow<List<SceneBinding>> = repo.observeAll()
}

class GetAllSceneBindingsUseCase @Inject constructor(
    private val repo: SceneBindingRepository,
) {
    suspend operator fun invoke(): List<SceneBinding> = repo.getAll()
}

class FindSceneBindingUseCase @Inject constructor(
    private val repo: SceneBindingRepository,
) {
    suspend operator fun invoke(id: String): SceneBinding? = repo.findById(id)
}

class SaveSceneBindingUseCase @Inject constructor(
    private val repo: SceneBindingRepository,
) {
    suspend operator fun invoke(binding: SceneBinding) = repo.save(binding)
}

class DeleteSceneBindingUseCase @Inject constructor(
    private val repo: SceneBindingRepository,
) {
    suspend operator fun invoke(id: String) = repo.delete(id)
}
