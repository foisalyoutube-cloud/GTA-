package com.example

import com.example.data.model.FramingConfig
import com.example.data.model.PipelineStage
import com.example.data.model.VerticalLayoutMode
import com.example.engine.AudioPeakDetector
import com.example.engine.ClippingEngine
import com.example.engine.ClippingProgressEvent
import com.example.engine.StreamMediaReader
import com.example.engine.VerticalFrameGenerator
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClippingEngineTest {

    @Test
    fun streamMediaReader_parsesUrlAndBuildsAudioEnvelope() {
        val reader = StreamMediaReader()
        val metadata = reader.parseSourceMetadata(
            sourceUrl = "https://youtube.com/@ViceCityVortex/live",
            isLiveMode = true
        )
        val buffer = reader.readAudioEnvelope(metadata)

        assertEquals("YouTube", metadata.platform)
        assertEquals("ViceCityVortex", metadata.creatorHandle)
        assertTrue(metadata.isLiveStream)
        assertTrue(buffer.samples.isNotEmpty())
        assertEquals(48_000, buffer.sampleRateHz)
    }

    @Test
    fun audioPeakDetector_detectsNonOverlappingHighEnergyPeaks() {
        val reader = StreamMediaReader()
        val detector = AudioPeakDetector()
        val metadata = reader.parseSourceMetadata("https://youtube.com/watch?v=gtavi-chase", false)
        val buffer = reader.readAudioEnvelope(metadata)

        val peaks = detector.detectPeaks(buffer, maxPeaks = 4, minSeparationSec = 60)

        assertEquals(4, peaks.size)
        for (i in 1 until peaks.size) {
            assertTrue(peaks[i].centerSec - peaks[i - 1].centerSec >= 60)
        }
        assertTrue(peaks.all { it.waveform.size == 10 && it.energyScore in 72..99 })
    }

    @Test
    fun verticalFrameGenerator_computesValid916SpecsForSplitAndPip() {
        val generator = VerticalFrameGenerator()

        val splitSpec = generator.computeLayoutSpec(
            FramingConfig(layoutMode = VerticalLayoutMode.SPLIT_STACK, splitRatio = 0.35f)
        )
        assertEquals(1080, splitSpec.targetWidth)
        assertEquals(1920, splitSpec.targetHeight)
        assertEquals(0f, splitSpec.facecamTargetBounds.top, 0.1f)
        assertEquals(splitSpec.splitY, splitSpec.facecamTargetBounds.bottom, 0.1f)
        assertEquals(splitSpec.splitY, splitSpec.gameplayTargetBounds.top, 0.1f)
        assertTrue(splitSpec.facecamSourceCrop.left >= 0f && splitSpec.facecamSourceCrop.right <= 1920f)
        assertTrue(splitSpec.gameplaySourceCrop.top >= 0f && splitSpec.gameplaySourceCrop.bottom <= 1080f)

        val pipSpec = generator.computeLayoutSpec(
            FramingConfig(
                layoutMode = VerticalLayoutMode.PIP_CORNER,
                facecamPosition = com.example.data.model.FacecamPosition.BOTTOM_RIGHT
            )
        )
        assertEquals(1920f, pipSpec.gameplayTargetBounds.height, 0.1f)
        assertTrue(pipSpec.facecamTargetBounds.width < 500f)

        val blurredSpec = generator.computeLayoutSpec(
            FramingConfig(layoutMode = VerticalLayoutMode.BLURRED_FILL)
        )
        assertEquals(1080, blurredSpec.targetWidth)
        assertEquals(1920, blurredSpec.targetHeight)
        assertTrue(blurredSpec.gameplayTargetBounds.top > 400f)
    }

    @Test
    fun clippingEngine_emitsAllThreeStagesAndCompletesAt100Percent() = runTest {
        val engine = ClippingEngine()
        val events = engine.processStream(
            sourceUrl = "https://youtube.com/watch?v=gtavi-vice-chase-live",
            isLiveMode = false,
            framingConfig = FramingConfig(),
            settings = com.example.data.model.ClipGenerationSettings(clipCount = 4),
            stepDelayMs = 0L
        ).toList()

        val stageUpdates = events.filterIsInstance<ClippingProgressEvent.StageUpdate>()
        val completed = events.filterIsInstance<ClippingProgressEvent.Completed>().single()

        assertTrue(stageUpdates.any { it.stage == PipelineStage.READING_MEDIA })
        assertTrue(stageUpdates.any { it.stage == PipelineStage.ANALYZING_PEAKS && it.progressPercent == 58 })
        assertTrue(stageUpdates.any { it.stage == PipelineStage.RENDERING_VERTICAL })
        assertEquals(100, stageUpdates.last().progressPercent)
        assertEquals(4, completed.moments.size)
        assertEquals(4, completed.generatedFrames.size)
    }

    @Test
    fun clippingEngine_respectsDefaultPeakCutThreeClipSetting() = runTest {
        val engine = ClippingEngine()
        val moments = engine.analyzeUrlInstant(
            sourceUrl = "https://www.youtube.com/watch?v=QdBZY2fkU-0",
            isLiveMode = false,
            settings = com.example.data.model.ClipGenerationSettings()
        )
        assertEquals(3, moments.size)
    }
}
