package com.arpositionset.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arpositionset.app.data.local.entity.PlacementRecordEntity

@Dao
interface PlacementRecordDao {
    @Query("SELECT * FROM placement_records WHERE sceneId = :sceneId ORDER BY createdAt ASC")
    suspend fun loadByScene(sceneId: String): List<PlacementRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PlacementRecordEntity)

    @Query("DELETE FROM placement_records WHERE placementId = :placementId")
    suspend fun delete(placementId: String)

    @Query("DELETE FROM placement_records WHERE sceneId = :sceneId")
    suspend fun clearScene(sceneId: String)
}
