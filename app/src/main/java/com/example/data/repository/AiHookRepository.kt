package com.example.data.repository

import android.util.Log
import com.example.data.model.ViralHookResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiHookRepository"

class AiHookRepository {

    private val hookSchema = Schema.obj(
        properties = mapOf(
            "viralTitle" to Schema.string(
                description = "Punchy YouTube Shorts / TikTok title under 65 characters"
            ),
            "openingHook" to Schema.string(
                description = "Attention-grabbing 3-second spoken or on-screen hook under 90 characters"
            ),
            "hashtags" to Schema.array(
                items = Schema.string(),
                description = "4 to 5 relevant gaming and shorts hashtags starting with #"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = hookSchema
                temperature = 0.85f
            }
        )
    }

    suspend fun generateGeminiViralHook(
        momentTitle: String,
        hypeTag: String,
        creatorName: String,
        startTime: String,
        endTime: String
    ): Result<ViralHookResult> {
        val prompt = """
            You are a viral gaming Shorts strategist specializing in GTA VI and high-energy live streams.
            Generate a punchy vertical Short title, an irresistible 3-second opening hook, and 5 hashtags for this clip:
            - Moment Title: $momentTitle
            - Hype Category: $hypeTag
            - Creator: $creatorName
            - Timestamp Window: $startTime to $endTime
            Keep the title crisp, energetic, and tailored for YouTube Shorts, Reels, and TikTok.
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            val json = JSONObject(jsonText)
            val title = json.optString("viralTitle").trim()
            val hook = json.optString("openingHook").trim()
            val tagsArray = json.optJSONArray("hashtags")
            val tags = buildList {
                if (tagsArray != null) {
                    for (i in 0 until tagsArray.length()) {
                        val tag = tagsArray.optString(i).trim()
                        if (tag.isNotEmpty()) {
                            add(if (tag.startsWith("#")) tag else "#$tag")
                        }
                    }
                }
            }

            if (title.isBlank() || hook.isBlank()) {
                Result.failure(IllegalStateException("Model returned incomplete viral hook fields."))
            } else {
                Result.success(
                    ViralHookResult(
                        viralTitle = title,
                        openingHook = hook,
                        hashtags = tags.ifEmpty { defaultTags(hypeTag) },
                        generatorSource = "Gemini 3.8 Flash"
                    )
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini viral hook generation timed out", e)
            Result.failure(Exception("Gemini request timed out after 45s. You can retry or use the built-in generator."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini viral hook generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Gemini generation failed."))
        }
    }

    fun generateLocalSmartFallback(
        momentTitle: String,
        hypeTag: String,
        creatorName: String
    ): ViralHookResult {
        val cleanTag = hypeTag.uppercase()
        val title = when {
            cleanTag.contains("CHASE") -> "Vice City 5-Star Escape Was INSANE"
            cleanTag.contains("CLUTCH") -> "$creatorName Pulled Off An Impossible Clutch"
            cleanTag.contains("EXPLOSION") -> "Chat Lost It Over This GTA VI Moment"
            cleanTag.contains("STUNT") -> "Wait Until You See This Rooftop Jump"
            else -> "${momentTitle.take(44)} | Shorts Cut"
        }
        val hook = when {
            cleanTag.contains("CHASE") -> "Nobody survives a 5-star Vice PD ambush like this—watch the final turn!"
            cleanTag.contains("CLUTCH") -> "0.4 seconds left on the clock and $creatorName somehow made it out."
            cleanTag.contains("EXPLOSION") -> "Look at live chat the exact second this happened on stream."
            else -> "Stop scrolling—this $cleanTag clip from $creatorName breaks the physics engine."
        }
        return ViralHookResult(
            viralTitle = title,
            openingHook = hook,
            hashtags = defaultTags(hypeTag),
            generatorSource = "Built-In Smart Generator"
        )
    }

    private fun defaultTags(hypeTag: String): List<String> {
        val normalized = hypeTag.replace(" ", "").replace("-", "")
        return listOf("#GTAVI", "#Shorts", "#GamingClips", "#$normalized", "#ViceCity")
    }
}
