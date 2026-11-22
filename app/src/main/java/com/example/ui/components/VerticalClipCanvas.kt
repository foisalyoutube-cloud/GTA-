package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FramingConfig
import com.example.data.model.FramingLayer
import com.example.data.model.VerticalLayoutMode
import com.example.ui.theme.AmberHype
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.FacecamFrameAccent
import com.example.ui.theme.GameplayFrameAccent
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.ViewportStage
import kotlin.math.sin

@Composable
fun VerticalClipCanvas(
    framing: FramingConfig,
    clipTitle: String,
    hypeTag: String,
    creatorName: String,
    timestampWindow: String,
    viralHookText: String?,
    sceneHueSeed: Int,
    isEditing: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onPanDelta: ((Float, Float) -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clip_anim")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val phase = if (isPlaying) animPhase else 0.32f

    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(22.dp))
            .background(ViewportStage)
            .border(
                width = 2.dp,
                color = if (isEditing) CobaltPrimary else Color(0xFF334155),
                shape = RoundedCornerShape(22.dp)
            )
            .then(
                if (isEditing && onPanDelta != null) {
                    Modifier
                        .testTag("vertical_preview_canvas")
                        .pointerInput(framing.activeLayer) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / (size.width.coerceAtLeast(1) * 0.6f)
                                val dy = dragAmount.y / (size.height.coerceAtLeast(1) * 0.6f)
                                onPanDelta(dx, dy)
                            }
                        }
                } else {
                    Modifier.testTag("vertical_preview_canvas")
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (framing.layoutMode) {
                VerticalLayoutMode.SPLIT_STACK -> {
                    val splitY = h * framing.splitRatio
                    drawFacecamLayer(
                        bounds = Rect(0f, 0f, w, splitY),
                        framing = framing,
                        phase = phase
                    )
                    drawGameplayLayer(
                        bounds = Rect(0f, splitY, w, h),
                        framing = framing,
                        phase = phase,
                        seed = sceneHueSeed
                    )
                    drawLine(
                        color = FacecamFrameAccent,
                        start = Offset(0f, splitY),
                        end = Offset(w, splitY),
                        strokeWidth = 4f
                    )
                    if (isEditing) {
                        drawCropGuideBox(
                            zone = Rect(0f, 0f, w, splitY),
                            boxWFraction = framing.facecamBoxWidth,
                            boxHFraction = framing.facecamBoxHeight,
                            isActive = framing.activeLayer == FramingLayer.FACECAM,
                            accent = FacecamFrameAccent
                        )
                        drawCropGuideBox(
                            zone = Rect(0f, splitY, w, h),
                            boxWFraction = framing.gameplayBoxWidth,
                            boxHFraction = framing.gameplayBoxHeight,
                            isActive = framing.activeLayer == FramingLayer.GAMEPLAY,
                            accent = GameplayFrameAccent
                        )
                    }
                }
                VerticalLayoutMode.PIP_CORNER -> {
                    drawGameplayLayer(
                        bounds = Rect(0f, 0f, w, h),
                        framing = framing,
                        phase = phase,
                        seed = sceneHueSeed
                    )
                    val pos = framing.facecamPosition
                    if (pos != com.example.data.model.FacecamPosition.NONE) {
                        val pipW = w * 0.38f
                        val pipH = pipW * 0.82f
                        val pipRect = when (pos) {
                            com.example.data.model.FacecamPosition.TOP_RIGHT ->
                                Rect(w * 0.94f - pipW, h * 0.08f, w * 0.94f, h * 0.08f + pipH)
                            com.example.data.model.FacecamPosition.BOTTOM_LEFT ->
                                Rect(w * 0.06f, h * 0.68f - pipH, w * 0.06f + pipW, h * 0.68f)
                            com.example.data.model.FacecamPosition.BOTTOM_RIGHT ->
                                Rect(w * 0.94f - pipW, h * 0.68f - pipH, w * 0.94f, h * 0.68f)
                            com.example.data.model.FacecamPosition.BOTTOM_CENTER ->
                                Rect((w - pipW) / 2f, h * 0.68f - pipH, (w + pipW) / 2f, h * 0.68f)
                            else ->
                                Rect(w * 0.06f, h * 0.08f, w * 0.06f + pipW, h * 0.08f + pipH)
                        }
                        clipRect(pipRect.left, pipRect.top, pipRect.right, pipRect.bottom) {
                            drawFacecamLayer(
                                bounds = pipRect,
                                framing = framing,
                                phase = phase
                            )
                        }
                        drawRoundRect(
                            color = if (isEditing && framing.activeLayer == FramingLayer.FACECAM) {
                                FacecamFrameAccent
                            } else {
                                Color.White.copy(alpha = 0.85f)
                            },
                            topLeft = Offset(pipRect.left, pipRect.top),
                            size = Size(pipRect.width, pipRect.height),
                            cornerRadius = CornerRadius(18f, 18f),
                            style = Stroke(width = 4f)
                        )
                    }
                    if (isEditing && framing.activeLayer == FramingLayer.GAMEPLAY) {
                        drawCropGuideBox(
                            zone = Rect(0f, 0f, w, h),
                            boxWFraction = framing.gameplayBoxWidth,
                            boxHFraction = framing.gameplayBoxHeight,
                            isActive = true,
                            accent = GameplayFrameAccent
                        )
                    }
                }
                VerticalLayoutMode.BLURRED_FILL -> {
                    drawGameplayLayer(
                        bounds = Rect(0f, 0f, w, h),
                        framing = framing,
                        phase = phase,
                        seed = sceneHueSeed
                    )
                    drawRect(
                        color = Color(0xFF0F172A).copy(alpha = 0.58f),
                        topLeft = Offset.Zero,
                        size = Size(w, h)
                    )
                    val centerRect = Rect(0f, h * 0.28f, w, h * 0.72f)
                    drawGameplayLayer(
                        bounds = centerRect,
                        framing = framing,
                        phase = phase,
                        seed = sceneHueSeed
                    )
                    drawRect(
                        color = GameplayFrameAccent,
                        topLeft = Offset(centerRect.left, centerRect.top),
                        size = Size(centerRect.width, centerRect.height),
                        style = Stroke(width = 3f)
                    )
                }
                VerticalLayoutMode.ZOOMED_IN,
                VerticalLayoutMode.FILL_SCREEN -> {
                    val boosted = if (framing.layoutMode == VerticalLayoutMode.ZOOMED_IN) {
                        framing.copy(gameplayZoom = maxOf(framing.gameplayZoom, 1.55f))
                    } else {
                        framing
                    }
                    drawGameplayLayer(
                        bounds = Rect(0f, 0f, w, h),
                        framing = boosted,
                        phase = phase,
                        seed = sceneHueSeed
                    )
                }
            }
        }

        // Top HUD badges inside 9:16 canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(99.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) CrimsonLive else AmberHype)
                    )
                    Text(
                        text = if (isEditing) "FRAMING 9:16" else "9:16 SHORT",
                        color = Color.White,
                        style = MonoMetricStyle.copy(fontSize = 10.sp)
                    )
                }
            }

            Surface(
                color = CobaltPrimary.copy(alpha = 0.88f),
                shape = RoundedCornerShape(99.dp)
            ) {
                Text(
                    text = timestampWindow,
                    color = Color.White,
                    style = MonoMetricStyle.copy(fontSize = 10.sp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Active crop label badge when editing
        if (isEditing) {
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.82f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Drag to pan ${framing.activeLayer.label}",
                    color = Color(0xFFBAE6FD),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Bottom caption & viral hook overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (framing.showHookCaption) {
                Surface(
                    color = AmberHype,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = hypeTag.uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.82f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = clipTitle,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!viralHookText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = viralHookText,
                                color = Color(0xFFCBD5E1),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "@$creatorName • GTA VI CLIPPING",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun DrawScope.drawFacecamLayer(
    bounds: Rect,
    framing: FramingConfig,
    phase: Float
) {
    clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A)),
                start = Offset(bounds.left, bounds.top),
                end = Offset(bounds.right, bounds.bottom)
            ),
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height)
        )

        val cx = bounds.center.x + framing.facecamPanX * bounds.width * 0.28f
        val cy = bounds.center.y + framing.facecamPanY * bounds.height * 0.28f
        val baseRadius = minOf(bounds.width, bounds.height) * 0.24f * framing.facecamZoom

        // Studio RGB backlight ring
        val pulse = 1f + 0.05f * sin(phase * Math.PI.toFloat() * 2f)
        drawCircle(
            color = FacecamFrameAccent.copy(alpha = 0.28f),
            radius = baseRadius * 1.45f * pulse,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = FacecamFrameAccent,
            radius = baseRadius * 1.15f,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )

        // Streamer silhouette + headset arc
        drawCircle(
            color = Color(0xFFE2E8F0),
            radius = baseRadius * 0.46f,
            center = Offset(cx, cy - baseRadius * 0.16f)
        )
        drawArc(
            color = CobaltPrimary,
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(cx - baseRadius * 0.56f, cy - baseRadius * 0.70f),
            size = Size(baseRadius * 1.12f, baseRadius * 1.0f),
            style = Stroke(width = 6f)
        )
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(cx - baseRadius * 0.78f, cy + baseRadius * 0.34f),
            size = Size(baseRadius * 1.56f, baseRadius * 0.9f),
            cornerRadius = CornerRadius(baseRadius * 0.45f, baseRadius * 0.45f)
        )
    }
}

private fun DrawScope.drawGameplayLayer(
    bounds: Rect,
    framing: FramingConfig,
    phase: Float,
    seed: Int
) {
    val skyTop = when (seed % 3) {
        0 -> Color(0xFF1D4ED8)
        1 -> Color(0xFF7C3AED)
        else -> Color(0xFF0369A1)
    }
    val skyHorizon = when (seed % 3) {
        0 -> Color(0xFFF97316)
        1 -> Color(0xFFEC4899)
        else -> Color(0xFF06B6D4)
    }

    clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyHorizon, Color(0xFF0F172A)),
                startY = bounds.top,
                endY = bounds.bottom
            ),
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height)
        )

        val panX = framing.gameplayPanX * bounds.width * 0.25f
        val panY = framing.gameplayPanY * bounds.height * 0.22f
        val zoom = framing.gameplayZoom
        val horizonY = bounds.top + bounds.height * 0.42f + panY

        // Vice City skyline silhouettes
        val skylineColor = Color(0xFF0F172A).copy(alpha = 0.75f)
        val buildingCount = 7
        val buildingW = (bounds.width * zoom) / buildingCount
        for (i in 0 until buildingCount) {
            val bh = ((i * 37 + seed * 19) % 55 + 28) * zoom
            val bx = bounds.left + panX + i * buildingW - (bounds.width * (zoom - 1f) * 0.5f)
            drawRect(
                color = skylineColor,
                topLeft = Offset(bx, horizonY - bh),
                size = Size(buildingW * 0.82f, bh)
            )
        }

        // Perspective Ocean Drive highway
        val roadCenter = bounds.center.x + panX
        val roadTopHalf = bounds.width * 0.08f * zoom
        val roadBottomHalf = bounds.width * 0.56f * zoom
        val roadPath = Path().apply {
            moveTo(roadCenter - roadTopHalf, horizonY)
            lineTo(roadCenter + roadTopHalf, horizonY)
            lineTo(roadCenter + roadBottomHalf, bounds.bottom)
            lineTo(roadCenter - roadBottomHalf, bounds.bottom)
            close()
        }
        drawPath(roadPath, color = Color(0xFF1E293B))

        // Animated dashed center highway markings
        val dashSteps = 5
        for (i in 0 until dashSteps) {
            val t1 = ((i + phase) / dashSteps).coerceIn(0f, 1f)
            val t2 = (t1 + 0.08f).coerceIn(0f, 1f)
            val y1 = horizonY + (bounds.bottom - horizonY) * (t1 * t1)
            val y2 = horizonY + (bounds.bottom - horizonY) * (t2 * t2)
            drawLine(
                color = Color(0xFFFBBF24),
                start = Offset(roadCenter, y1),
                end = Offset(roadCenter, y2),
                strokeWidth = 3f + t1 * 6f
            )
        }

        // Stylized sports car tail-lights streak
        val carY = bounds.top + bounds.height * 0.74f + panY * 0.5f
        val carW = bounds.width * 0.24f * zoom
        drawRoundRect(
            color = Color(0xFF0B1120),
            topLeft = Offset(roadCenter - carW * 0.5f, carY),
            size = Size(carW, carW * 0.42f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawCircle(
            color = CrimsonLive,
            radius = 5f * zoom,
            center = Offset(roadCenter - carW * 0.34f, carY + carW * 0.22f)
        )
        drawCircle(
            color = CrimsonLive,
            radius = 5f * zoom,
            center = Offset(roadCenter + carW * 0.34f, carY + carW * 0.22f)
        )
    }
}

private fun DrawScope.drawCropGuideBox(
    zone: Rect,
    boxWFraction: Float,
    boxHFraction: Float,
    isActive: Boolean,
    accent: Color
) {
    val bw = zone.width * boxWFraction
    val bh = zone.height * boxHFraction
    val left = zone.center.x - bw / 2f
    val top = zone.center.y - bh / 2f
    drawRoundRect(
        color = accent.copy(alpha = if (isActive) 0.95f else 0.45f),
        topLeft = Offset(left, top),
        size = Size(bw, bh),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = if (isActive) 4f else 2f)
    )
}
