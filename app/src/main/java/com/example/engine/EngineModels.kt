package com.example.engine

import com.example.data.model.DetectedMoment
import com.example.data.model.PipelineStage
import com.example.data.model.VerticalLayoutMode

data class SourceMediaMetadata(
    val sourceUrl: String,
    val platform: String,
    val creatorHandle: String,
    val videoId: String,
    val isLiveStream: Boolean,
    val sourceWidth: Int = 1920,
    val sourceHeight: Int = 1080,
    val fps: Int = 60,
    val durationSeconds: Int = 1200
)

data class AudioFrameSample(
    val timestampSec: Int,
    val rmsDb: Float,
    val peakAmplitude: Float,
    val chatVelocity: Float
)

data class AudioEnvelopeBuffer(
    val metadata: SourceMediaMetadata,
    val sampleRateHz: Int = 48_000,
    val windowStepSec: Int = 1,
    val samples: List<AudioFrameSample>
)

data class DetectedAudioPeak(
    val centerSec: Int,
    val startSec: Int,
    val endSec: Int,
    val peakDb: Float,
    val zScore: Float,
    val energyScore: Int,
    val hypeTag: String,
    val waveform: List<Float>
)

data class PixelRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

data class VerticalFrameLayoutSpec(
    val targetWidth: Int = 1080,
    val targetHeight: Int = 1920,
    val layoutMode: VerticalLayoutMode,
    val splitY: Float,
    val facecamTargetBounds: PixelRect,
    val gameplayTargetBounds: PixelRect,
    val facecamSourceCrop: PixelRect,
    val gameplaySourceCrop: PixelRect,
    val captionSafeBounds: PixelRect
)

data class GeneratedClipFrame(
    val momentId: String,
    val layoutSpec: VerticalFrameLayoutSpec,
    val renderedTimestamp: String
)

sealed interface ClippingProgressEvent {
    data class StageUpdate(
        val stage: PipelineStage,
        val progressPercent: Int,
        val statusDetail: String
    ) : ClippingProgressEvent

    data class Completed(
        val metadata: SourceMediaMetadata,
        val moments: List<DetectedMoment>,
        val generatedFrames: List<GeneratedClipFrame>
    ) : ClippingProgressEvent
}
