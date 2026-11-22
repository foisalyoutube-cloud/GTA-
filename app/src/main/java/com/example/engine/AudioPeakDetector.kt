package com.example.engine

import kotlin.math.sqrt

class AudioPeakDetector {

    fun detectPeaks(
        buffer: AudioEnvelopeBuffer,
        maxPeaks: Int = 4,
        minSeparationSec: Int = 60,
        clipDurationSec: Int = 30
    ): List<DetectedAudioPeak> {
        val samples = buffer.samples
        if (samples.isEmpty()) return emptyList()

        // Compute mean & std-dev of RMS dB across the stream
        val meanDb = samples.map { it.rmsDb }.average().toFloat()
        val variance = samples.map { (it.rmsDb - meanDb) * (it.rmsDb - meanDb) }.average().toFloat()
        val stdDev = sqrt(variance).coerceAtLeast(1.0f)

        // Score every window using smoothed 5-second energy + z-score + chat spike
        data class ScoredCandidate(
            val index: Int,
            val score: Float,
            val zScore: Float,
            val peakDb: Float
        )

        val candidates = ArrayList<ScoredCandidate>(samples.size)
        for (i in samples.indices) {
            val winStart = (i - 2).coerceAtLeast(0)
            val winEnd = (i + 2).coerceAtMost(samples.lastIndex)
            var sumAmp = 0f
            var sumChat = 0f
            var localMaxDb = -60f
            var count = 0
            for (j in winStart..winEnd) {
                val s = samples[j]
                sumAmp += s.peakAmplitude
                sumChat += s.chatVelocity
                if (s.rmsDb > localMaxDb) localMaxDb = s.rmsDb
                count++
            }
            val avgAmp = sumAmp / count
            val avgChat = sumChat / count
            val z = (localMaxDb - meanDb) / stdDev
            val composite = (z * 0.55f) + (avgAmp * 2.2f) + (avgChat * 1.4f)

            if (z >= 0.8f) {
                candidates.add(
                    ScoredCandidate(
                        index = i,
                        score = composite,
                        zScore = z,
                        peakDb = localMaxDb
                    )
                )
            }
        }

        // Non-maximum suppression: pick strongest peaks spaced at least minSeparationSec apart
        val selected = mutableListOf<ScoredCandidate>()
        for (candidate in candidates.sortedByDescending { it.score }) {
            val tooClose = selected.any { kotlin.math.abs(it.index - candidate.index) < minSeparationSec }
            if (!tooClose) {
                selected.add(candidate)
                if (selected.size >= maxPeaks) break
            }
        }

        val totalSec = samples.last().timestampSec.coerceAtLeast(clipDurationSec)
        val halfPre = (clipDurationSec * 0.42f).toInt()

        return selected
            .sortedBy { it.index }
            .mapIndexed { rank, peak ->
                val startSec = (peak.index - halfPre).coerceIn(0, (totalSec - clipDurationSec).coerceAtLeast(0))
                val endSec = (startSec + clipDurationSec).coerceAtMost(totalSec)
                val waveform = extractWaveformBars(samples, startSec, endSec, bars = 10)
                val normalizedEnergy = (78 + (peak.zScore * 7f).toInt()).coerceIn(72, 99)
                val chatPeak = samples[peak.index].chatVelocity

                val hypeTag = classifyHypeTag(
                    peakDb = peak.peakDb,
                    chatVelocity = chatPeak,
                    rankIndex = rank
                )

                DetectedAudioPeak(
                    centerSec = peak.index,
                    startSec = startSec,
                    endSec = endSec,
                    peakDb = peak.peakDb,
                    zScore = peak.zScore,
                    energyScore = normalizedEnergy,
                    hypeTag = hypeTag,
                    waveform = waveform
                )
            }
    }

    private fun extractWaveformBars(
        samples: List<AudioFrameSample>,
        startSec: Int,
        endSec: Int,
        bars: Int
    ): List<Float> {
        val span = (endSec - startSec).coerceAtLeast(1)
        val bucketSize = (span.toFloat() / bars).coerceAtLeast(1f)
        return List(bars) { barIdx ->
            val from = (startSec + (barIdx * bucketSize).toInt()).coerceIn(0, samples.lastIndex)
            val to = (startSec + ((barIdx + 1) * bucketSize).toInt()).coerceIn(from, samples.lastIndex)
            var peakAmp = 0f
            for (i in from..to) {
                if (samples[i].peakAmplitude > peakAmp) peakAmp = samples[i].peakAmplitude
            }
            peakAmp.coerceIn(0.18f, 1.0f)
        }
    }

    private fun classifyHypeTag(peakDb: Float, chatVelocity: Float, rankIndex: Int): String {
        return when {
            peakDb > -3.0f && chatVelocity > 0.85f -> "HIGH SPEED CHASE"
            chatVelocity > 0.88f -> "CHAT EXPLOSION"
            rankIndex % 2 == 1 -> "INSANE CLUTCH"
            else -> "STUNT JUMP"
        }
    }
}
