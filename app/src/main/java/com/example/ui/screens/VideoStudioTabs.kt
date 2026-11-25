package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.media.AssSubtitleGenerator
import com.example.data.model.BlurMaskRect
import com.example.data.model.CaptionStyle
import com.example.data.model.EditingConfig
import com.example.data.model.LayoutMode
import com.example.data.model.PipelineProgress
import com.example.data.model.SolidShapeRect
import com.example.data.model.StudioFont
import com.example.ui.components.Dark3DButton
import com.example.ui.components.Gold3DButton
import com.example.ui.components.Luxury3DCard
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
import java.util.UUID

@Composable
fun StudioFramingTab(
    config: EditingConfig,
    onUpdate: ((EditingConfig) -> EditingConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("9:16 Vertical Framing", style = MaterialTheme.typography.titleMedium, color = GoldLight)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LayoutMode.entries.forEach { mode ->
                Box(modifier = Modifier.weight(1f)) {
                    SelectableGoldChip(
                        label = mode.displayName,
                        selected = config.layoutMode == mode,
                        onClick = { onUpdate { it.copy(layoutMode = mode) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        StudioSliderRow(
            label = "Zoom",
            valueText = "%.2fx".format(config.zoom),
            value = config.zoom,
            range = 0.75f..2.5f,
            onValueChange = { z -> onUpdate { it.copy(zoom = z) } }
        )
        StudioSliderRow(
            label = "Pan X",
            valueText = "%.2f".format(config.panX),
            value = config.panX,
            range = -1f..1f,
            onValueChange = { x -> onUpdate { it.copy(panX = x) } }
        )
        StudioSliderRow(
            label = "Pan Y",
            valueText = "%.2f".format(config.panY),
            value = config.panY,
            range = -1f..1f,
            onValueChange = { y -> onUpdate { it.copy(panY = y) } }
        )

        Dark3DButton(
            text = "Reset Framing",
            icon = Icons.Filled.RestartAlt,
            onClick = { onUpdate { it.copy(zoom = 1.0f, panX = 0f, panY = 0f) } },
            modifier = Modifier.fillMaxWidth(),
            height = 44.dp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioCaptionsTab(
    config: EditingConfig,
    captionText: String,
    onCaptionTextChange: (String) -> Unit,
    onUpdate: ((EditingConfig) -> EditingConfig) -> Unit
) {
    val swatches = listOf("#F8F2E4", "#FFC81F", "#35D685", "#4AA8FF", "#FF5B4F", "#FFFFFF")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Karaoke Caption Text", style = MaterialTheme.typography.titleMedium, color = GoldLight)
        OutlinedTextField(
            value = captionText,
            onValueChange = onCaptionTextChange,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = GoldEdge.copy(alpha = 0.6f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Text("Subtitle Style", style = MaterialTheme.typography.titleMedium, color = GoldLight)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CaptionStyle.entries.forEach { style ->
                SelectableGoldChip(
                    label = style.displayName,
                    selected = config.captionStyle == style,
                    onClick = { onUpdate { it.copy(captionStyle = style) } }
                )
            }
        }

        Text("Font Family", style = MaterialTheme.typography.titleMedium, color = GoldLight)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StudioFont.entries.forEach { font ->
                SelectableGoldChip(
                    label = font.displayName,
                    selected = config.fontFamily == font,
                    onClick = { onUpdate { it.copy(fontFamily = font) } }
                )
            }
        }

        StudioSliderRow(
            label = "Font Size",
            valueText = "${config.fontSizeSp.toInt()}sp",
            value = config.fontSizeSp,
            range = 20f..48f,
            onValueChange = { sz -> onUpdate { it.copy(fontSizeSp = sz) } }
        )

        StudioSliderRow(
            label = "Vertical Pos",
            valueText = "${(config.captionPositionY * 100).toInt()}%",
            value = config.captionPositionY,
            range = 0.25f..0.86f,
            onValueChange = { py -> onUpdate { it.copy(captionPositionY = py) } }
        )

        Text("Primary Color", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        ColorSwatchRow(
            swatches = swatches,
            selectedHex = config.primaryColorHex,
            onSelect = { hex -> onUpdate { it.copy(primaryColorHex = hex) } }
        )

        Text("Karaoke Highlight Color", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        ColorSwatchRow(
            swatches = swatches,
            selectedHex = config.highlightColorHex,
            onSelect = { hex -> onUpdate { it.copy(highlightColorHex = hex) } }
        )
    }
}

@Composable
fun StudioAudioTab(
    config: EditingConfig,
    hasVoiceFile: Boolean,
    onPickMusic: () -> Unit,
    onUpdate: ((EditingConfig) -> EditingConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Audio Mixer & Ducking", style = MaterialTheme.typography.titleMedium, color = GoldLight)
        StudioSliderRow(
            label = "Original Stream",
            valueText = "${(config.originalAudioVolume * 100).toInt()}%",
            value = config.originalAudioVolume,
            range = 0f..1.5f,
            onValueChange = { v -> onUpdate { it.copy(originalAudioVolume = v) } }
        )
        StudioSliderRow(
            label = if (hasVoiceFile) "AI Voice" else "AI Voice (No WAV)",
            valueText = "${(config.voiceVolume * 100).toInt()}%",
            value = config.voiceVolume,
            range = 0f..1.5f,
            onValueChange = { v -> onUpdate { it.copy(voiceVolume = v) } }
        )
        StudioSliderRow(
            label = "Background Music",
            valueText = "${(config.musicVolume * 100).toInt()}%",
            value = config.musicVolume,
            range = 0f..1.2f,
            onValueChange = { v -> onUpdate { it.copy(musicVolume = v) } }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto Audio Ducking", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Lowers stream & music while AI voice speaks", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Switch(
                checked = config.duckingEnabled,
                onCheckedChange = { checked -> onUpdate { it.copy(duckingEnabled = checked) } },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BgMain,
                    checkedTrackColor = GoldPrimary,
                    uncheckedTrackColor = CardSecondary
                )
            )
        }

        Dark3DButton(
            text = if (config.musicUri != null) "Change Background Music" else "Add Background Music",
            icon = Icons.Filled.MusicNote,
            accentColor = ActionBlue,
            onClick = onPickMusic,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StudioShapesTab(
    config: EditingConfig,
    selectedOverlayId: String?,
    onSelectOverlay: (String?) -> Unit,
    onUpdate: ((EditingConfig) -> EditingConfig) -> Unit
) {
    val boxColors = listOf("#12100C", "#231E17", "#FFC81F", "#4AA8FF", "#FF5B4F")
    val selectedBlur = config.blurMasks.firstOrNull { it.id == selectedOverlayId }
    val selectedSolid = config.solidShapes.firstOrNull { it.id == selectedOverlayId }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Blur Masks & Solid Shapes", style = MaterialTheme.typography.titleMedium, color = GoldLight)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Dark3DButton(
                text = "Add Blur Box",
                icon = Icons.Filled.BlurOn,
                accentColor = GoldPrimary,
                onClick = {
                    val id = "blur_${UUID.randomUUID().toString().take(6)}"
                    onUpdate { it.copy(blurMasks = it.blurMasks + BlurMaskRect(id = id)) }
                    onSelectOverlay(id)
                },
                modifier = Modifier.weight(1f)
            )
            Dark3DButton(
                text = "Add Solid Box",
                icon = Icons.Filled.AddBox,
                accentColor = ActionBlue,
                onClick = {
                    val id = "solid_${UUID.randomUUID().toString().take(6)}"
                    onUpdate { it.copy(solidShapes = it.solidShapes + SolidShapeRect(id = id)) }
                    onSelectOverlay(id)
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (selectedBlur != null) {
            Luxury3DCard(modifier = Modifier.fillMaxWidth(), goldBorder = true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selected Blur Mask (drag on preview)", style = MaterialTheme.typography.titleMedium, color = GoldLight)
                    StudioSliderRow(
                        label = "Width",
                        valueText = "${(selectedBlur.widthFraction * 100).toInt()}%",
                        value = selectedBlur.widthFraction,
                        range = 0.15f..0.95f,
                        onValueChange = { w ->
                            onUpdate { cfg ->
                                cfg.copy(blurMasks = cfg.blurMasks.map { if (it.id == selectedBlur.id) it.copy(widthFraction = w) else it })
                            }
                        }
                    )
                    StudioSliderRow(
                        label = "Height",
                        valueText = "${(selectedBlur.heightFraction * 100).toInt()}%",
                        value = selectedBlur.heightFraction,
                        range = 0.06f..0.5f,
                        onValueChange = { h ->
                            onUpdate { cfg ->
                                cfg.copy(blurMasks = cfg.blurMasks.map { if (it.id == selectedBlur.id) it.copy(heightFraction = h) else it })
                            }
                        }
                    )
                    Dark3DButton(
                        text = "Delete Blur Box",
                        icon = Icons.Filled.Delete,
                        accentColor = StatusError,
                        onClick = {
                            onUpdate { cfg -> cfg.copy(blurMasks = cfg.blurMasks.filterNot { it.id == selectedBlur.id }) }
                            onSelectOverlay(null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 42.dp
                    )
                }
            }
        } else if (selectedSolid != null) {
            Luxury3DCard(modifier = Modifier.fillMaxWidth(), goldBorder = true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selected Solid Shape (drag on preview)", style = MaterialTheme.typography.titleMedium, color = GoldLight)
                    StudioSliderRow(
                        label = "Width",
                        valueText = "${(selectedSolid.widthFraction * 100).toInt()}%",
                        value = selectedSolid.widthFraction,
                        range = 0.15f..0.95f,
                        onValueChange = { w ->
                            onUpdate { cfg ->
                                cfg.copy(solidShapes = cfg.solidShapes.map { if (it.id == selectedSolid.id) it.copy(widthFraction = w) else it })
                            }
                        }
                    )
                    StudioSliderRow(
                        label = "Height",
                        valueText = "${(selectedSolid.heightFraction * 100).toInt()}%",
                        value = selectedSolid.heightFraction,
                        range = 0.06f..0.5f,
                        onValueChange = { h ->
                            onUpdate { cfg ->
                                cfg.copy(solidShapes = cfg.solidShapes.map { if (it.id == selectedSolid.id) it.copy(heightFraction = h) else it })
                            }
                        }
                    )
                    Text("Box Color", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    ColorSwatchRow(
                        swatches = boxColors,
                        selectedHex = selectedSolid.colorHex,
                        onSelect = { hex ->
                            onUpdate { cfg ->
                                cfg.copy(solidShapes = cfg.solidShapes.map { if (it.id == selectedSolid.id) it.copy(colorHex = hex) else it })
                            }
                        }
                    )
                    Dark3DButton(
                        text = "Delete Solid Shape",
                        icon = Icons.Filled.Delete,
                        accentColor = StatusError,
                        onClick = {
                            onUpdate { cfg -> cfg.copy(solidShapes = cfg.solidShapes.filterNot { it.id == selectedSolid.id }) }
                            onSelectOverlay(null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 42.dp
                    )
                }
            }
        } else {
            Text(
                text = "Tap a Blur or Solid box on the 9:16 preview above to resize, color, or delete it.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun StudioExportTab(
    hasExportedMp4: Boolean,
    exportProgress: PipelineProgress?,
    onSaveDraft: () -> Unit,
    onRenderExport: () -> Unit,
    onCancelExport: () -> Unit,
    onDismissExportStatus: () -> Unit,
    onSaveGallery: () -> Unit,
    onShare: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Render & Export 9:16 MP4", style = MaterialTheme.typography.titleMedium, color = GoldLight)

        if (exportProgress != null) {
            Luxury3DCard(modifier = Modifier.fillMaxWidth(), goldBorder = true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = exportProgress.stageName,
                            style = MaterialTheme.typography.titleMedium,
                            color = when {
                                exportProgress.errorMessage != null -> StatusError
                                exportProgress.isCompleted -> StatusSuccess
                                else -> GoldLight
                            }
                        )
                        Text("${exportProgress.percent}%", style = MaterialTheme.typography.labelLarge, color = GoldPrimary)
                    }
                    LinearProgressIndicator(
                        progress = { (exportProgress.percent / 100f).coerceIn(0f, 1f) },
                        color = if (exportProgress.errorMessage != null) StatusError else GoldPrimary,
                        trackColor = CardSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Text(
                        text = exportProgress.errorMessage ?: exportProgress.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (exportProgress.isCompleted || exportProgress.errorMessage != null) {
                        TextButton(onClick = onDismissExportStatus) {
                            Text("OK", color = GoldPrimary)
                        }
                    } else {
                        Dark3DButton(
                            text = "Cancel Export",
                            icon = Icons.Filled.Close,
                            accentColor = StatusError,
                            onClick = onCancelExport,
                            modifier = Modifier.fillMaxWidth(),
                            height = 42.dp
                        )
                    }
                }
            }
        } else {
            Gold3DButton(
                text = "Render 1080×1920 MP4",
                icon = Icons.Filled.MovieCreation,
                onClick = onRenderExport,
                modifier = Modifier.fillMaxWidth(),
                testTag = "studio_render_mp4_button"
            )
        }

        Dark3DButton(
            text = "Save Draft Configuration",
            icon = Icons.Filled.Save,
            onClick = onSaveDraft,
            modifier = Modifier.fillMaxWidth(),
            testTag = "studio_save_draft_button"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Dark3DButton(
                text = "Save Gallery",
                icon = Icons.Filled.Download,
                accentColor = StatusSuccess,
                enabled = hasExportedMp4,
                onClick = onSaveGallery,
                modifier = Modifier.weight(1f),
                testTag = "studio_save_gallery_button"
            )
            Dark3DButton(
                text = "Share MP4",
                icon = Icons.Filled.Share,
                accentColor = ActionBlue,
                enabled = hasExportedMp4,
                onClick = onShare,
                modifier = Modifier.weight(1f),
                testTag = "studio_share_mp4_button"
            )
        }
    }
}

@Composable
private fun StudioSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = GoldPrimary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = GoldPrimary,
                activeTrackColor = GoldPrimary,
                inactiveTrackColor = CardSecondary
            )
        )
    }
}

@Composable
private fun ColorSwatchRow(
    swatches: List<String>,
    selectedHex: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEach { hex ->
            val selected = hex.equals(selectedHex, ignoreCase = true)
            val colorInt = AssSubtitleGenerator.parseColorSafe(hex, android.graphics.Color.WHITE)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) GoldPrimary else GoldEdge.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}
