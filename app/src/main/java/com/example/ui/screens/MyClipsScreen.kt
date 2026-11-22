package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ClipEntity
import com.example.ui.components.VerticalClipCanvas
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CobaltOnContainer
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.SlateBorder

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyClipsScreen(
    clips: List<ClipEntity>,
    activePlaybackClip: ClipEntity?,
    isPlaying: Boolean,
    onOpenPlayer: (ClipEntity) -> Unit,
    onTogglePlayback: () -> Unit,
    onClosePlayer: () -> Unit,
    onDownloadClip: (ClipEntity) -> Unit,
    onDeleteClip: (ClipEntity) -> Unit,
    onRegenerateAiHook: (ClipEntity) -> Unit,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (clips.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = CobaltContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.VideoLibrary,
                                contentDescription = null,
                                tint = CobaltOnContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Clips Exported Yet",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Run the URL Clipper or enable Live Auto-Clipping to generate framed 9:16 shorts here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onNavigateToStudio,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Open Clip Studio")
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (activePlaybackClip != null) {
            item(key = "active_player_${activePlaybackClip.id}") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "9:16 Clip Playback",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = activePlaybackClip.viralTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = onClosePlayer) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close player"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        VerticalClipCanvas(
                            framing = activePlaybackClip.toFramingConfig(),
                            clipTitle = activePlaybackClip.viralTitle,
                            hypeTag = activePlaybackClip.hypeTag,
                            creatorName = activePlaybackClip.creatorName,
                            timestampWindow = "${activePlaybackClip.startTime} - ${activePlaybackClip.endTime}",
                            viralHookText = activePlaybackClip.viralHook,
                            sceneHueSeed = activePlaybackClip.sceneHueSeed,
                            isEditing = false,
                            isPlaying = isPlaying,
                            modifier = Modifier.widthIn(max = 240.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onTogglePlayback,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlaying) "Pause" else "Play")
                            }

                            Button(
                                onClick = { onDownloadClip(activePlaybackClip) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download")
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Clips (${clips.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Tap any clip to play 9:16 preview",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(clips, key = { it.id }) { clip ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPlayer(clip) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        VerticalClipCanvas(
                            framing = clip.toFramingConfig(),
                            clipTitle = clip.viralTitle,
                            hypeTag = clip.hypeTag,
                            creatorName = clip.creatorName,
                            timestampWindow = clip.startTime,
                            viralHookText = null,
                            sceneHueSeed = clip.sceneHueSeed,
                            isEditing = false,
                            isPlaying = false,
                            modifier = Modifier.width(92.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = AmberContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = clip.hypeTag,
                                        color = AmberOnContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (clip.isAutoClipped) {
                                    Surface(
                                        color = EmeraldContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "AUTO-CLIP",
                                            color = EmeraldOnContainer,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = clip.viralTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = clip.viralHook,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "@${clip.creatorName} • ${clip.startTime}-${clip.endTime} (${clip.durationSec}s)",
                                style = MonoMetricStyle.copy(
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                clip.hashtagList().take(4).forEach { tag ->
                                    Surface(
                                        color = CobaltContainer,
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            color = CobaltOnContainer,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onDownloadClip(clip) },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("download_clip_${clip.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download")
                        }

                        OutlinedButton(
                            onClick = { onOpenPlayer(clip) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("play_clip_${clip.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play")
                        }

                        IconButton(
                            onClick = { onRegenerateAiHook(clip) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("ai_refresh_clip_${clip.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Refresh AI viral hook",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { onDeleteClip(clip) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_clip_${clip.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete clip",
                                tint = CrimsonLive
                            )
                        }
                    }
                }
            }
        }
    }
}
