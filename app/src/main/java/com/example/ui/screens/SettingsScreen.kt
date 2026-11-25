package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.AiTestResult
import com.example.data.model.AiVoiceOption
import com.example.data.model.CaptionStyle
import com.example.data.model.GeminiModelOption
import com.example.data.model.TargetLanguage
import com.example.data.model.VoiceMode
import com.example.data.network.TwitchResult
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
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BrohiViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: BrohiViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settingsFlow.collectAsStateWithLifecycle()
    val engineStatus by viewModel.engineStatus.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var twitchClientId by remember { mutableStateOf(viewModel.getTwitchClientId()) }
    var twitchClientSecret by remember { mutableStateOf(viewModel.getTwitchClientSecret()) }
    var aiTestStatus by remember { mutableStateOf<String?>(null) }
    var aiTestOk by remember { mutableStateOf(true) }
    var twitchTestStatus by remember { mutableStateOf<String?>(null) }
    var twitchTestOk by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.refreshStorageStats()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A150E), BgMain)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
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
                        testTag = "settings_back_button"
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "BROHI CLIPPING Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            color = GoldLight
                        )
                        Text(
                            text = "AI Engine, Encrypted Keys & Storage",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Gemini AI Section (Firebase AI Logic)
            item {
                Luxury3DCard(modifier = Modifier.fillMaxWidth(), goldBorder = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gemini AI Engine", style = MaterialTheme.typography.titleLarge, color = GoldLight)
                            StatusPill(label = "App Check Active", color = StatusSuccess)
                        }
                        Text(
                            text = "Powered by Firebase AI Logic — no manual Gemini API key is needed. Select your preferred Gemini 3/4 model below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        GeminiModelOption.entries.forEach { opt ->
                            val selected = settings.geminiModelId == opt.modelId
                            Luxury3DCard(
                                goldBorder = selected,
                                onClick = {
                                    viewModel.updateSettings { it.copy(geminiModelId = opt.modelId) }
                                },
                                contentPadding = PaddingValues(12.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(opt.displayName, style = MaterialTheme.typography.titleMedium, color = if (selected) GoldLight else TextPrimary)
                                        Text(opt.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    }
                                    if (selected) StatusPill(label = "Selected", color = GoldPrimary)
                                }
                            }
                        }

                        Dark3DButton(
                            text = "Test Gemini AI Connection",
                            icon = Icons.Filled.NetworkCheck,
                            accentColor = StatusSuccess,
                            onClick = {
                                aiTestStatus = "Testing Firebase AI Logic (${settings.geminiModelId})..."
                                scope.launch {
                                    when (val res = viewModel.testGeminiAi(settings.geminiModelId)) {
                                        is AiTestResult.Success -> {
                                            aiTestOk = true
                                            aiTestStatus = res.message
                                        }
                                        is AiTestResult.Failure -> {
                                            aiTestOk = false
                                            aiTestStatus = res.error
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "settings_test_gemini_button"
                        )
                        if (!aiTestStatus.isNullOrBlank()) {
                            Text(
                                text = aiTestStatus.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (aiTestOk) StatusSuccess else StatusError
                            )
                        }
                    }
                }
            }

            // Twitch Helix API Section (Android Keystore encrypted)
            item {
                Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Twitch Helix API", style = MaterialTheme.typography.titleLarge, color = GoldLight)
                            StatusPill(label = "Keystore AES-256", color = ActionBlue)
                        }

                        OutlinedTextField(
                            value = twitchClientId,
                            onValueChange = { twitchClientId = it },
                            label = { Text("Twitch Client ID", color = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_twitch_client_id"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = GoldEdge.copy(alpha = 0.6f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = twitchClientSecret,
                            onValueChange = { twitchClientSecret = it },
                            label = { Text("Twitch Client Secret", color = TextSecondary) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_twitch_client_secret"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = GoldEdge.copy(alpha = 0.6f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Gold3DButton(
                                text = "Save Keys",
                                icon = Icons.Filled.Save,
                                onClick = {
                                    viewModel.saveTwitchCredentials(twitchClientId, twitchClientSecret)
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_save_twitch_button"
                            )
                            Dark3DButton(
                                text = "Test Twitch",
                                icon = Icons.Filled.Key,
                                accentColor = ActionBlue,
                                onClick = {
                                    viewModel.saveTwitchCredentials(twitchClientId, twitchClientSecret)
                                    twitchTestStatus = "Authenticating with Twitch OAuth..."
                                    scope.launch {
                                        when (val res = viewModel.testTwitchConnection()) {
                                            is TwitchResult.Success -> {
                                                twitchTestOk = true
                                                twitchTestStatus = res.data
                                            }
                                            is TwitchResult.MissingCredentials -> {
                                                twitchTestOk = false
                                                twitchTestStatus = res.message
                                            }
                                            is TwitchResult.Error -> {
                                                twitchTestOk = false
                                                twitchTestStatus = res.message
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "settings_test_twitch_button"
                            )
                        }

                        if (!twitchTestStatus.isNullOrBlank()) {
                            Text(
                                text = twitchTestStatus.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (twitchTestOk) StatusSuccess else StatusError
                            )
                        }
                    }
                }
            }

            // Default Preferences
            item {
                Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Default Studio Preferences", style = MaterialTheme.typography.titleLarge, color = GoldLight)

                        Text("Default Language", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TargetLanguage.entries.forEach { lang ->
                                SelectableGoldChip(
                                    label = lang.displayName,
                                    selected = settings.defaultLanguage == lang.name,
                                    onClick = { viewModel.updateSettings { it.copy(defaultLanguage = lang.name) } }
                                )
                            }
                        }

                        Text("Default Voice", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AiVoiceOption.entries.forEach { voice ->
                                SelectableGoldChip(
                                    label = voice.displayName,
                                    selected = settings.defaultVoice == voice.name,
                                    onClick = { viewModel.updateSettings { it.copy(defaultVoice = voice.name) } }
                                )
                            }
                        }

                        Text("Default Caption Style", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CaptionStyle.entries.forEach { style ->
                                SelectableGoldChip(
                                    label = style.displayName,
                                    selected = settings.defaultCaptionStyle == style.name,
                                    onClick = { viewModel.updateSettings { it.copy(defaultCaptionStyle = style.name) } }
                                )
                            }
                        }

                        Text("Default Voice Mode", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VoiceMode.entries.forEach { mode ->
                                SelectableGoldChip(
                                    label = mode.displayName,
                                    selected = settings.defaultVoiceMode == mode.name,
                                    onClick = { viewModel.updateSettings { it.copy(defaultVoiceMode = mode.name) } }
                                )
                            }
                        }
                    }
                }
            }

            // Video Engine Diagnostics
            item {
                Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Media3 Video Engine", style = MaterialTheme.typography.titleLarge, color = GoldLight)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Prefer Hardware Encoding", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("Falls back automatically to software codec if unavailable", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            Switch(
                                checked = settings.preferHardwareEncoder,
                                onCheckedChange = { checked ->
                                    viewModel.updateSettings { it.copy(preferHardwareEncoder = checked) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BgMain,
                                    checkedTrackColor = GoldPrimary,
                                    uncheckedTrackColor = CardSecondary
                                )
                            )
                        }
                        Text(
                            text = "HW Video Encoder: ${engineStatus.hardwareVideoEncoderName ?: "Software Fallback"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "SW Fallback: ${engineStatus.softwareVideoEncoderName ?: "Available"} • AAC Audio: ${if (engineStatus.audioAacEncoderAvailable) "Ready" else "Unavailable"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Storage & Cache
            item {
                Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Storage & Cache", style = MaterialTheme.typography.titleLarge, color = GoldLight)
                        Text(
                            text = "Vault Shorts: ${formatBytes(storageStats.shortsBytes)} • Studio Exports: ${formatBytes(storageStats.exportsBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Temporary Cache: ${formatBytes(storageStats.tempCacheBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Dark3DButton(
                            text = "Clear Temporary Cache",
                            icon = Icons.Filled.CleaningServices,
                            accentColor = ActionBlue,
                            onClick = { viewModel.clearTempCache() },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "settings_clear_cache_button"
                        )
                    }
                }
            }

            // About & Privacy
            item {
                Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("About BROHI CLIPPING", style = MaterialTheme.typography.titleLarge, color = GoldLight)
                        Text("Version 1.0 • Native Kotlin, Jetpack Compose & Media3 Transformer", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(
                            text = "Zero mandatory user accounts. Videos and Room metadata stay on your device. Audio chunks and stream metadata are transmitted securely via Firebase AI Logic only when you start AI scanning.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.2f MB".format(bytes / (1024f * 1024f))
    }
}
