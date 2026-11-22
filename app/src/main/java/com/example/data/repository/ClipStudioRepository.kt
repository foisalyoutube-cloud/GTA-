package com.example.data.repository

import com.example.data.local.ClipDao
import com.example.data.local.ClipEntity
import com.example.data.local.LiveCreatorDao
import com.example.data.local.LiveCreatorEntity
import com.example.data.local.LiveLogDao
import com.example.data.local.LiveLogEntity
import com.example.data.model.ClipGenerationSettings
import com.example.data.model.DetectedMoment
import com.example.data.model.FramingConfig
import com.example.data.model.ViralHookResult
import com.example.engine.ClippingEngine
import com.example.engine.ClippingProgressEvent
import kotlinx.coroutines.flow.Flow

class ClipStudioRepository(
    private val clipDao: ClipDao,
    private val liveCreatorDao: LiveCreatorDao,
    private val liveLogDao: LiveLogDao,
    private val exportHelper: ClipExportHelper,
    private val clippingEngine: ClippingEngine = ClippingEngine()
) {
    val allClips: Flow<List<ClipEntity>> = clipDao.observeAllClips()
    val allCreators: Flow<List<LiveCreatorEntity>> = liveCreatorDao.observeAllCreators()
    val recentLogs: Flow<List<LiveLogEntity>> = liveLogDao.observeRecentLogs()

    fun processStream(
        sourceUrl: String,
        isLiveMode: Boolean,
        framingConfig: FramingConfig,
        settings: ClipGenerationSettings = ClipGenerationSettings()
    ): Flow<ClippingProgressEvent> = clippingEngine.processStream(
        sourceUrl = sourceUrl,
        isLiveMode = isLiveMode,
        framingConfig = framingConfig,
        settings = settings
    )

    fun extractSimulatedMoments(
        url: String,
        isLiveMode: Boolean,
        settings: ClipGenerationSettings = ClipGenerationSettings()
    ): List<DetectedMoment> = clippingEngine.analyzeUrlInstant(url, isLiveMode, settings)

    suspend fun seedInitialCreatorsIfEmpty() {
        if (liveCreatorDao.countCreators() == 0) {
            liveCreatorDao.insertCreator(
                LiveCreatorEntity(
                    name = "ViceCityVortex",
                    platform = "YouTube Live",
                    streamUrl = "https://youtube.com/@ViceCityVortex/live",
                    autoClipEnabled = true,
                    isCurrentlyLive = true,
                    clipsGeneratedCount = 3
                )
            )
            liveCreatorDao.insertCreator(
                LiveCreatorEntity(
                    name = "LeonidaDrift",
                    platform = "Twitch",
                    streamUrl = "https://twitch.tv/leonidadrift",
                    autoClipEnabled = true,
                    isCurrentlyLive = true,
                    clipsGeneratedCount = 2
                )
            )
            liveCreatorDao.insertCreator(
                LiveCreatorEntity(
                    name = "NeonHeistTV",
                    platform = "Kick",
                    streamUrl = "https://kick.com/neonheisttv",
                    autoClipEnabled = false,
                    isCurrentlyLive = false,
                    clipsGeneratedCount = 1
                )
            )
            liveLogDao.insertLog(
                LiveLogEntity(
                    creatorName = "ViceCityVortex",
                    eventMessage = "Watchlist initialized • Ready for auto-clip monitoring",
                    clipTitle = null,
                    status = "READY"
                )
            )
        }
    }

    suspend fun saveClip(
        moment: DetectedMoment,
        sourceUrl: String,
        framing: FramingConfig,
        hookResult: ViralHookResult?,
        isAutoClipped: Boolean = false
    ): Long {
        val hashtags = (hookResult?.hashtags ?: listOf("#GTAVI", "#Shorts", "#ViceCity"))
            .joinToString(", ")
        val entity = ClipEntity(
            title = moment.title,
            creatorName = moment.creatorName,
            sourceUrl = sourceUrl,
            startTime = moment.startTime,
            endTime = moment.endTime,
            durationSec = moment.durationSec,
            hypeTag = moment.hypeTag,
            energyScore = moment.energyScore,
            viralTitle = hookResult?.viralTitle ?: moment.title,
            viralHook = hookResult?.openingHook
                ?: "Unbelievable ${moment.hypeTag.lowercase()} caught live on stream!",
            hashtagsCsv = hashtags,
            layoutModeName = framing.layoutMode.name,
            splitRatio = framing.splitRatio,
            facecamZoom = framing.facecamZoom,
            facecamPanX = framing.facecamPanX,
            facecamPanY = framing.facecamPanY,
            facecamBoxWidth = framing.facecamBoxWidth,
            facecamBoxHeight = framing.facecamBoxHeight,
            gameplayZoom = framing.gameplayZoom,
            gameplayPanX = framing.gameplayPanX,
            gameplayPanY = framing.gameplayPanY,
            gameplayBoxWidth = framing.gameplayBoxWidth,
            gameplayBoxHeight = framing.gameplayBoxHeight,
            showHookCaption = framing.showHookCaption,
            sceneHueSeed = moment.sceneHueSeed,
            isAutoClipped = isAutoClipped
        )
        return clipDao.insertClip(entity)
    }

    suspend fun updateClipHook(clip: ClipEntity, hookResult: ViralHookResult) {
        clipDao.updateClip(
            clip.copy(
                viralTitle = hookResult.viralTitle,
                viralHook = hookResult.openingHook,
                hashtagsCsv = hookResult.hashtags.joinToString(", ")
            )
        )
    }

    suspend fun deleteClip(clipId: Long) {
        clipDao.deleteClipById(clipId)
    }

    suspend fun addCreator(name: String, platform: String, streamUrl: String) {
        liveCreatorDao.insertCreator(
            LiveCreatorEntity(
                name = name.trim(),
                platform = platform,
                streamUrl = streamUrl.trim(),
                autoClipEnabled = true,
                isCurrentlyLive = true
            )
        )
        liveLogDao.insertLog(
            LiveLogEntity(
                creatorName = name.trim(),
                eventMessage = "Added creator to $platform auto-clip watchlist",
                status = "ADDED"
            )
        )
    }

    suspend fun toggleAutoClip(creatorId: Long, enabled: Boolean, creatorName: String) {
        liveCreatorDao.setAutoClipEnabled(creatorId, enabled)
        liveLogDao.insertLog(
            LiveLogEntity(
                creatorName = creatorName,
                eventMessage = if (enabled) "Auto-Clip on Live enabled" else "Auto-Clip on Live paused",
                status = if (enabled) "ACTIVE" else "PAUSED"
            )
        )
    }

    suspend fun deleteCreator(creatorId: Long) {
        liveCreatorDao.deleteCreatorById(creatorId)
    }

    suspend fun recordAutoClippedHighlight(
        creator: LiveCreatorEntity,
        moment: DetectedMoment,
        hook: ViralHookResult
    ): Long {
        val clipId = saveClip(
            moment = moment,
            sourceUrl = creator.streamUrl,
            framing = FramingConfig(),
            hookResult = hook,
            isAutoClipped = true
        )
        val now = System.currentTimeMillis()
        liveCreatorDao.incrementClippedCount(creator.id, now)
        liveLogDao.insertLog(
            LiveLogEntity(
                creatorName = creator.name,
                eventMessage = "Audio peak (${moment.energyScore}/100) auto-clipped to 9:16",
                clipTitle = hook.viralTitle,
                status = "CLIPPED",
                timestamp = now
            )
        )
        return clipId
    }

    suspend fun logMonitoringState(message: String, status: String) {
        liveLogDao.insertLog(
            LiveLogEntity(
                creatorName = "Studio Engine",
                eventMessage = message,
                status = status
            )
        )
    }

    suspend fun exportClip(clip: ClipEntity): Result<ClipExportResult> =
        exportHelper.exportVerticalShort(clip)
}
