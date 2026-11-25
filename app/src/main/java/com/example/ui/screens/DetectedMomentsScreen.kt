package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DetectedMoment
import com.example.ui.components.Dark3DButton
import com.example.ui.components.Gold3DButton
import com.example.ui.components.Gold3DIconButton
import com.example.ui.components.Luxury3DCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.ActionBlue
import com.example.ui.theme.BgMain
import com.example.ui.theme.CardSecondary
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldEdge
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BrohiViewModel

@Composable
fun DetectedMomentsScreen(
    viewModel: BrohiViewModel,
    onBack: () -> Unit,
    onOpenStudio: (shortId: String) -> Unit,
    onFinishedToVault: () -> Unit
) {
    val moments by viewModel.detectedMoments.collectAsStateWithLifecycle()
    val progress by viewModel.generationProgress.collectAsStateWithLifecycle()
    val selectedCount = moments.count { it.isSelected }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A150E), BgMain)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Gold3DIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        testTag = "moments_back_button"
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Detected Moments",
                            style = MaterialTheme.typography.headlineMedium,
                            color = GoldLight
                        )
                        Text(
                            text = "$selectedCount of ${moments.size} selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Gold3DIconButton(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort by Viral Score",
                    onClick = { viewModel.sortMomentsByViralScore() },
                    testTag = "moments_sort_button"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Dark3DButton(
                    text = "Select All",
                    icon = Icons.Filled.CheckBox,
                    onClick = { viewModel.selectAllMoments(true) },
                    modifier = Modifier.weight(1f),
                    height = 44.dp,
                    testTag = "moments_select_all_button"
                )
                Dark3DButton(
                    text = "Deselect All",
                    icon = Icons.Filled.CheckBoxOutlineBlank,
                    accentColor = TextSecondary,
                    onClick = { viewModel.selectAllMoments(false) },
                    modifier = Modifier.weight(1f),
                    height = 44.dp,
                    testTag = "moments_deselect_all_button"
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(moments, key = { it.id }) { moment ->
                    MomentCard3D(
                        moment = moment,
                        onToggle = { viewModel.toggleMomentSelection(moment.id) }
                    )
                }
            }

            val activeProgress = progress
            if (activeProgress != null) {
                Luxury3DCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    goldBorder = true
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stage ${activeProgress.stageIndex}/${activeProgress.totalStages}: ${activeProgress.stageName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (activeProgress.errorMessage != null) StatusError else GoldLight
                            )
                            Text(
                                text = "${activeProgress.percent}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = GoldPrimary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (activeProgress.percent / 100f).coerceIn(0f, 1f) },
                            color = if (activeProgress.errorMessage != null) StatusError else GoldPrimary,
                            trackColor = CardSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                        )
                        Text(
                            text = activeProgress.errorMessage ?: activeProgress.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        if (activeProgress.errorMessage != null) {
                            TextButton(onClick = { viewModel.clearGenerationError() }) {
                                Text("Dismiss", color = GoldPrimary)
                            }
                        } else {
                            Dark3DButton(
                                text = "Cancel Rendering",
                                icon = Icons.Filled.Close,
                                accentColor = StatusError,
                                onClick = { viewModel.cancelGeneration() },
                                modifier = Modifier.fillMaxWidth(),
                                height = 44.dp
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.padding(16.dp)) {
                    Gold3DButton(
                        text = "Generate $selectedCount Selected Clip(s)",
                        icon = Icons.Filled.AutoFixHigh,
                        enabled = selectedCount > 0,
                        onClick = {
                            viewModel.generateSelectedShorts { firstId ->
                                if (firstId != null) {
                                    onOpenStudio(firstId)
                                } else {
                                    onFinishedToVault()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "moments_generate_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun MomentCard3D(
    moment: DetectedMoment,
    onToggle: () -> Unit
) {
    Luxury3DCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("moment_card_${moment.id}"),
        goldBorder = moment.isSelected,
        onClick = onToggle
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(GoldLight, GoldPrimary, GoldDark)))
                            .border(1.dp, GoldEdge, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${moment.viralScore}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = BgMain
                        )
                    }
                    Column {
                        StatusPill(
                            label = "${formatSeconds(moment.startSec)} → ${formatSeconds(moment.endSec)} (${moment.durationSec}s)",
                            color = ActionBlue
                        )
                    }
                }

                Checkbox(
                    checked = moment.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = GoldPrimary,
                        uncheckedColor = GoldEdge,
                        checkmarkColor = BgMain
                    )
                )
            }

            Text(
                text = moment.title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = moment.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (moment.narrationScript.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgMain.copy(alpha = 0.65f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "AI Hook: \"${moment.narrationScript}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldLight
                    )
                }
            }
        }
    }
}
