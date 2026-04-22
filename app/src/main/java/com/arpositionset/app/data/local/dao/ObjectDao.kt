package com.arpositionset.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arpositionset.app.data.local.entity.ObjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectDao {
    @Query("SELECT * FROM objects WHERE sourceType = :sourceType ORDER BY createdAt DESC")
    fun observeBySource(sourceType: String): Flow<List<ObjectEntity>>

    @Query("SELECT * FROM objects WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ObjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ObjectEntity)

    @Query("DELETE FROM objects WHERE id = :id")
    suspend fun delete(id: String)
}
