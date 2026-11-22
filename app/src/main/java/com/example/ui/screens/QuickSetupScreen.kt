package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SetupChecklistItem
import com.example.data.model.SetupToolProfile
import com.example.data.model.TargetOsPlatform
import com.example.data.repository.SetupGuideRepository
import com.example.ui.components.SetupGuideDocumentDialog
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CobaltOnContainer
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.ViewportStage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSetupScreen(
    repoUrl: String,
    selectedProfile: SetupToolProfile,
    selectedOs: TargetOsPlatform,
    checklist: List<SetupChecklistItem>,
    onRepoUrlChange: (String) -> Unit,
    onSelectProfile: (SetupToolProfile) -> Unit,
    onSelectOs: (TargetOsPlatform) -> Unit,
    onToggleChecklistItem: (String) -> Unit,
    onCopiedMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var showDocumentDialog by remember { mutableStateOf(false) }
    var openDialogOnTxt by remember { mutableStateOf(false) }
    var expandedGuideIds by remember { mutableStateOf(setOf("01 / THE ROUTE", "STEP 0-3")) }
    var showTroubleshoot by remember { mutableStateOf(false) }

    val neonPromptText = remember { SetupGuideRepository.loadNeonClippingPrompt(context) }
    val activeAgentPrompt = if (selectedProfile == SetupToolProfile.NEONCLIPPING) {
        neonPromptText
    } else {
        SetupGuideRepository.peakCutFullAgentPrompt
    }

    val terminalCommands = remember(selectedProfile, selectedOs, repoUrl) {
        SetupGuideRepository.getTerminalCommands(selectedProfile, selectedOs, repoUrl)
    }

    val guideSections = if (selectedProfile == SetupToolProfile.NEONCLIPPING) {
        SetupGuideRepository.neonClippingGuideSections
    } else {
        SetupGuideRepository.peakCutFieldGuideSections
    }

    val completedCount = checklist.count { it.checked }
    val completionFraction = if (checklist.isEmpty()) 0f else completedCount.toFloat() / checklist.size

    if (showDocumentDialog) {
        SetupGuideDocumentDialog(
            initialShowTxt = openDialogOnTxt,
            onDismiss = { showDocumentDialog = false },
            onCopiedMessage = onCopiedMessage
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Source Setup Guide PDF & TXT Viewer Banner
        item {
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
                                text = "Complete Setup Guide & Prompts",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "PeakCut 15-page Field Guide PDF + NeonClipping Hinglish Agent Prompt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                openDialogOnTxt = false
                                showDocumentDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_pdf_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View PDF Guide")
                        }

                        FilledTonalButton(
                            onClick = {
                                openDialogOnTxt = true
                                showDocumentDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_txt_prompt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hinglish Prompt")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select Setup Profile & OS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SetupToolProfile.entries.forEach { profile ->
                            FilterChip(
                                selected = selectedProfile == profile,
                                onClick = { onSelectProfile(profile) },
                                label = { Text(profile.label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TargetOsPlatform.entries.forEach { os ->
                            FilterChip(
                                selected = selectedOs == os,
                                onClick = { onSelectOs(os) },
                                label = { Text(os.label) }
                            )
                        }
                    }
                }
            }
        }

        // 2. Copyable Agent Prompt Card (PeakCut Parts 1-3 or NeonClipping Hinglish Prompt)
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = CobaltPrimary
                            )
                            Column {
                                Text(
                                    text = if (selectedProfile == SetupToolProfile.NEONCLIPPING) {
                                        "NeonClipping Agent Prompt (Hinglish)"
                                    } else {
                                        "PeakCut Agent Prompt (Parts 1–3)"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Paste once into Claude Code, Codex, or your coding agent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = ViewportStage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = activeAgentPrompt.lines().take(10).joinToString("\n") + "\n...",
                            color = Color(0xFFE2E8F0),
                            style = MonoCodeStyle,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(activeAgentPrompt))
                            onCopiedMessage("Full ${selectedProfile.label} agent prompt copied!")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("copy_agent_prompt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Full Agent Prompt")
                    }
                }
            }
        }

        // 3. Connect Local AI Brain (Ollama Settings from Page 7 & Step 4)
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "04 / Connect Local AI Brain",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            color = EmeraldContainer,
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(
                                text = "Ready • No Key Needed",
                                color = EmeraldOnContainer,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Service: Ollama (on this machine)\n" +
                            "• Address: http://127.0.0.1:11434/v1\n" +
                            "• Model ID: llama3.2:3b (ollama pull llama3.2:3b)\n" +
                            "• Verify: Click Save and test -> green Ready status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. OS Terminal Commands & Custom Repo URL
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Terminal,
                            contentDescription = null,
                            tint = CobaltPrimary
                        )
                        Text(
                            text = "${selectedOs.label} Terminal Setup",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (selectedProfile == SetupToolProfile.CUSTOM) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = repoUrl,
                            onValueChange = onRepoUrlChange,
                            label = { Text("Custom Repository URL") },
                            placeholder = { Text("https://github.com/haziqahmedqasmi/PeakCut") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("repo_url_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = ViewportStage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = terminalCommands,
                            color = Color(0xFFE2E8F0),
                            style = MonoCodeStyle,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(terminalCommands))
                            onCopiedMessage("${selectedOs.label} setup commands copied!")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("copy_repo_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy ${selectedOs.label} Terminal Commands")
                    }
                }
            }
        }

        // 5. Step-by-Step Field Guide Cards
        item {
            Text(
                text = if (selectedProfile == SetupToolProfile.NEONCLIPPING) {
                    "NeonClipping Hinglish Walkthrough"
                } else {
                    "PeakCut Step-by-Step Field Guide"
                },
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(guideSections, key = { it.number }) { section ->
            val isExpanded = expandedGuideIds.contains(section.number)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable {
                        expandedGuideIds = if (isExpanded) {
                            expandedGuideIds - section.number
                        } else {
                            expandedGuideIds + section.number
                        }
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = CobaltContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = section.number,
                                    color = CobaltOnContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = section.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            section.details.forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Troubleshoot by Stage (Page 14)
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { showTroubleshoot = !showTroubleshoot }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                color = AmberContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Build,
                                    contentDescription = null,
                                    tint = AmberOnContainer,
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "11 / Troubleshoot by Stage",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Tap to view fixes for ports, models, FFmpeg & blocked URLs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (showTroubleshoot) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle troubleshooting"
                        )
                    }

                    AnimatedVisibility(visible = showTroubleshoot) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SetupGuideRepository.troubleshootStages.forEach { item ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = item.stage,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = item.fix,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Launch & Vyro Campaign Checklist
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Launch & Campaign Checklist",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Surface(
                            color = CobaltContainer,
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(
                                text = "$completedCount / ${checklist.size}",
                                color = CobaltOnContainer,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { completionFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = if (completedCount == checklist.size) EmeraldSuccess else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        items(checklist, key = { it.id }) { item ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onToggleChecklistItem(item.id) }
                    .testTag("checklist_item_${item.id}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (item.checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                        contentDescription = if (item.checked) "Checked" else "Unchecked",
                        tint = if (item.checked) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (item.checked) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
