package com.example.engine

import com.example.data.model.ClipGenerationSettings
import com.example.data.model.DetectedMoment
import com.example.data.model.FramingConfig
import com.example.data.model.PipelineStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ClippingEngine(
    private val mediaReader: StreamMediaReader = StreamMediaReader(),
    private val peakDetector: AudioPeakDetector = AudioPeakDetector(),
    private val frameGenerator: VerticalFrameGenerator = VerticalFrameGenerator()
) {

    fun processStream(
        sourceUrl: String,
        isLiveMode: Boolean,
        framingConfig: FramingConfig = FramingConfig(),
        settings: ClipGenerationSettings = ClipGenerationSettings(),
        stepDelayMs: Long = 380L
    ): Flow<ClippingProgressEvent> = flow {
        // Stage 1: Reading video & downloading audio
        emit(
            ClippingProgressEvent.StageUpdate(
                stage = PipelineStage.READING_MEDIA,
                progressPercent = 14,
                statusDetail = "Connecting to stream metadata & demuxing video track..."
            )
        )
        val metadata = mediaReader.parseSourceMetadata(sourceUrl, isLiveMode)
        if (stepDelayMs > 0L) delay(stepDelayMs)

        emit(
            ClippingProgressEvent.StageUpdate(
                stage = PipelineStage.READING_MEDIA,
                progressPercent = 34,
                statusDetail = "Downloading 48kHz audio envelope (${metadata.platform})..."
            )
        )
        val audioBuffer = mediaReader.readAudioEnvelope(metadata)
        if (stepDelayMs > 0L) delay(stepDelayMs)

        // Stage 2: Audio-based peak detection
        emit(
            ClippingProgressEvent.StageUpdate(
                stage = PipelineStage.ANALYZING_PEAKS,
                progressPercent = 46,
                statusDetail = "Running ${settings.processingMode.label} audio & chat spike scan..."
            )
        )
        if (stepDelayMs > 0L) delay(stepDelayMs)

        val targetDuration = ((settings.minLengthSec + settings.maxLengthSec) / 2).coerceIn(15, 80)
        val peaks = peakDetector.detectPeaks(
            buffer = audioBuffer,
            maxPeaks = settings.clipCount.coerceIn(1, 6),
            clipDurationSec = targetDuration
        )
        emit(
            ClippingProgressEvent.StageUpdate(
                stage = PipelineStage.ANALYZING_PEAKS,
                progressPercent = 58,
                statusDetail = "Analyzing audio/action peaks — ${peaks.size} moments locked (58%)"
            )
        )
        if (stepDelayMs > 0L) delay((stepDelayMs * 1.4f).toLong())

        val moments = mapPeaksToMoments(metadata, peaks)
        emit(
            ClippingProgressEvent.StageUpdate(
                stage = PipelineStage.ANALYZING_PEAKS,
                progressPercent = 74,
                statusDetail = "Ranked top ${moments.size} hype windows (${settings.minLengthSec}-${settings.maxLengthSec}s)..."
            )
        )
        if (stepDelayMs > 0L) delay(stepDelayMs)

        // Stage 3: 9:16 frame generation logic
        emit(
            ClippingProgressEvent.StageUpdate(
                stage = PipelineStage.RENDERING_VERTICAL,
                progressPercent = 88,
                statusDetail = "Generating ${settings.qualityLabel} 9:16 frames (${framingConfig.layoutMode.label}, ${settings.captionStyle.label})..."
            )
        )
        val generatedFrames = moments.map { moment ->
            val layoutSpec = frameGenerator.computeLayoutSpec(
                framing = framingConfig,
                sourceWidth = metadata.sourceWidth,
                sourceHeight = metadata.sourceHeight
            )
            GeneratedClipFrame(
                momentId = moment.id,
                layoutSpec = layoutSpec,
                renderedTimestamp = moment.startTime
            )
        }
        if (stepDelayMs > 0L) delay(stepDelayMs)

        emit(
            ClippingProgressEvent.StageUpdate(
                stage = PipelineStage.COMPLETED,
                progressPercent = 100,
                statusDetail = "Ready! Select any Best Moment and click Get Preview."
            )
        )
        emit(
            ClippingProgressEvent.Completed(
                metadata = metadata,
                moments = moments,
                generatedFrames = generatedFrames
            )
        )
    }

    fun analyzeUrlInstant(
        sourceUrl: String,
        isLiveMode: Boolean,
        settings: ClipGenerationSettings = ClipGenerationSettings()
    ): List<DetectedMoment> {
        val metadata = mediaReader.parseSourceMetadata(sourceUrl, isLiveMode)
        val audioBuffer = mediaReader.readAudioEnvelope(metadata)
        val targetDuration = ((settings.minLengthSec + settings.maxLengthSec) / 2).coerceIn(15, 80)
        val peaks = peakDetector.detectPeaks(
            buffer = audioBuffer,
            maxPeaks = settings.clipCount.coerceIn(1, 6),
            clipDurationSec = targetDuration
        )
        return mapPeaksToMoments(metadata, peaks)
    }

    fun computeFrameLayout(framing: FramingConfig): VerticalFrameLayoutSpec =
        frameGenerator.computeLayoutSpec(framing)

    private fun mapPeaksToMoments(
        metadata: SourceMediaMetadata,
        peaks: List<DetectedAudioPeak>
    ): List<DetectedMoment> {
        val titleTemplates = mapOf(
            "HIGH SPEED CHASE" to "5-Star Ocean Beach Chopper Escape",
            "INSANE CLUTCH" to "Vice Metro Vault Breach Clutch",
            "CHAT EXPLOSION" to "Chat Explosion on Secret Map Glitch",
            "STUNT JUMP" to "Rooftop Banshee Skyline Stunt"
        )
        val prefix = if (metadata.isLiveStream) "LIVE PEAK: " else ""

        return peaks.mapIndexed { index, peak ->
            val baseTitle = titleTemplates[peak.hypeTag] ?: "High-Energy Highlight #${index + 1}"
            DetectedMoment(
                id = "m_${metadata.videoId}_${index + 1}",
                title = "$prefix$baseTitle",
                creatorName = metadata.creatorHandle,
                startTime = formatTimestamp(peak.startSec),
                endTime = formatTimestamp(peak.endSec),
                durationSec = (peak.endSec - peak.startSec).coerceAtLeast(1),
                hypeTag = peak.hypeTag,
                energyScore = peak.energyScore,
                waveform = peak.waveform,
                sceneHueSeed = (kotlin.math.abs(metadata.videoId.hashCode()) + index) % 5
            )
        }
    }

    private fun formatTimestamp(totalSec: Int): String {
        val mins = totalSec / 60
        val secs = totalSec % 60
        return "%02d:%02d".format(mins, secs)
    }
}
