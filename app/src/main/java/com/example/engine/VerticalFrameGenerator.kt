package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.data.local.ClipEntity
import com.example.data.model.FacecamPosition
import com.example.data.model.FramingConfig
import com.example.data.model.VerticalLayoutMode

class VerticalFrameGenerator {

    fun computeLayoutSpec(
        framing: FramingConfig,
        sourceWidth: Int = 1920,
        sourceHeight: Int = 1080,
        targetWidth: Int = 1080,
        targetHeight: Int = 1920
    ): VerticalFrameLayoutSpec {
        val tw = targetWidth.toFloat()
        val th = targetHeight.toFloat()
        val clampedSplit = framing.splitRatio.coerceIn(0.22f, 0.55f)
        val splitY = if (framing.layoutMode == VerticalLayoutMode.SPLIT_STACK) {
            th * clampedSplit
        } else {
            0f
        }

        val facecamTargetBounds = when (framing.layoutMode) {
            VerticalLayoutMode.SPLIT_STACK -> PixelRect(0f, 0f, tw, splitY)
            VerticalLayoutMode.PIP_CORNER -> computePipRect(tw, th, framing.facecamPosition)
            else -> PixelRect(0f, 0f, 0f, 0f)
        }

        val gameplayTargetBounds = when (framing.layoutMode) {
            VerticalLayoutMode.SPLIT_STACK -> PixelRect(0f, splitY, tw, th)
            VerticalLayoutMode.BLURRED_FILL -> PixelRect(0f, th * 0.28f, tw, th * 0.72f)
            else -> PixelRect(0f, 0f, tw, th)
        }

        val facecamSourceCrop = computeSourceCropRect(
            sourceWidth = sourceWidth.toFloat(),
            sourceHeight = sourceHeight.toFloat(),
            boxWidthFraction = framing.facecamBoxWidth,
            boxHeightFraction = framing.facecamBoxHeight,
            zoom = framing.facecamZoom,
            panX = framing.facecamPanX,
            panY = framing.facecamPanY,
            defaultAnchorX = 0.18f,
            defaultAnchorY = 0.78f
        )

        val effectiveGameplayZoom = when (framing.layoutMode) {
            VerticalLayoutMode.ZOOMED_IN -> maxOf(framing.gameplayZoom, 1.45f)
            VerticalLayoutMode.FILL_SCREEN -> maxOf(framing.gameplayZoom, 1.25f)
            else -> framing.gameplayZoom
        }

        val gameplaySourceCrop = computeSourceCropRect(
            sourceWidth = sourceWidth.toFloat(),
            sourceHeight = sourceHeight.toFloat(),
            boxWidthFraction = framing.gameplayBoxWidth,
            boxHeightFraction = framing.gameplayBoxHeight,
            zoom = effectiveGameplayZoom,
            panX = framing.gameplayPanX,
            panY = framing.gameplayPanY,
            defaultAnchorX = 0.50f,
            defaultAnchorY = 0.50f
        )

        val captionSafeBounds = PixelRect(
            left = 56f,
            top = th - 430f,
            right = tw - 56f,
            bottom = th - 110f
        )

        return VerticalFrameLayoutSpec(
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            layoutMode = framing.layoutMode,
            splitY = splitY,
            facecamTargetBounds = facecamTargetBounds,
            gameplayTargetBounds = gameplayTargetBounds,
            facecamSourceCrop = facecamSourceCrop,
            gameplaySourceCrop = gameplaySourceCrop,
            captionSafeBounds = captionSafeBounds
        )
    }

    private fun computePipRect(tw: Float, th: Float, position: FacecamPosition): PixelRect {
        val pipW = tw * 0.36f
        val pipH = pipW * 0.85f
        val margin = 44f
        return when (position) {
            FacecamPosition.TOP_RIGHT -> PixelRect(tw - margin - pipW, 64f, tw - margin, 64f + pipH)
            FacecamPosition.BOTTOM_LEFT -> PixelRect(margin, th - 470f - pipH, margin + pipW, th - 470f)
            FacecamPosition.BOTTOM_RIGHT -> PixelRect(tw - margin - pipW, th - 470f - pipH, tw - margin, th - 470f)
            FacecamPosition.BOTTOM_CENTER -> PixelRect((tw - pipW) / 2f, th - 470f - pipH, (tw + pipW) / 2f, th - 470f)
            FacecamPosition.NONE -> PixelRect(0f, 0f, 0f, 0f)
            else -> PixelRect(margin, 64f, margin + pipW, 64f + pipH)
        }
    }

    private fun computeSourceCropRect(
        sourceWidth: Float,
        sourceHeight: Float,
        boxWidthFraction: Float,
        boxHeightFraction: Float,
        zoom: Float,
        panX: Float,
        panY: Float,
        defaultAnchorX: Float,
        defaultAnchorY: Float
    ): PixelRect {
        val safeZoom = zoom.coerceIn(0.8f, 2.5f)
        val cropW = (sourceWidth * boxWidthFraction.coerceIn(0.25f, 1f) / safeZoom)
            .coerceIn(64f, sourceWidth)
        val cropH = (sourceHeight * boxHeightFraction.coerceIn(0.25f, 1f) / safeZoom)
            .coerceIn(64f, sourceHeight)

        val maxCenterShiftX = (sourceWidth - cropW) / 2f
        val maxCenterShiftY = (sourceHeight - cropH) / 2f

        val baseCenterX = sourceWidth * defaultAnchorX
        val baseCenterY = sourceHeight * defaultAnchorY
        val centerX = (baseCenterX + panX * maxCenterShiftX).coerceIn(cropW / 2f, sourceWidth - cropW / 2f)
        val centerY = (baseCenterY + panY * maxCenterShiftY).coerceIn(cropH / 2f, sourceHeight - cropH / 2f)

        return PixelRect(
            left = centerX - cropW / 2f,
            top = centerY - cropH / 2f,
            right = centerX + cropW / 2f,
            bottom = centerY + cropH / 2f
        )
    }

    fun renderFrameBitmap(clip: ClipEntity): Bitmap {
        val framing = clip.toFramingConfig()
        val spec = computeLayoutSpec(framing = framing)
        val bitmap = Bitmap.createBitmap(spec.targetWidth, spec.targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
        }
        canvas.drawRect(0f, 0f, spec.targetWidth.toFloat(), spec.targetHeight.toFloat(), bgPaint)

        val facecamRect = spec.facecamTargetBounds.toRectF()
        val gameplayRect = spec.gameplayTargetBounds.toRectF()

        when (spec.layoutMode) {
            VerticalLayoutMode.SPLIT_STACK -> {
                drawFacecamArea(canvas, facecamRect, clip)
                drawGameplayArea(canvas, gameplayRect, clip)

                val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#38BDF8")
                    strokeWidth = 6f
                }
                canvas.drawLine(0f, spec.splitY, spec.targetWidth.toFloat(), spec.splitY, dividerPaint)
            }
            VerticalLayoutMode.PIP_CORNER -> {
                drawGameplayArea(canvas, gameplayRect, clip)
                if (facecamRect.width() > 0f && facecamRect.height() > 0f) {
                    drawFacecamArea(canvas, facecamRect, clip)
                    val pipBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 6f
                        color = Color.parseColor("#38BDF8")
                    }
                    canvas.drawRoundRect(facecamRect, 28f, 28f, pipBorder)
                }
            }
            VerticalLayoutMode.BLURRED_FILL -> {
                // Soft backdrop + crisp centered 16:9 gameplay box
                drawGameplayArea(
                    canvas,
                    RectF(0f, 0f, spec.targetWidth.toFloat(), spec.targetHeight.toFloat()),
                    clip
                )
                val dimOverlay = Paint().apply { color = Color.argb(135, 15, 23, 42) }
                canvas.drawRect(0f, 0f, spec.targetWidth.toFloat(), spec.targetHeight.toFloat(), dimOverlay)
                drawGameplayArea(canvas, gameplayRect, clip)
                val frameBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    color = Color.parseColor("#60A5FA")
                }
                canvas.drawRect(gameplayRect, frameBorder)
            }
            VerticalLayoutMode.ZOOMED_IN,
            VerticalLayoutMode.FILL_SCREEN -> {
                drawGameplayArea(canvas, gameplayRect, clip)
            }
        }

        if (clip.showHookCaption) {
            val h = spec.targetHeight.toFloat()
            val w = spec.targetWidth.toFloat()

            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1D4ED8")
            }
            val badgeRect = RectF(56f, h - 420f, 460f, h - 350f)
            canvas.drawRoundRect(badgeRect, 20f, 20f, badgePaint)

            val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 30f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText(clip.hypeTag.uppercase(), 84f, h - 375f, badgeText)

            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(220, 15, 23, 42)
            }
            val captionRect = RectF(56f, h - 330f, w - 56f, h - 120f)
            canvas.drawRoundRect(captionRect, 28f, 28f, cardPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 44f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText(clip.viralTitle.take(36), 90f, h - 252f, titlePaint)

            val hookPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                textSize = 30f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            canvas.drawText(clip.viralHook.take(52), 90f, h - 188f, hookPaint)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 26f
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText(
            "GTA VI CLIPPING  •  ${clip.creatorName}  •  ${clip.startTime}-${clip.endTime}",
            56f,
            spec.targetHeight - 56f,
            footerPaint
        )
        return bitmap
    }

    private fun drawFacecamArea(canvas: Canvas, bounds: RectF, clip: ClipEntity) {
        canvas.save()
        canvas.clipRect(bounds)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bounds.left, bounds.top, bounds.right, bounds.bottom,
                Color.parseColor("#1E1B4B"),
                Color.parseColor("#0F172A"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(bounds, bg)

        val cx = bounds.centerX() + clip.facecamPanX * 80f
        val cy = bounds.centerY() + clip.facecamPanY * 60f
        val r = (minOf(bounds.width(), bounds.height()) * 0.24f) * clip.facecamZoom.coerceIn(0.8f, 1.8f)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawCircle(cx, cy - r * 0.1f, r * 1.15f, ringPaint)

        val silhouettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
        }
        canvas.drawCircle(cx, cy - r * 0.25f, r * 0.45f, silhouettePaint)
        val shoulders = RectF(cx - r * 0.85f, cy + r * 0.2f, cx + r * 0.85f, cy + r * 1.1f)
        canvas.drawRoundRect(shoulders, r * 0.45f, r * 0.45f, silhouettePaint)
        canvas.restore()
    }

    private fun drawGameplayArea(canvas: Canvas, bounds: RectF, clip: ClipEntity) {
        canvas.save()
        canvas.clipRect(bounds)
        val sky = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bounds.left, bounds.top, bounds.left, bounds.bottom,
                Color.parseColor("#1D4ED8"),
                Color.parseColor("#0F172A"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(bounds, sky)

        val horizonY = bounds.top + bounds.height() * 0.45f + clip.gameplayPanY * 80f
        val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
        }
        val roadPath = Path().apply {
            moveTo(bounds.centerX() - 90f + clip.gameplayPanX * 90f, horizonY)
            lineTo(bounds.centerX() + 90f + clip.gameplayPanX * 90f, horizonY)
            lineTo(bounds.right + 120f, bounds.bottom)
            lineTo(bounds.left - 120f, bounds.bottom)
            close()
        }
        canvas.drawPath(roadPath, roadPaint)

        val lanePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
            strokeWidth = 10f
        }
        canvas.drawLine(
            bounds.centerX() + clip.gameplayPanX * 90f,
            horizonY,
            bounds.centerX() + clip.gameplayPanX * 40f,
            bounds.bottom,
            lanePaint
        )
        canvas.restore()
    }

    private fun PixelRect.toRectF(): RectF = RectF(left, top, right, bottom)
}
