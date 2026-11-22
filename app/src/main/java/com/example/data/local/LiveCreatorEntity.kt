package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "live_creators")
data class LiveCreatorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val platform: String,
    val streamUrl: String,
    val autoClipEnabled: Boolean = true,
    val isCurrentlyLive: Boolean = true,
    val clipsGeneratedCount: Int = 0,
    val lastClippedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
