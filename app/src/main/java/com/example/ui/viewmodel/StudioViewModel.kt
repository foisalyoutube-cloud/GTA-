package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClipEntity
import com.example.data.local.LiveCreatorEntity
import com.example.data.local.LiveLogEntity
import com.example.data.local.StudioDatabase
import com.example.data.model.CaptionStyle
import com.example.data.model.ClipGenerationSettings
import com.example.data.model.DetectedMoment
import com.example.data.model.FacecamPosition
import com.example.data.model.FramingConfig
import com.example.data.model.FramingLayer
import com.example.data.model.HookUiState
import com.example.data.model.PipelineState
import com.example.data.model.ProcessingMode
import com.example.data.model.SetupChecklistItem
import com.example.data.model.SetupToolProfile
import com.example.data.model.StudioTab
import com.example.data.model.TargetOsPlatform
import com.example.data.model.VerticalLayoutMode
import com.example.data.model.ViralHookResult
import com.example.data.repository.AiHookRepository
import com.example.data.repository.ClipExportHelper
import com.example.data.repository.ClipExportResult
import com.example.data.repository.ClipStudioRepository
import com.example.engine.ClippingProgressEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface StudioEvent {
    data class ShowMessage(val message: String, val actionLabel: String? = null, val targetTab: StudioTab? = null) : StudioEvent
    data class ClipExported(val result: ClipExportResult) : StudioEvent
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StudioDatabase.getInstance(application)
    private val repository = ClipStudioRepository(
        clipDao = database.clipDao(),
        liveCreatorDao = database.liveCreatorDao(),
        liveLogDao = database.liveLogDao(),
        exportHelper = ClipExportHelper(application)
    )
    private val aiHookRepository = AiHookRepository()

    private val _selectedTab = MutableStateFlow(StudioTab.CLIP_STUDIO)
    val selectedTab: StateFlow<StudioTab> = _selectedTab.asStateFlow()

    private val _pipelineState = MutableStateFlow(PipelineState())
    val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

    private val _detectedMoments = MutableStateFlow<List<DetectedMoment>>(emptyList())
    val detectedMoments: StateFlow<List<DetectedMoment>> = _detectedMoments.asStateFlow()

    private val _selectedMoment = MutableStateFlow<DetectedMoment?>(null)
    val selectedMoment: StateFlow<DetectedMoment?> = _selectedMoment.asStateFlow()

    private val _isPreviewOpen = MutableStateFlow(true)
    val isPreviewOpen: StateFlow<Boolean> = _isPreviewOpen.asStateFlow()

    private val _framingConfig = MutableStateFlow(FramingConfig())
    val framingConfig: StateFlow<FramingConfig> = _framingConfig.asStateFlow()

    private val _hookUiState = MutableStateFlow<HookUiState>(HookUiState.Idle)
    val hookUiState: StateFlow<HookUiState> = _hookUiState.asStateFlow()

    private val _isLiveMonitorActive = MutableStateFlow(false)
    val isLiveMonitorActive: StateFlow<Boolean> = _isLiveMonitorActive.asStateFlow()

    private val _activePlaybackClip = MutableStateFlow<ClipEntity?>(null)
    val activePlaybackClip: StateFlow<ClipEntity?> = _activePlaybackClip.asStateFlow()

    private val _isPlaybackPlaying = MutableStateFlow(true)
    val isPlaybackPlaying: StateFlow<Boolean> = _isPlaybackPlaying.asStateFlow()

    private val _repoUrl = MutableStateFlow("https://github.com/haziqahmedqasmi/PeakCut")
    val repoUrl: StateFlow<String> = _repoUrl.asStateFlow()

    private val _clipSettings = MutableStateFlow(ClipGenerationSettings())
    val clipSettings: StateFlow<ClipGenerationSettings> = _clipSettings.asStateFlow()

    private val _setupToolProfile = MutableStateFlow(SetupToolProfile.PEAKCUT)
    val setupToolProfile: StateFlow<SetupToolProfile> = _setupToolProfile.asStateFlow()

    private val _targetOs = MutableStateFlow(TargetOsPlatform.WINDOWS)
    val targetOs: StateFlow<TargetOsPlatform> = _targetOs.asStateFlow()

    private val _setupChecklist = MutableStateFlow(
        listOf(
            SetupChecklistItem(
                id = "prereqs",
                title = "01. Verify Python 3.11+, Git, FFmpeg & 10GB disk",
                subtitle = "Keep install folder short: C:\\peakcut (Win) or ~/peakcut (Mac/Linux)",
                checked = true
            ),
            SetupChecklistItem(
                id = "local_ai",
                title = "02. Connect Local AI Brain (Ollama llama3.2:3b)",
                subtitle = "Settings > AI -> http://127.0.0.1:11434/v1 -> Save & test = green Ready",
                checked = false
            ),
            SetupChecklistItem(
                id = "first_test",
                title = "03. Run first clip test (3 clips, 15-45s, 720p)",
                subtitle = "Verify http://127.0.0.1:8765 (PeakCut) or :8770 (NeonClipping /healthz)",
                checked = false
            ),
            SetupChecklistItem(
                id = "review_mp4",
                title = "04. Watch before posting & check 1080x1920 MP4",
                subtitle = "Inspect hook, captions, facecam/HUD framing & audio before downloading",
                checked = false
            ),
            SetupChecklistItem(
                id = "vyro_brief",
                title = "05. Submit against eligible campaign brief",
                subtitle = "Confirm approved creator/source & post link per Vyro campaign terms",
                checked = false
            )
        )
    )
    val setupChecklist: StateFlow<List<SetupChecklistItem>> = _setupChecklist.asStateFlow()

    private val _events = MutableSharedFlow<StudioEvent>()
    val events: SharedFlow<StudioEvent> = _events.asSharedFlow()

    val savedClips: StateFlow<List<ClipEntity>> = repository.allClips.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val liveCreators: StateFlow<List<LiveCreatorEntity>> = repository.allCreators.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val liveLogs: StateFlow<List<LiveLogEntity>> = repository.recentLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var pipelineJob: Job? = null
    private var liveMonitorJob: Job? = null
    private var liveClipCounter = 0

    init {
        viewModelScope.launch {
            repository.seedInitialCreatorsIfEmpty()
        }
        val initialMoments = repository.extractSimulatedMoments(
            url = _pipelineState.value.sourceUrl,
            isLiveMode = false
        )
        _detectedMoments.value = initialMoments
        _selectedMoment.value = initialMoments.firstOrNull()
    }

    fun selectTab(tab: StudioTab) {
        _selectedTab.value = tab
    }

    fun updateSourceUrl(url: String) {
        _pipelineState.update { it.copy(sourceUrl = url) }
    }

    fun applyQuickSampleUrl(url: String) {
        _pipelineState.update { it.copy(sourceUrl = url, uploadedLocalVideoName = null) }
    }

    fun onLocalVideoSelected(displayName: String) {
        val cleanName = displayName.ifBlank { "creator_upload.mp4" }
        _pipelineState.update {
            it.copy(
                sourceUrl = "local://upload/$cleanName",
                uploadedLocalVideoName = cleanName,
                statusDetail = "Loaded local video: $cleanName — tap Clip Now to process"
            )
        }
        viewModelScope.launch {
            _events.emit(StudioEvent.ShowMessage("Loaded MP4: $cleanName"))
        }
    }

    fun updateClipCount(delta: Int) {
        _clipSettings.update {
            it.copy(clipCount = (it.clipCount + delta).coerceIn(1, 6))
        }
    }

    fun updateDurationRange(minSec: Int, maxSec: Int) {
        _clipSettings.update {
            it.copy(minLengthSec = minSec, maxLengthSec = maxSec)
        }
    }

    fun setProcessingMode(mode: ProcessingMode) {
        _clipSettings.update { it.copy(processingMode = mode) }
    }

    fun setQualityLabel(quality: String) {
        _clipSettings.update { it.copy(qualityLabel = quality) }
    }

    fun setCaptionStyle(style: CaptionStyle) {
        _clipSettings.update { it.copy(captionStyle = style) }
        _framingConfig.update { it.copy(captionStyle = style) }
    }

    fun setFacecamPosition(position: FacecamPosition) {
        _clipSettings.update { it.copy(facecamPosition = position) }
        _framingConfig.update { current ->
            val targetMode = when (position) {
                FacecamPosition.TOP_SPLIT -> VerticalLayoutMode.SPLIT_STACK
                FacecamPosition.NONE -> VerticalLayoutMode.FILL_SCREEN
                else -> VerticalLayoutMode.PIP_CORNER
            }
            current.copy(facecamPosition = position, layoutMode = targetMode)
        }
    }

    fun setSetupToolProfile(profile: SetupToolProfile) {
        _setupToolProfile.value = profile
    }

    fun setTargetOs(os: TargetOsPlatform) {
        _targetOs.value = os
    }

    fun startPipeline(isLiveMode: Boolean) {
        val url = _pipelineState.value.sourceUrl.trim()
        if (url.isBlank()) {
            viewModelScope.launch {
                _events.emit(StudioEvent.ShowMessage("Please paste a YouTube video, live stream URL, or upload an MP4 first."))
            }
            return
        }

        pipelineJob?.cancel()
        pipelineJob = viewModelScope.launch {
            repository.processStream(
                sourceUrl = url,
                isLiveMode = isLiveMode,
                framingConfig = _framingConfig.value,
                settings = _clipSettings.value
            ).collect { event ->
                when (event) {
                    is ClippingProgressEvent.StageUpdate -> {
                        _pipelineState.update {
                            it.copy(
                                isLiveMode = isLiveMode,
                                isRunning = event.progressPercent < 100,
                                stage = event.stage,
                                progressPercent = event.progressPercent,
                                statusDetail = event.statusDetail
                            )
                        }
                    }
                    is ClippingProgressEvent.Completed -> {
                        _detectedMoments.value = event.moments
                        _selectedMoment.value = event.moments.firstOrNull()
                        _isPreviewOpen.value = true
                        _hookUiState.value = HookUiState.Idle

                        if (isLiveMode && !_isLiveMonitorActive.value) {
                            startLiveMonitoring()
                        }

                        _events.emit(
                            StudioEvent.ShowMessage(
                                if (isLiveMode) "Live stream clipping pipeline active (100%)."
                                else "Found ${event.moments.size} viral GTA VI moments (100%)."
                            )
                        )
                    }
                }
            }
        }
    }

    fun selectMoment(moment: DetectedMoment) {
        _selectedMoment.value = moment
        _hookUiState.value = HookUiState.Idle
    }

    fun getPreviewForMoment(moment: DetectedMoment) {
        _selectedMoment.value = moment
        _isPreviewOpen.value = true
        _hookUiState.value = HookUiState.Idle
    }

    fun selectFramingLayer(layer: FramingLayer) {
        _framingConfig.update { it.copy(activeLayer = layer) }
    }

    fun setLayoutMode(mode: VerticalLayoutMode) {
        _framingConfig.update { it.copy(layoutMode = mode) }
    }

    fun updateSplitRatio(ratio: Float) {
        _framingConfig.update { it.copy(splitRatio = ratio.coerceIn(0.22f, 0.54f)) }
    }

    fun updateActiveZoom(zoom: Float) {
        val clamped = zoom.coerceIn(0.8f, 2.0f)
        _framingConfig.update { current ->
            if (current.activeLayer == FramingLayer.FACECAM) {
                current.copy(facecamZoom = clamped)
            } else {
                current.copy(gameplayZoom = clamped)
            }
        }
    }

    fun updateActiveBoxWidth(width: Float) {
        val clamped = width.coerceIn(0.45f, 1.0f)
        _framingConfig.update { current ->
            if (current.activeLayer == FramingLayer.FACECAM) {
                current.copy(facecamBoxWidth = clamped)
            } else {
                current.copy(gameplayBoxWidth = clamped)
            }
        }
    }

    fun updateActiveBoxHeight(height: Float) {
        val clamped = height.coerceIn(0.45f, 1.0f)
        _framingConfig.update { current ->
            if (current.activeLayer == FramingLayer.FACECAM) {
                current.copy(facecamBoxHeight = clamped)
            } else {
                current.copy(gameplayBoxHeight = clamped)
            }
        }
    }

    fun panActiveLayer(deltaX: Float, deltaY: Float) {
        _framingConfig.update { current ->
            if (current.activeLayer == FramingLayer.FACECAM) {
                current.copy(
                    facecamPanX = (current.facecamPanX + deltaX).coerceIn(-1f, 1f),
                    facecamPanY = (current.facecamPanY + deltaY).coerceIn(-1f, 1f)
                )
            } else {
                current.copy(
                    gameplayPanX = (current.gameplayPanX + deltaX).coerceIn(-1f, 1f),
                    gameplayPanY = (current.gameplayPanY + deltaY).coerceIn(-1f, 1f)
                )
            }
        }
    }

    fun toggleHookCaption(show: Boolean) {
        _framingConfig.update { it.copy(showHookCaption = show) }
    }

    fun resetFraming() {
        _framingConfig.value = FramingConfig()
    }

    fun generateAiViralHookForSelectedMoment() {
        val moment = _selectedMoment.value ?: return
        viewModelScope.launch {
            _hookUiState.value = HookUiState.Loading
            val result = aiHookRepository.generateGeminiViralHook(
                momentTitle = moment.title,
                hypeTag = moment.hypeTag,
                creatorName = moment.creatorName,
                startTime = moment.startTime,
                endTime = moment.endTime
            )
            _hookUiState.value = result.fold(
                onSuccess = { HookUiState.Success(it) },
                onFailure = { HookUiState.Error(it.message ?: "Gemini hook generation failed.") }
            )
        }
    }

    fun useSmartFallbackHook() {
        val moment = _selectedMoment.value ?: return
        val fallback = aiHookRepository.generateLocalSmartFallback(
            momentTitle = moment.title,
            hypeTag = moment.hypeTag,
            creatorName = moment.creatorName
        )
        _hookUiState.value = HookUiState.Success(fallback)
    }

    fun createClipFromPreview() {
        val moment = _selectedMoment.value ?: return
        val framing = _framingConfig.value
        val sourceUrl = _pipelineState.value.sourceUrl
        val currentHook = (_hookUiState.value as? HookUiState.Success)?.result
            ?: aiHookRepository.generateLocalSmartFallback(
                momentTitle = moment.title,
                hypeTag = moment.hypeTag,
                creatorName = moment.creatorName
            )

        viewModelScope.launch {
            repository.saveClip(
                moment = moment,
                sourceUrl = sourceUrl,
                framing = framing,
                hookResult = currentHook,
                isAutoClipped = false
            )
            _events.emit(
                StudioEvent.ShowMessage(
                    message = "9:16 Clip saved to My Clips!",
                    actionLabel = "View Clips",
                    targetTab = StudioTab.MY_CLIPS
                )
            )
        }
    }

    fun addCreator(name: String, platform: String, streamUrl: String) {
        if (name.isBlank() || streamUrl.isBlank()) {
            viewModelScope.launch {
                _events.emit(StudioEvent.ShowMessage("Enter both a creator name and stream URL."))
            }
            return
        }
        viewModelScope.launch {
            repository.addCreator(name, platform, streamUrl)
            _events.emit(StudioEvent.ShowMessage("Added $name to live watchlist."))
        }
    }

    fun toggleCreatorAutoClip(creator: LiveCreatorEntity) {
        viewModelScope.launch {
            repository.toggleAutoClip(
                creatorId = creator.id,
                enabled = !creator.autoClipEnabled,
                creatorName = creator.name
            )
        }
    }

    fun deleteCreator(creatorId: Long) {
        viewModelScope.launch {
            repository.deleteCreator(creatorId)
        }
    }

    fun toggleLiveMonitoring() {
        if (_isLiveMonitorActive.value) {
            stopLiveMonitoring()
        } else {
            startLiveMonitoring()
        }
    }

    private fun startLiveMonitoring() {
        liveMonitorJob?.cancel()
        _isLiveMonitorActive.value = true
        viewModelScope.launch {
            repository.logMonitoringState(
                message = "Live Auto-Clipping engine started (host session active)",
                status = "MONITORING"
            )
        }
        liveMonitorJob = viewModelScope.launch {
            while (true) {
                delay(6500L)
                val enabledCreators = liveCreators.value.filter { it.autoClipEnabled }
                if (enabledCreators.isNotEmpty()) {
                    val target = enabledCreators[liveClipCounter % enabledCreators.size]
                    liveClipCounter++
                    val hypeTags = listOf("HIGH SPEED CHASE", "INSANE CLUTCH", "CHAT EXPLOSION", "STUNT JUMP")
                    val tag = hypeTags[liveClipCounter % hypeTags.size]
                    val moment = DetectedMoment(
                        id = "live_${System.currentTimeMillis()}",
                        title = "${target.name} Auto-Clip #$liveClipCounter",
                        creatorName = target.name,
                        startTime = "LIVE-${18 + liveClipCounter}:04",
                        endTime = "LIVE-${18 + liveClipCounter}:34",
                        durationSec = 30,
                        hypeTag = tag,
                        energyScore = 91 + (liveClipCounter % 9),
                        waveform = listOf(0.4f, 0.7f, 0.95f, 1.0f, 0.85f, 0.65f, 0.9f, 0.75f),
                        sceneHueSeed = liveClipCounter % 5
                    )
                    val hook = aiHookRepository.generateLocalSmartFallback(
                        momentTitle = moment.title,
                        hypeTag = moment.hypeTag,
                        creatorName = target.name
                    )
                    repository.recordAutoClippedHighlight(target, moment, hook)
                }
            }
        }
    }

    private fun stopLiveMonitoring() {
        liveMonitorJob?.cancel()
        liveMonitorJob = null
        _isLiveMonitorActive.value = false
        viewModelScope.launch {
            repository.logMonitoringState(
                message = "Live Auto-Clipping engine paused by user",
                status = "PAUSED"
            )
        }
    }

    fun openClipPlayer(clip: ClipEntity) {
        _activePlaybackClip.value = clip
        _isPlaybackPlaying.value = true
    }

    fun togglePlayback() {
        _isPlaybackPlaying.update { !it }
    }

    fun closeClipPlayer() {
        _activePlaybackClip.value = null
    }

    fun deleteClip(clip: ClipEntity) {
        viewModelScope.launch {
            if (_activePlaybackClip.value?.id == clip.id) {
                _activePlaybackClip.value = null
            }
            repository.deleteClip(clip.id)
            _events.emit(StudioEvent.ShowMessage("Deleted \"${clip.viralTitle}\"."))
        }
    }

    fun downloadClip(clip: ClipEntity) {
        viewModelScope.launch {
            val result = repository.exportClip(clip)
            result.fold(
                onSuccess = { exported ->
                    _events.emit(StudioEvent.ClipExported(exported))
                },
                onFailure = { error ->
                    _events.emit(
                        StudioEvent.ShowMessage(
                            "Download failed: ${error.localizedMessage ?: "Unknown export error"}"
                        )
                    )
                }
            )
        }
    }

    fun regenerateClipHookWithAi(clip: ClipEntity) {
        viewModelScope.launch {
            _events.emit(StudioEvent.ShowMessage("Generating fresh Gemini hook for clip..."))
            val result = aiHookRepository.generateGeminiViralHook(
                momentTitle = clip.title,
                hypeTag = clip.hypeTag,
                creatorName = clip.creatorName,
                startTime = clip.startTime,
                endTime = clip.endTime
            )
            result.fold(
                onSuccess = { hook ->
                    repository.updateClipHook(clip, hook)
                    if (_activePlaybackClip.value?.id == clip.id) {
                        _activePlaybackClip.value = clip.copy(
                            viralTitle = hook.viralTitle,
                            viralHook = hook.openingHook,
                            hashtagsCsv = hook.hashtags.joinToString(", ")
                        )
                    }
                    _events.emit(StudioEvent.ShowMessage("Updated clip with Gemini AI viral hook!"))
                },
                onFailure = { err ->
                    _events.emit(
                        StudioEvent.ShowMessage("Gemini error: ${err.localizedMessage ?: "Check connection"}")
                    )
                }
            )
        }
    }

    fun updateRepoUrl(url: String) {
        _repoUrl.value = url
    }

    fun toggleChecklistItem(id: String) {
        _setupChecklist.update { items ->
            items.map { if (it.id == id) it.copy(checked = !it.checked) else it }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pipelineJob?.cancel()
        liveMonitorJob?.cancel()
    }
}
