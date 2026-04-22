package com.arpositionset.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.arpositionset.app.data.local.dao.ObjectDao
import com.arpositionset.app.data.local.dao.PlacementRecordDao
import com.arpositionset.app.data.local.dao.SceneBindingDao
import com.arpositionset.app.data.local.entity.ObjectEntity
import com.arpositionset.app.data.local.entity.PlacementRecordEntity
import com.arpositionset.app.data.local.entity.SceneBindingEntity

@Database(
    entities = [ObjectEntity::class, PlacementRecordEntity::class, SceneBindingEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class ArDatabase : RoomDatabase() {
    abstract fun objectDao(): ObjectDao
    abstract fun placementRecordDao(): PlacementRecordDao
    abstract fun sceneBindingDao(): SceneBindingDao

    companion object {
        const val NAME = "ar-cache.db"
    }
}
