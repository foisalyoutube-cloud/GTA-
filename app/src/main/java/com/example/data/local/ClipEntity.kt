package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FramingConfig
import com.example.data.model.FramingLayer
import com.example.data.model.VerticalLayoutMode

@Entity(tableName = "clips")
data class ClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val creatorName: String,
    val sourceUrl: String,
    val startTime: String,
    val endTime: String,
    val durationSec: Int,
    val hypeTag: String,
    val energyScore: Int,
    val viralTitle: String,
    val viralHook: String,
    val hashtagsCsv: String,
    val layoutModeName: String,
    val splitRatio: Float,
    val facecamZoom: Float,
    val facecamPanX: Float,
    val facecamPanY: Float,
    val facecamBoxWidth: Float,
    val facecamBoxHeight: Float,
    val gameplayZoom: Float,
    val gameplayPanX: Float,
    val gameplayPanY: Float,
    val gameplayBoxWidth: Float,
    val gameplayBoxHeight: Float,
    val showHookCaption: Boolean,
    val sceneHueSeed: Int,
    val isAutoClipped: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toFramingConfig(): FramingConfig = FramingConfig(
        activeLayer = FramingLayer.FACECAM,
        layoutMode = runCatching { VerticalLayoutMode.valueOf(layoutModeName) }
            .getOrDefault(VerticalLayoutMode.SPLIT_STACK),
        splitRatio = splitRatio,
        facecamZoom = facecamZoom,
        facecamPanX = facecamPanX,
        facecamPanY = facecamPanY,
        facecamBoxWidth = facecamBoxWidth,
        facecamBoxHeight = facecamBoxHeight,
        gameplayZoom = gameplayZoom,
        gameplayPanX = gameplayPanX,
        gameplayPanY = gameplayPanY,
        gameplayBoxWidth = gameplayBoxWidth,
        gameplayBoxHeight = gameplayBoxHeight,
        showHookCaption = showHookCaption
    )

    fun hashtagList(): List<String> =
        hashtagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
