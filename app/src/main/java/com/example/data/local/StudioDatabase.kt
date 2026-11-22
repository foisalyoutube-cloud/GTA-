package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ClipEntity::class, LiveCreatorEntity::class, LiveLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao
    abstract fun liveCreatorDao(): LiveCreatorDao
    abstract fun liveLogDao(): LiveLogDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        fun getInstance(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "gta_vi_clipping_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
