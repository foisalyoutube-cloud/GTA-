package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PipelineStage
import com.example.data.model.PipelineState
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CobaltOnContainer
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.SlateBorder

@Composable
fun PipelineProgressCard(
    pipelineState: PipelineState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = pipelineState.progressPercent / 100f,
        animationSpec = tween(durationMillis = 320),
        label = "pipeline_progress"
    )

    val isDone = pipelineState.stage == PipelineStage.COMPLETED
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pipeline_progress_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SlateBorder),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (isDone) EmeraldContainer else CobaltContainer,
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SlowMotionVideo,
                                contentDescription = "Pipeline mode",
                                tint = if (isDone) EmeraldOnContainer else CobaltOnContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (pipelineState.isLiveMode) "LIVE PIPELINE" else "SIMULATED PIPELINE",
                                color = if (isDone) EmeraldOnContainer else CobaltOnContainer,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text(
                    text = "${pipelineState.progressPercent}%",
                    style = MonoMetricStyle.copy(
                        color = if (isDone) EmeraldSuccess else CobaltPrimary
                    ),
                    modifier = Modifier.testTag("pipeline_percentage_text")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .testTag("pipeline_progress_bar"),
                color = if (isDone) EmeraldSuccess else CobaltPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pipelineState.statusDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            val stages = listOf(
                PipelineStage.READING_MEDIA,
                PipelineStage.ANALYZING_PEAKS,
                PipelineStage.RENDERING_VERTICAL
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                stages.forEach { step ->
                    val completed = pipelineState.stage.stepNumber > step.stepNumber ||
                        pipelineState.stage == PipelineStage.COMPLETED
                    val active = pipelineState.stage == step
                    PipelineStepRow(
                        stepNumber = step.stepNumber,
                        label = step.label,
                        isCompleted = completed,
                        isActive = active
                    )
                }
            }
        }
    }
}

@Composable
private fun PipelineStepRow(
    stepNumber: Int,
    label: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    val tint: Color = when {
        isCompleted -> EmeraldSuccess
        isActive -> CobaltPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (isCompleted) "Step $stepNumber complete" else "Step $stepNumber pending",
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "$stepNumber) $label",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isActive || isCompleted) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isActive || isCompleted) MaterialTheme.colorScheme.onSurface else tint
        )
    }
}
