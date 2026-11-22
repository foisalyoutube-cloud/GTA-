package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CaptionStyle
import com.example.data.model.ClipGenerationSettings
import com.example.data.model.DetectedMoment
import com.example.data.model.FacecamPosition
import com.example.data.model.FramingConfig
import com.example.data.model.FramingLayer
import com.example.data.model.HookUiState
import com.example.data.model.PipelineState
import com.example.data.model.ProcessingMode
import com.example.data.model.VerticalLayoutMode
import com.example.ui.components.PipelineProgressCard
import com.example.ui.components.VerticalClipCanvas
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberHype
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CobaltOnContainer
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.SlateBorder
import kotlinx.coroutines.launch

@Composable
fun ClipStudioScreen(
    pipelineState: PipelineState,
    clipSettings: ClipGenerationSettings,
    detectedMoments: List<DetectedMoment>,
    selectedMoment: DetectedMoment?,
    isPreviewOpen: Boolean,
    framingConfig: FramingConfig,
    hookUiState: HookUiState,
    onUrlChange: (String) -> Unit,
    onSampleUrlSelect: (String) -> Unit,
    onLocalVideoSelected: (String) -> Unit,
    onUpdateClipCount: (Int) -> Unit,
    onUpdateDurationRange: (Int, Int) -> Unit,
    onSetProcessingMode: (ProcessingMode) -> Unit,
    onSetQualityLabel: (String) -> Unit,
    onSetCaptionStyle: (CaptionStyle) -> Unit,
    onSetFacecamPosition: (FacecamPosition) -> Unit,
    onClipNow: () -> Unit,
    onStartLiveClipping: () -> Unit,
    onSelectMoment: (DetectedMoment) -> Unit,
    onGetPreview: (DetectedMoment) -> Unit,
    onSelectFramingLayer: (FramingLayer) -> Unit,
    onSetLayoutMode: (VerticalLayoutMode) -> Unit,
    onSplitRatioChange: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onBoxWidthChange: (Float) -> Unit,
    onBoxHeightChange: (Float) -> Unit,
    onPanDelta: (Float, Float) -> Unit,
    onToggleHookCaption: (Boolean) -> Unit,
    onResetFraming: () -> Unit,
    onGenerateAiHook: () -> Unit,
    onUseFallbackHook: () -> Unit,
    onCreateClip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compactScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            var displayName = "creator_upload.mp4"
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) displayName = cursor.getString(idx) ?: displayName
                    }
                }
            onLocalVideoSelected(displayName)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    UrlClipperCard(
                        pipelineState = pipelineState,
                        clipSettings = clipSettings,
                        onUrlChange = onUrlChange,
                        onSampleUrlSelect = onSampleUrlSelect,
                        onUploadMp4Click = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        onUpdateClipCount = onUpdateClipCount,
                        onUpdateDurationRange = onUpdateDurationRange,
                        onSetProcessingMode = onSetProcessingMode,
                        onSetQualityLabel = onSetQualityLabel,
                        onSetCaptionStyle = onSetCaptionStyle,
                        onSetFacecamPosition = onSetFacecamPosition,
                        onClipNow = onClipNow,
                        onStartLiveClipping = onStartLiveClipping
                    )
                    PipelineProgressCard(pipelineState = pipelineState)
                    BestMomentsSection(
                        moments = detectedMoments,
                        selectedMoment = selectedMoment,
                        onSelectMoment = onSelectMoment,
                        onGetPreview = onGetPreview
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PreviewAndFramingSection(
                        selectedMoment = selectedMoment,
                        isPreviewOpen = isPreviewOpen,
                        framingConfig = framingConfig,
                        hookUiState = hookUiState,
                        onSelectFramingLayer = onSelectFramingLayer,
                        onSetLayoutMode = onSetLayoutMode,
                        onSplitRatioChange = onSplitRatioChange,
                        onZoomChange = onZoomChange,
                        onBoxWidthChange = onBoxWidthChange,
                        onBoxHeightChange = onBoxHeightChange,
                        onPanDelta = onPanDelta,
                        onToggleHookCaption = onToggleHookCaption,
                        onResetFraming = onResetFraming,
                        onGenerateAiHook = onGenerateAiHook,
                        onUseFallbackHook = onUseFallbackHook,
                        onCreateClip = onCreateClip
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(compactScrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UrlClipperCard(
                    pipelineState = pipelineState,
                    clipSettings = clipSettings,
                    onUrlChange = onUrlChange,
                    onSampleUrlSelect = onSampleUrlSelect,
                    onUploadMp4Click = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onUpdateClipCount = onUpdateClipCount,
                    onUpdateDurationRange = onUpdateDurationRange,
                    onSetProcessingMode = onSetProcessingMode,
                    onSetQualityLabel = onSetQualityLabel,
                    onSetCaptionStyle = onSetCaptionStyle,
                    onSetFacecamPosition = onSetFacecamPosition,
                    onClipNow = onClipNow,
                    onStartLiveClipping = onStartLiveClipping
                )
                PipelineProgressCard(pipelineState = pipelineState)
                BestMomentsSection(
                    moments = detectedMoments,
                    selectedMoment = selectedMoment,
                    onSelectMoment = onSelectMoment,
                    onGetPreview = { moment ->
                        onGetPreview(moment)
                        coroutineScope.launch {
                            compactScrollState.animateScrollTo(compactScrollState.maxValue / 2)
                        }
                    }
                )
                PreviewAndFramingSection(
                    selectedMoment = selectedMoment,
                    isPreviewOpen = isPreviewOpen,
                    framingConfig = framingConfig,
                    hookUiState = hookUiState,
                    onSelectFramingLayer = onSelectFramingLayer,
                    onSetLayoutMode = onSetLayoutMode,
                    onSplitRatioChange = onSplitRatioChange,
                    onZoomChange = onZoomChange,
                    onBoxWidthChange = onBoxWidthChange,
                    onBoxHeightChange = onBoxHeightChange,
                    onPanDelta = onPanDelta,
                    onToggleHookCaption = onToggleHookCaption,
                    onResetFraming = onResetFraming,
                    onGenerateAiHook = onGenerateAiHook,
                    onUseFallbackHook = onUseFallbackHook,
                    onCreateClip = onCreateClip
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UrlClipperCard(
    pipelineState: PipelineState,
    clipSettings: ClipGenerationSettings,
    onUrlChange: (String) -> Unit,
    onSampleUrlSelect: (String) -> Unit,
    onUploadMp4Click: () -> Unit,
    onUpdateClipCount: (Int) -> Unit,
    onUpdateDurationRange: (Int, Int) -> Unit,
    onSetProcessingMode: (ProcessingMode) -> Unit,
    onSetQualityLabel: (String) -> Unit,
    onSetCaptionStyle: (CaptionStyle) -> Unit,
    onSetFacecamPosition: (FacecamPosition) -> Unit,
    onClipNow: () -> Unit,
    onStartLiveClipping: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SlateBorder),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bring a Source",
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedButton(
                    onClick = onUploadMp4Click,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("upload_mp4_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Upload MP4")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Paste a YouTube, Twitch or Kick link — or upload an MP4 from your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = pipelineState.sourceUrl,
                onValueChange = onUrlChange,
                label = { Text("YouTube, Twitch, Kick URL or Local MP4") },
                placeholder = { Text("https://youtube.com/watch?v=...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = "Stream URL"
                    )
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("url_input_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AssistChip(
                    onClick = { onSampleUrlSelect("https://www.youtube.com/watch?v=QdBZY2fkU-0") },
                    label = { Text("90s Quick Test") }
                )
                AssistChip(
                    onClick = { onSampleUrlSelect("https://youtube.com/@ViceCityVortex/live") },
                    label = { Text("Vortex Live") }
                )
                AssistChip(
                    onClick = { onSampleUrlSelect("https://twitch.tv/leonidadrift") },
                    label = { Text("Twitch VOD") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clip Settings panel matching PeakCut Page 9 & NeonClipping Step 9
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Clip Settings (${clipSettings.clipCount} clips • ${clipSettings.minLengthSec}-${clipSettings.maxLengthSec}s • ${clipSettings.qualityLabel})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "How many clips",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { onUpdateClipCount(-1) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease clip count")
                            }
                            Text(
                                text = clipSettings.clipCount.toString(),
                                style = MonoMetricStyle
                            )
                            IconButton(
                                onClick = { onUpdateClipCount(1) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase clip count")
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = clipSettings.minLengthSec == 15 && clipSettings.maxLengthSec == 45,
                            onClick = { onUpdateDurationRange(15, 45) },
                            label = { Text("15-45s") }
                        )
                        FilterChip(
                            selected = clipSettings.minLengthSec == 30 && clipSettings.maxLengthSec == 60,
                            onClick = { onUpdateDurationRange(30, 60) },
                            label = { Text("30-60s") }
                        )
                        FilterChip(
                            selected = clipSettings.qualityLabel == "720p",
                            onClick = { onSetQualityLabel("720p") },
                            label = { Text("720p") }
                        )
                        FilterChip(
                            selected = clipSettings.qualityLabel == "1080p",
                            onClick = { onSetQualityLabel("1080p") },
                            label = { Text("1080p") }
                        )
                        ProcessingMode.entries.forEach { mode ->
                            FilterChip(
                                selected = clipSettings.processingMode == mode,
                                onClick = { onSetProcessingMode(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Caption Style",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CaptionStyle.entries.forEach { style ->
                            FilterChip(
                                selected = clipSettings.captionStyle == style,
                                onClick = { onSetCaptionStyle(style) },
                                label = { Text(style.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Facecam Position",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FacecamPosition.entries.forEach { pos ->
                            FilterChip(
                                selected = clipSettings.facecamPosition == pos,
                                onClick = { onSetFacecamPosition(pos) },
                                label = { Text(pos.label) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onClipNow,
                    enabled = !pipelineState.isRunning,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("clip_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCut,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clip Now", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                FilledTonalButton(
                    onClick = onStartLiveClipping,
                    enabled = !pipelineState.isRunning,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CobaltContainer,
                        contentColor = CobaltOnContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("start_live_clipping_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RadioButtonChecked,
                        contentDescription = null,
                        tint = CrimsonLive,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Live Clipping", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun BestMomentsSection(
    moments: List<DetectedMoment>,
    selectedMoment: DetectedMoment?,
    onSelectMoment: (DetectedMoment) -> Unit,
    onGetPreview: (DetectedMoment) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SlateBorder),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Best Moments Finder",
                    style = MaterialTheme.typography.titleLarge
                )
                Surface(
                    color = CobaltContainer,
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text(
                        text = "${moments.size} PEAKS",
                        color = CobaltOnContainer,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any moment to select or press Get Preview to load it into the 9:16 framing studio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                moments.forEachIndexed { index, moment ->
                    val isSelected = selectedMoment?.id == moment.id
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) CobaltContainer.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) CobaltPrimary else SlateBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onSelectMoment(moment) }
                            .testTag("moment_card_$index")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = AmberContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Bolt,
                                            contentDescription = null,
                                            tint = AmberHype,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = moment.hypeTag,
                                            color = AmberOnContainer,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                Text(
                                    text = "${moment.startTime} - ${moment.endTime} (${moment.durationSec}s)",
                                    style = MonoMetricStyle.copy(
                                        fontSize = MaterialTheme.typography.labelSmall.fontSize
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = moment.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Audio peak waveform visualizer
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    moment.waveform.forEach { level ->
                                        Box(
                                            modifier = Modifier
                                                .width(5.dp)
                                                .height((level * 22).dp.coerceAtLeast(4.dp))
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (level > 0.85f) AmberHype else CobaltPrimary.copy(alpha = 0.7f)
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Hype ${moment.energyScore}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                FilledTonalButton(
                                    onClick = { onGetPreview(moment) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("get_preview_button_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Get Preview")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewAndFramingSection(
    selectedMoment: DetectedMoment?,
    isPreviewOpen: Boolean,
    framingConfig: FramingConfig,
    hookUiState: HookUiState,
    onSelectFramingLayer: (FramingLayer) -> Unit,
    onSetLayoutMode: (VerticalLayoutMode) -> Unit,
    onSplitRatioChange: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onBoxWidthChange: (Float) -> Unit,
    onBoxHeightChange: (Float) -> Unit,
    onPanDelta: (Float, Float) -> Unit,
    onToggleHookCaption: (Boolean) -> Unit,
    onResetFraming: () -> Unit,
    onGenerateAiHook: () -> Unit,
    onUseFallbackHook: () -> Unit,
    onCreateClip: () -> Unit
) {
    val activeMoment = selectedMoment ?: return
    val generatedHook = (hookUiState as? HookUiState.Success)?.result

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, SlateBorder),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "9:16 Framing Studio",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Frame Facecam & Gameplay before exporting",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onResetFraming,
                    modifier = Modifier.testTag("reset_framing_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reset framing",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(visible = isPreviewOpen) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VerticalClipCanvas(
                        framing = framingConfig,
                        clipTitle = generatedHook?.viralTitle ?: activeMoment.title,
                        hypeTag = activeMoment.hypeTag,
                        creatorName = activeMoment.creatorName,
                        timestampWindow = "${activeMoment.startTime} - ${activeMoment.endTime}",
                        viralHookText = generatedHook?.openingHook,
                        sceneHueSeed = activeMoment.sceneHueSeed,
                        isEditing = true,
                        isPlaying = true,
                        onPanDelta = onPanDelta,
                        modifier = Modifier.widthIn(max = 260.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Layer selector: Facecam Area vs Gameplay Area
            Text(
                text = "Select Area to Frame",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = framingConfig.activeLayer == FramingLayer.FACECAM,
                    onClick = { onSelectFramingLayer(FramingLayer.FACECAM) },
                    label = { Text("Facecam Area") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Face,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_facecam_area")
                )
                FilterChip(
                    selected = framingConfig.activeLayer == FramingLayer.GAMEPLAY,
                    onClick = { onSelectFramingLayer(FramingLayer.GAMEPLAY) },
                    label = { Text("Gameplay Area") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_gameplay_area")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Layout mode toggle
            Text(
                text = "Layout Preset",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VerticalLayoutMode.entries.forEach { mode ->
                    FilterChip(
                        selected = framingConfig.layoutMode == mode,
                        onClick = { onSetLayoutMode(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (framingConfig.layoutMode == VerticalLayoutMode.SPLIT_STACK) {
                val facePct = (framingConfig.splitRatio * 100).toInt()
                SliderControlRow(
                    label = "Split Ratio (Cam $facePct% / Game ${100 - facePct}%)",
                    value = framingConfig.splitRatio,
                    valueRange = 0.22f..0.54f,
                    onValueChange = onSplitRatioChange,
                    testTag = "split_ratio_slider"
                )
            }

            val isFacecam = framingConfig.activeLayer == FramingLayer.FACECAM
            val activeZoom = if (isFacecam) framingConfig.facecamZoom else framingConfig.gameplayZoom
            val activeBoxW = if (isFacecam) framingConfig.facecamBoxWidth else framingConfig.gameplayBoxWidth
            val activeBoxH = if (isFacecam) framingConfig.facecamBoxHeight else framingConfig.gameplayBoxHeight

            SliderControlRow(
                label = "${framingConfig.activeLayer.label} Zoom (${String.format("%.2fx", activeZoom)})",
                value = activeZoom,
                valueRange = 0.8f..2.0f,
                onValueChange = onZoomChange,
                testTag = "active_zoom_slider"
            )

            SliderControlRow(
                label = "Crop Width (${(activeBoxW * 100).toInt()}%)",
                value = activeBoxW,
                valueRange = 0.45f..1.0f,
                onValueChange = onBoxWidthChange,
                testTag = "crop_width_slider"
            )

            SliderControlRow(
                label = "Crop Height (${(activeBoxH * 100).toInt()}%)",
                value = activeBoxH,
                valueRange = 0.45f..1.0f,
                onValueChange = onBoxHeightChange,
                testTag = "crop_height_slider"
            )

            // Fine Pan Offset Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pan Offset (${framingConfig.activeLayer.label})",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { onPanDelta(-0.12f, 0f) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Pan left")
                    }
                    IconButton(onClick = { onPanDelta(0f, -0.12f) }) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Pan up")
                    }
                    IconButton(onClick = { onPanDelta(0f, 0.12f) }) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Pan down")
                    }
                    IconButton(onClick = { onPanDelta(0.12f, 0f) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Pan right")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show Hook & Hype Caption Overlay",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = framingConfig.showHookCaption,
                    onCheckedChange = onToggleHookCaption
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Viral Hook & Title Helper
            AiViralHookCard(
                hookUiState = hookUiState,
                onGenerateAiHook = onGenerateAiHook,
                onUseFallbackHook = onUseFallbackHook
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onCreateClip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("create_clip_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.MovieCreation,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Clip", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SliderControlRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiViralHookCard(
    hookUiState: HookUiState,
    onGenerateAiHook: () -> Unit,
    onUseFallbackHook: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = CobaltPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "AI Viral Hook & Title Helper",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onGenerateAiHook,
                    enabled = hookUiState !is HookUiState.Loading,
                    modifier = Modifier
                        .weight(1.25f)
                        .height(48.dp)
                        .testTag("generate_ai_hook_button")
                ) {
                    if (hookUiState is HookUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generating...")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gemini Hook")
                    }
                }

                OutlinedButton(
                    onClick = onUseFallbackHook,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("fallback_hook_button")
                ) {
                    Text("Smart Preset")
                }
            }

            when (hookUiState) {
                HookUiState.Idle -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Generate a scroll-stopping title, 3-second hook, and hashtags for this moment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HookUiState.Loading -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Asking Gemini 3.8 Flash for viral hooks...",
                        style = MaterialTheme.typography.bodySmall,
                        color = CobaltPrimary
                    )
                }
                is HookUiState.Success -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = EmeraldContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = hookUiState.result.generatorSource,
                            color = EmeraldOnContainer,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = hookUiState.result.viralTitle,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "\"${hookUiState.result.openingHook}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        hookUiState.result.hashtags.forEach { tag ->
                            Surface(
                                color = CobaltContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = CobaltOnContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
                is HookUiState.Error -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = hookUiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tip: Tap Smart Preset above to use the built-in hook generator offline.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
