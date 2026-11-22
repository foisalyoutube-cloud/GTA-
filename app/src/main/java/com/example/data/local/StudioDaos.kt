package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips ORDER BY createdAt DESC")
    fun observeAllClips(): Flow<List<ClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipEntity): Long

    @Update
    suspend fun updateClip(clip: ClipEntity)

    @Query("DELETE FROM clips WHERE id = :clipId")
    suspend fun deleteClipById(clipId: Long)
}

@Dao
interface LiveCreatorDao {
    @Query("SELECT * FROM live_creators ORDER BY createdAt DESC")
    fun observeAllCreators(): Flow<List<LiveCreatorEntity>>

    @Query("SELECT COUNT(*) FROM live_creators")
    suspend fun countCreators(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreator(creator: LiveCreatorEntity): Long

    @Query("UPDATE live_creators SET autoClipEnabled = :enabled WHERE id = :creatorId")
    suspend fun setAutoClipEnabled(creatorId: Long, enabled: Boolean)

    @Query("UPDATE live_creators SET isCurrentlyLive = :isLive WHERE id = :creatorId")
    suspend fun setLiveStatus(creatorId: Long, isLive: Boolean)

    @Query("UPDATE live_creators SET clipsGeneratedCount = clipsGeneratedCount + 1, lastClippedAt = :timestamp WHERE id = :creatorId")
    suspend fun incrementClippedCount(creatorId: Long, timestamp: Long)

    @Query("DELETE FROM live_creators WHERE id = :creatorId")
    suspend fun deleteCreatorById(creatorId: Long)
}

@Dao
interface LiveLogDao {
    @Query("SELECT * FROM live_logs ORDER BY timestamp DESC LIMIT 40")
    fun observeRecentLogs(): Flow<List<LiveLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LiveLogEntity): Long

    @Query("DELETE FROM live_logs")
    suspend fun clearLogs()
}
