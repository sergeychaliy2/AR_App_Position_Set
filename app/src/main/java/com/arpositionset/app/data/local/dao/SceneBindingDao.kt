package com.arpositionset.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arpositionset.app.data.local.entity.SceneBindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SceneBindingDao {
    @Query("SELECT * FROM scene_bindings ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<SceneBindingEntity>>

    @Query("SELECT * FROM scene_bindings")
    suspend fun getAll(): List<SceneBindingEntity>

    @Query("SELECT * FROM scene_bindings WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SceneBindingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SceneBindingEntity)

    @Query("DELETE FROM scene_bindings WHERE id = :id")
    suspend fun delete(id: String)
}
