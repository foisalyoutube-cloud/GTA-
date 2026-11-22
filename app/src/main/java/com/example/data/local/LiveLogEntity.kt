package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "live_logs")
data class LiveLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val creatorName: String,
    val eventMessage: String,
    val clipTitle: String? = null,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)
