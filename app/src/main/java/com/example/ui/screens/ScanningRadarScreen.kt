package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.Dark3DButton
import com.example.ui.components.Gold3DButton
import com.example.ui.components.Gold3DIconButton
import com.example.ui.components.Luxury3DCard
import com.example.ui.components.Radar3DScanner
import com.example.ui.components.StatusPill
import com.example.ui.theme.BgMain
import com.example.ui.theme.CardSecondary
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BrohiViewModel

@Composable
fun ScanningRadarScreen(
    viewModel: BrohiViewModel,
    onBack: () -> Unit,
    onScanComplete: () -> Unit
) {
    val state by viewModel.scanningState.collectAsStateWithLifecycle()

    LaunchedEffect(state.completedSuccess) {
        if (state.completedSuccess) {
            onScanComplete()
        }
    }

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
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Gold3DIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = {
                            viewModel.cancelAiScan()
                            onBack()
                        },
                        testTag = "radar_back_button"
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Viral Radar",
                            style = MaterialTheme.typography.headlineMedium,
                            color = GoldLight
                        )
                        Text(
                            text = "Gemini Multimodal Detection",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                StatusPill(
                    label = when {
                        state.errorMessage != null -> "Error"
                        state.isScanning -> "Scanning"
                        state.completedSuccess -> "Complete"
                        else -> "Standby"
                    },
                    color = when {
                        state.errorMessage != null -> StatusError
                        state.isScanning -> GoldPrimary
                        else -> StatusSuccess
                    }
                )
            }

            Radar3DScanner(
                isScanning = state.isScanning,
                detectedMomentAngles = state.blipAngles,
                modifier = Modifier.size(270.dp)
            )

            Luxury3DCard(
                modifier = Modifier.fillMaxWidth(),
                goldBorder = true
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress: ${state.progressPercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldLight
                        )
                        Text(
                            text = "${formatSeconds(state.scannedSec)} / ${formatSeconds(state.totalSec)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (state.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = GoldPrimary,
                        trackColor = CardSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Detected Moments",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${state.detectedCount}",
                            style = MaterialTheme.typography.titleLarge,
                            color = StatusSuccess
                        )
                    }

                    Text(
                        text = state.errorMessage ?: state.statusLog,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.errorMessage != null) StatusError else TextPrimary,
                        textAlign = TextAlign.Start
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.errorMessage != null) {
                    Gold3DButton(
                        text = "Retry AI Scan",
                        icon = Icons.Filled.Refresh,
                        onClick = { viewModel.startAiScan(onCompleted = {}) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "radar_retry_button"
                    )
                }
                Dark3DButton(
                    text = if (state.isScanning) "Stop & Cancel Scan" else "Back to Config",
                    icon = Icons.Filled.Stop,
                    accentColor = StatusError,
                    onClick = {
                        viewModel.cancelAiScan()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "radar_stop_button"
                )
            }
        }
    }
}

fun formatSeconds(sec: Int): String {
    val m = (sec.coerceAtLeast(0)) / 60
    val s = (sec.coerceAtLeast(0)) % 60
    return "%02d:%02d".format(m, s)
}
