package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AiVoiceOption
import com.example.data.model.TargetLanguage
import com.example.data.model.VoiceMode
import com.example.ui.components.Dark3DButton
import com.example.ui.components.Gold3DButton
import com.example.ui.components.Gold3DIconButton
import com.example.ui.components.Luxury3DCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.ActionBlue
import com.example.ui.theme.BgMain
import com.example.ui.theme.CardSecondary
import com.example.ui.theme.GoldEdge
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BrohiViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamConfigScreen(
    viewModel: BrohiViewModel,
    onBack: () -> Unit,
    onStartScan: () -> Unit
) {
    val scanConfig by viewModel.scanConfig.collectAsStateWithLifecycle()
    val config = scanConfig

    val attachVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.attachLocalVideoToCurrentSession(uri)
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A150E), BgMain)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (config == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No stream selected.", color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Gold3DButton(text = "Return Home", onClick = onBack)
            }
            return@Box
        }

        val meta = config.metadata
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Gold3DIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        testTag = "config_back_button"
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Stream Configuration",
                            style = MaterialTheme.typography.headlineMedium,
                            color = GoldLight
                        )
                        Text(
                            text = "${meta.platform} • Model: ${config.geminiModelId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Stream Preview Card
            item {
                Luxury3DCard(modifier = Modifier.fillMaxWidth(), goldBorder = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!meta.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = meta.thumbnailUrl,
                                contentDescription = meta.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(165.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardSecondary)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                label = if (meta.isDirectPlayable) "Direct Video Ready" else "Metadata Mode",
                                color = if (meta.isDirectPlayable) StatusSuccess else ActionBlue
                            )
                            Text(
                                text = "${meta.durationMs / 1000}s",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldLight
                            )
                        }
                        Text(
                            text = meta.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${meta.streamer} • ${meta.sourceUrl}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!meta.streamNotice.isNullOrBlank()) {
                            Text(
                                text = meta.streamNotice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GoldLight.copy(alpha = 0.9f)
                            )
                            Dark3DButton(
                                text = "Attach Local Recording (Optional)",
                                icon = Icons.Filled.AttachFile,
                                accentColor = ActionBlue,
                                onClick = {
                                    attachVideoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "config_attach_video_button"
                            )
                        }
                    }
                }
            }

            // Target Language
            item {
                Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Target Language", style = MaterialTheme.typography.titleMedium, color = GoldLight)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TargetLanguage.entries.forEach { lang ->
                                val selected = lang == config.language
                                SelectableGoldChip(
                                    label = lang.displayName,
                                    selected = selected,
                                    onClick = { viewModel.updateScanConfig { it.copy(language = lang) } }
                                )
                            }
                        }
                    }
                }
            }

            // Voice Mode
            item {
                Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Commentary Mode", style = MaterialTheme.typography.titleMedium, color = GoldLight)
                        VoiceMode.entries.forEach { mode ->
                            val selected = mode == config.voiceMode
                            Luxury3DCard(
                                goldBorder = selected,
                                onClick = { viewModel.updateScanConfig { it.copy(voiceMode = mode) } },
                                contentPadding = PaddingValues(12.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (selected) GoldLight else TextPrimary
                                        )
                                        Text(
                                            text = mode.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    if (selected) {
                                        StatusPill(label = "Active", color = GoldPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AI Voice Persona
            if (config.voiceMode != VoiceMode.OFF) {
                item {
                    Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("AI Voice Persona", style = MaterialTheme.typography.titleMedium, color = GoldLight)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AiVoiceOption.entries.forEach { voice ->
                                    SelectableGoldChip(
                                        label = voice.displayName,
                                        selected = voice == config.voiceOption,
                                        onClick = { viewModel.updateScanConfig { it.copy(voiceOption = voice) } }
                                    )
                                }
                            }
                            Text(
                                text = "${config.voiceOption.displayName}: ${config.voiceOption.toneHint}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Clip Duration & Count
            item {
                Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Clip Duration", style = MaterialTheme.typography.titleMedium, color = GoldLight)
                            Text("${config.clipDurationSec}s", style = MaterialTheme.typography.labelLarge, color = GoldPrimary)
                        }
                        Slider(
                            value = config.clipDurationSec.toFloat(),
                            onValueChange = { v ->
                                viewModel.updateScanConfig { it.copy(clipDurationSec = v.roundToInt().coerceIn(20, 60)) }
                            },
                            valueRange = 20f..60f,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldPrimary,
                                activeTrackColor = GoldPrimary,
                                inactiveTrackColor = CardSecondary
                            )
                        )

                        Text("Clips to Detect", style = MaterialTheme.typography.titleMedium, color = GoldLight)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(3, 5, 10).forEach { count ->
                                Box(modifier = Modifier.weight(1f)) {
                                    SelectableGoldChip(
                                        label = "$count Clips",
                                        selected = config.clipCount == count,
                                        onClick = { viewModel.updateScanConfig { it.copy(clipCount = count) } },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Privacy disclosure & Start Scanning button
            item {
                Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = ActionBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.privacy_ai_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Gold3DButton(
                    text = "Start AI Scanning",
                    icon = Icons.Filled.Radar,
                    onClick = {
                        viewModel.startAiScan(onCompleted = {})
                        onStartScan()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "config_start_scanning_button"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SelectableGoldChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Luxury3DCard(
        modifier = modifier,
        goldBorder = selected,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) GoldLight else TextSecondary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
