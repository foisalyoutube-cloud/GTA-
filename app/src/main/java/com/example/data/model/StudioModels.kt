package com.example.data.model

enum class StudioTab(val label: String) {
    CLIP_STUDIO("Clip Studio"),
    LIVE_CREATORS("Live Creators"),
    MY_CLIPS("My Clips"),
    QUICK_SETUP("Setup Guide")
}

enum class PipelineStage(val stepNumber: Int, val label: String) {
    IDLE(0, "Ready to analyze stream"),
    READING_MEDIA(1, "Reading video & downloading audio"),
    ANALYZING_PEAKS(2, "Analyzing audio/action peaks to find best moments"),
    RENDERING_VERTICAL(3, "Rendering vertical 9:16 clips"),
    COMPLETED(4, "All vertical 9:16 clips ready")
}

enum class ProcessingMode(val label: String, val subtitle: String) {
    FAST("Fast (~2 min)", "Quick audio & caption pass"),
    DEEP("Deep (AI Pick)", "Full transcript + LLM moment rank")
}

enum class CaptionStyle(val label: String) {
    GTA_NEON("GTA Neon"),
    BOUNCE("Bounce"),
    HYPE("Hype"),
    CLEAN("Clean"),
    HORMOZI("Hormozi"),
    MR_BEAST("MrBeast"),
    IMPACT("Impact")
}

enum class FacecamPosition(val label: String) {
    TOP_SPLIT("Top Split"),
    TOP_LEFT("Top-Left"),
    TOP_RIGHT("Top-Right"),
    BOTTOM_LEFT("Bottom-Left"),
    BOTTOM_RIGHT("Bottom-Right"),
    BOTTOM_CENTER("Bottom-Centre"),
    NONE("None")
}

data class ClipGenerationSettings(
    val clipCount: Int = 3,
    val minLengthSec: Int = 15,
    val maxLengthSec: Int = 45,
    val qualityLabel: String = "720p",
    val processingMode: ProcessingMode = ProcessingMode.FAST,
    val captionStyle: CaptionStyle = CaptionStyle.GTA_NEON,
    val facecamPosition: FacecamPosition = FacecamPosition.TOP_SPLIT
)

data class PipelineState(
    val sourceUrl: String = "https://www.youtube.com/watch?v=QdBZY2fkU-0",
    val uploadedLocalVideoName: String? = null,
    val isLiveMode: Boolean = false,
    val isRunning: Boolean = false,
    val stage: PipelineStage = PipelineStage.IDLE,
    val progressPercent: Int = 0,
    val statusDetail: String = "Paste a YouTube, Twitch, Kick link or upload an MP4",
    val isSimulatedMode: Boolean = true
)

data class DetectedMoment(
    val id: String,
    val title: String,
    val creatorName: String,
    val startTime: String,
    val endTime: String,
    val durationSec: Int,
    val hypeTag: String,
    val energyScore: Int,
    val waveform: List<Float>,
    val sceneHueSeed: Int
)

enum class FramingLayer(val label: String) {
    FACECAM("Facecam Area"),
    GAMEPLAY("Gameplay Area")
}

enum class VerticalLayoutMode(val label: String) {
    SPLIT_STACK("Gameplay + Facecam"),
    PIP_CORNER("Facecam PiP"),
    BLURRED_FILL("Blurred Fill"),
    ZOOMED_IN("Zoomed In"),
    FILL_SCREEN("Fill Screen")
}

data class FramingConfig(
    val activeLayer: FramingLayer = FramingLayer.FACECAM,
    val layoutMode: VerticalLayoutMode = VerticalLayoutMode.SPLIT_STACK,
    val facecamPosition: FacecamPosition = FacecamPosition.TOP_SPLIT,
    val captionStyle: CaptionStyle = CaptionStyle.GTA_NEON,
    val splitRatio: Float = 0.34f,
    val facecamZoom: Float = 1.15f,
    val facecamPanX: Float = 0f,
    val facecamPanY: Float = 0f,
    val facecamBoxWidth: Float = 0.72f,
    val facecamBoxHeight: Float = 0.68f,
    val gameplayZoom: Float = 1.25f,
    val gameplayPanX: Float = 0f,
    val gameplayPanY: Float = 0f,
    val gameplayBoxWidth: Float = 0.85f,
    val gameplayBoxHeight: Float = 0.78f,
    val showHookCaption: Boolean = true
)

data class ViralHookResult(
    val viralTitle: String,
    val openingHook: String,
    val hashtags: List<String>,
    val generatorSource: String
)

sealed interface HookUiState {
    data object Idle : HookUiState
    data object Loading : HookUiState
    data class Success(val result: ViralHookResult) : HookUiState
    data class Error(val message: String) : HookUiState
}

enum class SetupToolProfile(val label: String) {
    PEAKCUT("PeakCut Guide"),
    NEONCLIPPING("NeonClipping (Hinglish)"),
    CUSTOM("Custom Repo")
}

enum class TargetOsPlatform(val label: String) {
    WINDOWS("Windows"),
    MAC_LINUX("macOS / Linux")
}

data class SetupChecklistItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val checked: Boolean = false
)
