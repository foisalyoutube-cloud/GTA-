package com.example.engine

import kotlin.math.abs
import kotlin.math.sin

class StreamMediaReader {

    fun parseSourceMetadata(sourceUrl: String, isLiveMode: Boolean): SourceMediaMetadata {
        val trimmed = sourceUrl.trim()
        val lower = trimmed.lowercase()

        val platform = when {
            "twitch.tv" in lower -> "Twitch"
            "kick.com" in lower -> "Kick"
            "youtu.be" in lower || "youtube.com" in lower -> "YouTube"
            else -> "Stream URL"
        }

        val creatorHandle = extractCreatorHandle(trimmed)
        val videoId = extractVideoId(trimmed)
        val detectedLive = isLiveMode || "/live" in lower || "twitch.tv/" in lower || "kick.com/" in lower
        val durationSec = if (detectedLive) 1800 else 1080

        return SourceMediaMetadata(
            sourceUrl = trimmed,
            platform = platform,
            creatorHandle = creatorHandle,
            videoId = videoId,
            isLiveStream = detectedLive,
            sourceWidth = 1920,
            sourceHeight = 1080,
            fps = 60,
            durationSeconds = durationSec
        )
    }

    fun readAudioEnvelope(metadata: SourceMediaMetadata): AudioEnvelopeBuffer {
        val seed = abs(metadata.sourceUrl.hashCode())
        val duration = metadata.durationSeconds.coerceAtLeast(180)

        // Deterministic multi-burst peak anchors across the stream timeline
        val burstAnchors = listOf(
            BurstAnchor(centerSec = 88 + (seed % 25), intensityDb = -2.2f, chatBoost = 0.96f),
            BurstAnchor(centerSec = 304 + (seed % 40), intensityDb = -3.1f, chatBoost = 0.89f),
            BurstAnchor(centerSec = 558 + (seed % 45), intensityDb = -3.6f, chatBoost = 0.94f),
            BurstAnchor(centerSec = 874 + (seed % 50), intensityDb = -4.2f, chatBoost = 0.82f)
        )

        val samples = ArrayList<AudioFrameSample>(duration)
        for (sec in 0 until duration) {
            // Baseline stream commentary audio (-26 dB to -18 dB)
            val ambientWave = sin(sec * 0.23f + (seed % 11)) * 3.2f
            var rmsDb = -22.0f + ambientWave
            var chatVelocity = 0.18f + 0.08f * abs(sin(sec * 0.11f))

            for (anchor in burstAnchors) {
                val dist = abs(sec - anchor.centerSec)
                if (dist <= 16) {
                    val falloff = 1f - (dist / 17f)
                    val burstDb = anchor.intensityDb - (1f - falloff) * 14f
                    if (burstDb > rmsDb) {
                        rmsDb = burstDb
                    }
                    chatVelocity = maxOf(chatVelocity, anchor.chatBoost * falloff + 0.15f)
                }
            }

            val clampedDb = rmsDb.coerceIn(-45f, -0.5f)
            val linearAmp = Math.pow(10.0, (clampedDb / 20.0)).toFloat().coerceIn(0.01f, 1.0f)

            samples.add(
                AudioFrameSample(
                    timestampSec = sec,
                    rmsDb = clampedDb,
                    peakAmplitude = linearAmp,
                    chatVelocity = chatVelocity.coerceIn(0f, 1f)
                )
            )
        }

        return AudioEnvelopeBuffer(
            metadata = metadata,
            sampleRateHz = 48_000,
            windowStepSec = 1,
            samples = samples
        )
    }

    private fun extractCreatorHandle(url: String): String {
        val atMatch = Regex("@([A-Za-z0-9_.-]+)").find(url)?.groupValues?.getOrNull(1)
        if (!atMatch.isNullOrBlank()) return atMatch
        val segment = url
            .removePrefix("https://")
            .removePrefix("http://")
            .split("/")
            .lastOrNull { it.isNotBlank() && !it.startsWith("watch") && it != "live" }
            ?.substringBefore("?")
            ?.take(20)
        return if (!segment.isNullOrBlank() && !segment.contains(".")) segment else "ViceCityStreamer"
    }

    private fun extractVideoId(url: String): String {
        val vParam = Regex("[?&]v=([A-Za-z0-9_-]+)").find(url)?.groupValues?.getOrNull(1)
        if (!vParam.isNullOrBlank()) return vParam
        return abs(url.hashCode()).toString(16).take(8)
    }

    private data class BurstAnchor(
        val centerSec: Int,
        val intensityDb: Float,
        val chatBoost: Float
    )
}
