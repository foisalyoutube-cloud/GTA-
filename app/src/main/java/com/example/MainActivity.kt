package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.data.model.StudioTab
import com.example.ui.screens.ClipStudioScreen
import com.example.ui.screens.LiveCreatorsScreen
import com.example.ui.screens.MyClipsScreen
import com.example.ui.screens.QuickSetupScreen
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CobaltOnContainer
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CrimsonOnContainer
import com.example.ui.theme.GtaClippingTheme
import com.example.ui.theme.SlateBorder
import com.example.ui.viewmodel.StudioEvent
import com.example.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            GtaClippingTheme {
                GtaClippingStudioApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GtaClippingStudioApp(
    viewModel: StudioViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val pipelineState by viewModel.pipelineState.collectAsStateWithLifecycle()
    val clipSettings by viewModel.clipSettings.collectAsStateWithLifecycle()
    val detectedMoments by viewModel.detectedMoments.collectAsStateWithLifecycle()
    val selectedMoment by viewModel.selectedMoment.collectAsStateWithLifecycle()
    val isPreviewOpen by viewModel.isPreviewOpen.collectAsStateWithLifecycle()
    val framingConfig by viewModel.framingConfig.collectAsStateWithLifecycle()
    val hookUiState by viewModel.hookUiState.collectAsStateWithLifecycle()
    val savedClips by viewModel.savedClips.collectAsStateWithLifecycle()
    val liveCreators by viewModel.liveCreators.collectAsStateWithLifecycle()
    val liveLogs by viewModel.liveLogs.collectAsStateWithLifecycle()
    val isLiveMonitorActive by viewModel.isLiveMonitorActive.collectAsStateWithLifecycle()
    val activePlaybackClip by viewModel.activePlaybackClip.collectAsStateWithLifecycle()
    val isPlaybackPlaying by viewModel.isPlaybackPlaying.collectAsStateWithLifecycle()
    val repoUrl by viewModel.repoUrl.collectAsStateWithLifecycle()
    val setupToolProfile by viewModel.setupToolProfile.collectAsStateWithLifecycle()
    val targetOs by viewModel.targetOs.collectAsStateWithLifecycle()
    val checklist by viewModel.setupChecklist.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(enabled = selectedTab != StudioTab.CLIP_STUDIO) {
        viewModel.selectTab(StudioTab.CLIP_STUDIO)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StudioEvent.ShowMessage -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed && event.targetTab != null) {
                        viewModel.selectTab(event.targetTab)
                    }
                }
                is StudioEvent.ClipExported -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Saved ${event.result.fileName} to ${event.result.displayLocation}",
                        actionLabel = "Share",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, event.result.shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share 9:16 Clip")
                        )
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    shadowElevation = 0.dp
                ) {
                    Column {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "GTA VI CLIPPING",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            },
                            actions = {
                                if (isLiveMonitorActive) {
                                    Surface(
                                        color = CrimsonContainer,
                                        shape = RoundedCornerShape(99.dp),
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(CrimsonLive)
                                            )
                                            Text(
                                                text = "AUTO-CLIP LIVE",
                                                color = CrimsonOnContainer,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = CobaltContainer,
                                        shape = RoundedCornerShape(99.dp),
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Text(
                                            text = "STUDIO READY",
                                            color = CobaltOnContainer,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SlateBorder)
                        )
                    }
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIcon(
                                        tab = tab,
                                        selected = selected,
                                        clipCount = savedClips.size
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.size(12.dp))
                        StudioTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIcon(
                                        tab = tab,
                                        selected = selected,
                                        clipCount = savedClips.size
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(SlateBorder)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (selectedTab) {
                        StudioTab.CLIP_STUDIO -> ClipStudioScreen(
                            pipelineState = pipelineState,
                            clipSettings = clipSettings,
                            detectedMoments = detectedMoments,
                            selectedMoment = selectedMoment,
                            isPreviewOpen = isPreviewOpen,
                            framingConfig = framingConfig,
                            hookUiState = hookUiState,
                            onUrlChange = viewModel::updateSourceUrl,
                            onSampleUrlSelect = viewModel::applyQuickSampleUrl,
                            onLocalVideoSelected = viewModel::onLocalVideoSelected,
                            onUpdateClipCount = viewModel::updateClipCount,
                            onUpdateDurationRange = viewModel::updateDurationRange,
                            onSetProcessingMode = viewModel::setProcessingMode,
                            onSetQualityLabel = viewModel::setQualityLabel,
                            onSetCaptionStyle = viewModel::setCaptionStyle,
                            onSetFacecamPosition = viewModel::setFacecamPosition,
                            onClipNow = { viewModel.startPipeline(isLiveMode = false) },
                            onStartLiveClipping = { viewModel.startPipeline(isLiveMode = true) },
                            onSelectMoment = viewModel::selectMoment,
                            onGetPreview = viewModel::getPreviewForMoment,
                            onSelectFramingLayer = viewModel::selectFramingLayer,
                            onSetLayoutMode = viewModel::setLayoutMode,
                            onSplitRatioChange = viewModel::updateSplitRatio,
                            onZoomChange = viewModel::updateActiveZoom,
                            onBoxWidthChange = viewModel::updateActiveBoxWidth,
                            onBoxHeightChange = viewModel::updateActiveBoxHeight,
                            onPanDelta = viewModel::panActiveLayer,
                            onToggleHookCaption = viewModel::toggleHookCaption,
                            onResetFraming = viewModel::resetFraming,
                            onGenerateAiHook = viewModel::generateAiViralHookForSelectedMoment,
                            onUseFallbackHook = viewModel::useSmartFallbackHook,
                            onCreateClip = viewModel::createClipFromPreview
                        )

                        StudioTab.LIVE_CREATORS -> LiveCreatorsScreen(
                            creators = liveCreators,
                            logs = liveLogs,
                            isMonitorActive = isLiveMonitorActive,
                            onToggleMonitoring = viewModel::toggleLiveMonitoring,
                            onAddCreator = viewModel::addCreator,
                            onToggleCreatorAutoClip = viewModel::toggleCreatorAutoClip,
                            onDeleteCreator = viewModel::deleteCreator
                        )

                        StudioTab.MY_CLIPS -> MyClipsScreen(
                            clips = savedClips,
                            activePlaybackClip = activePlaybackClip,
                            isPlaying = isPlaybackPlaying,
                            onOpenPlayer = viewModel::openClipPlayer,
                            onTogglePlayback = viewModel::togglePlayback,
                            onClosePlayer = viewModel::closeClipPlayer,
                            onDownloadClip = viewModel::downloadClip,
                            onDeleteClip = viewModel::deleteClip,
                            onRegenerateAiHook = viewModel::regenerateClipHookWithAi,
                            onNavigateToStudio = { viewModel.selectTab(StudioTab.CLIP_STUDIO) }
                        )

                        StudioTab.QUICK_SETUP -> QuickSetupScreen(
                            repoUrl = repoUrl,
                            selectedProfile = setupToolProfile,
                            selectedOs = targetOs,
                            checklist = checklist,
                            onRepoUrlChange = viewModel::updateRepoUrl,
                            onSelectProfile = viewModel::setSetupToolProfile,
                            onSelectOs = viewModel::setTargetOs,
                            onToggleChecklistItem = viewModel::toggleChecklistItem,
                            onCopiedMessage = { msg ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIcon(
    tab: StudioTab,
    selected: Boolean,
    clipCount: Int
) {
    val icon: ImageVector = when (tab) {
        StudioTab.CLIP_STUDIO -> if (selected) Icons.Filled.ContentCut else Icons.Outlined.ContentCut
        StudioTab.LIVE_CREATORS -> if (selected) Icons.Filled.LiveTv else Icons.Outlined.LiveTv
        StudioTab.MY_CLIPS -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
        StudioTab.QUICK_SETUP -> if (selected) Icons.Filled.Terminal else Icons.Outlined.Terminal
    }

    if (tab == StudioTab.MY_CLIPS && clipCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(clipCount.coerceAtMost(99).toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = tab.label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = tab.label)
    }
}
