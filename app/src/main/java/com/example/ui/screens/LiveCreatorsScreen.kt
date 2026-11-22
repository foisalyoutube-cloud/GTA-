package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.LiveCreatorEntity
import com.example.data.local.LiveLogEntity
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberHype
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CobaltOnContainer
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CrimsonOnContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SlateBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveCreatorsScreen(
    creators: List<LiveCreatorEntity>,
    logs: List<LiveLogEntity>,
    isMonitorActive: Boolean,
    onToggleMonitoring: () -> Unit,
    onAddCreator: (String, String, String) -> Unit,
    onToggleCreatorAutoClip: (LiveCreatorEntity) -> Unit,
    onDeleteCreator: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var creatorName by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("YouTube Live") }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Host session advisory banner
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = AmberContainer,
                border = BorderStroke(1.dp, AmberHype.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Host session requirement",
                        tint = AmberOnContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Host Session Required for Live Monitoring",
                            style = MaterialTheme.typography.titleSmall,
                            color = AmberOnContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.host_session_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberOnContainer
                        )
                    }
                }
            }
        }

        // Live Auto-Clipping Engine Control Card
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isMonitorActive) EmeraldSuccess else MaterialTheme.colorScheme.outline)
                            )
                            Text(
                                text = if (isMonitorActive) "Live Auto-Clipping: ACTIVE" else "Live Auto-Clipping: STANDBY",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val enabledCount = creators.count { it.autoClipEnabled }
                        Text(
                            text = if (isMonitorActive) {
                                "Watching $enabledCount enabled creator(s) — auto-creating 9:16 shorts on audio peaks."
                            } else {
                                "$enabledCount creator(s) armed. Start engine to monitor live streams."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onToggleMonitoring,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMonitorActive) CrimsonLive else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("toggle_live_engine_button")
                    ) {
                        Icon(
                            imageVector = if (isMonitorActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMonitorActive) "Stop" else "Start")
                    }
                }
            }
        }

        // Add Creator Form
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add Live Creator",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = creatorName,
                        onValueChange = { creatorName = it },
                        label = { Text("Creator Channel Name") },
                        placeholder = { Text("e.g. ViceCityKing") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creator_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text("Live Stream URL") },
                        placeholder = { Text("https://youtube.com/@creator/live") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creator_url_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("YouTube Live", "Twitch", "Kick").forEach { platform ->
                            FilterChip(
                                selected = selectedPlatform == platform,
                                onClick = { selectedPlatform = platform },
                                label = { Text(platform, maxLines = 1) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onAddCreator(creatorName, selectedPlatform, streamUrl)
                            if (creatorName.isNotBlank() && streamUrl.isNotBlank()) {
                                creatorName = ""
                                streamUrl = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_creator_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Live Watchlist")
                    }
                }
            }
        }

        item {
            Text(
                text = "Monitored Creators (${creators.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(creators, key = { it.id }) { creator ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LiveTv,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = creator.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = if (creator.isCurrentlyLive) CrimsonContainer else CobaltContainer,
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                Text(
                                    text = if (creator.isCurrentlyLive) "LIVE" else "OFFLINE",
                                    color = if (creator.isCurrentlyLive) CrimsonOnContainer else CobaltOnContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${creator.platform} • ${creator.streamUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Auto-Clip on Live: ${if (creator.autoClipEnabled) "ON" else "OFF"} • ${creator.clipsGeneratedCount} clips",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (creator.autoClipEnabled) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = creator.autoClipEnabled,
                            onCheckedChange = { onToggleCreatorAutoClip(creator) },
                            modifier = Modifier.testTag("autoclip_switch_${creator.id}")
                        )
                        IconButton(
                            onClick = { onDeleteCreator(creator.id) },
                            modifier = Modifier.testTag("delete_creator_${creator.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Remove ${creator.name}",
                                tint = CrimsonLive
                            )
                        }
                    }
                }
            }
        }

        // Live Clipping Queue / Log
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Live Clipping Queue & Log",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(logs, key = { it.id }) { log ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = if (log.status == "CLIPPED") EmeraldContainer else CobaltContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = log.status,
                                    color = if (log.status == "CLIPPED") EmeraldOnContainer else CobaltOnContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = log.creatorName,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = log.eventMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!log.clipTitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Saved: ${log.clipTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = timeFormatter.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
