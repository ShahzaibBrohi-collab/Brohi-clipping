package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.media.AssSubtitleGenerator
import com.example.data.model.CaptionStyle
import com.example.data.model.EditingConfig
import com.example.data.model.LayoutMode
import com.example.data.model.StudioFont
import com.example.ui.components.Dark3DButton
import com.example.ui.components.Gold3DIconButton
import com.example.ui.components.Luxury3DCard
import com.example.ui.theme.ActionBlue
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.BgMain
import com.example.ui.theme.GoldEdge
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.JakartaSansFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BrohiViewModel
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

private enum class StudioTab(val label: String) {
    FRAMING("Framing"),
    CAPTIONS("Captions"),
    AUDIO("Audio"),
    SHAPES("Blur & Shapes"),
    EXPORT("Export")
}

@OptIn(UnstableApi::class)
@Composable
fun VideoStudioScreen(
    viewModel: BrohiViewModel,
    shortId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val record by viewModel.repository.observeShortById(shortId).collectAsStateWithLifecycle(initialValue = null)
    val exportProgress by viewModel.studioExportProgress.collectAsStateWithLifecycle()

    var config by remember { mutableStateOf(EditingConfig()) }
    var captionText by remember { mutableStateOf("") }
    var configInitialized by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(StudioTab.FRAMING) }
    var selectedOverlayId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(record) {
        val loaded = record ?: return@LaunchedEffect
        if (!configInitialized) {
            config = viewModel.repository.decodeConfig(loaded.editingConfigJson)
            captionText = loaded.caption
            configInitialized = true
        }
    }

    val attachVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.attachVideoToShortDraft(shortId, uri)
            }
        }
    )

    val musicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                config = config.copy(musicUri = uri.toString())
                viewModel.showToast("Background music track linked.")
            }
        }
    )

    val playableUri = remember(record?.finalMp4Path, record?.sourceUri) {
        val mp4 = record?.finalMp4Path?.let { File(it) }?.takeIf { it.exists() }
        when {
            mp4 != null -> Uri.fromFile(mp4)
            !record?.sourceUri.isNullOrBlank() -> Uri.parse(record?.sourceUri)
            else -> null
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(playableUri, config.startMs, config.endMs) {
        if (playableUri != null) {
            val item = if (record?.finalMp4Path == null) {
                MediaItem.Builder()
                    .setUri(playableUri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(config.startMs)
                            .setEndPositionMs(config.endMs.coerceAtLeast(config.startMs + 3_000L))
                            .build()
                    )
                    .build()
            } else {
                MediaItem.fromUri(playableUri)
            }
            exoPlayer.setMediaItem(item)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = false
        }
    }

    LaunchedEffect(config.originalAudioVolume) {
        exoPlayer.volume = config.originalAudioVolume.coerceIn(0f, 1f)
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(80L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Recompute live karaoke cues when user edits caption text
    val liveWordCues = remember(captionText, config.startMs, config.endMs) {
        val dur = (config.endMs - config.startMs).coerceAtLeast(3_000L)
        viewModel.geminiService.buildWordCues(captionText, dur)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A150E), BgMain)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Studio Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Gold3DIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        testTag = "studio_back_button"
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "3D Video Studio",
                            style = MaterialTheme.typography.headlineMedium,
                            color = GoldLight
                        )
                        Text(
                            text = record?.title ?: "Loading Draft...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
                Gold3DIconButton(
                    icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Preview" else "Play Preview",
                    onClick = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    testTag = "studio_play_pause_button"
                )
            }

            // 9:16 Vertical Interactive Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF332919), Color(0xFF12100C), Color.Black)
                            )
                        )
                        .border(1.5.dp, GoldEdge, RoundedCornerShape(18.dp))
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextZoom = (config.zoom * zoom).coerceIn(0.75f, 2.5f)
                                val nextPanX = (config.panX + pan.x / 220f).coerceIn(-1f, 1f)
                                val nextPanY = (config.panY + pan.y / 220f).coerceIn(-1f, 1f)
                                config = config.copy(zoom = nextZoom, panX = nextPanX, panY = nextPanY)
                            }
                        }
                ) {
                    val canvasW = constraints.maxWidth.toFloat()
                    val canvasH = constraints.maxHeight.toFloat()
                    val density = LocalDensity.current

                    if (playableUri != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { pv ->
                                pv.player = exoPlayer
                                pv.resizeMode = if (config.layoutMode == LayoutMode.FILL) {
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                } else {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = config.zoom
                                    scaleY = config.zoom
                                    translationX = config.panX * canvasW * 0.35f
                                    translationY = config.panY * canvasH * 0.35f
                                }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Preview Ready for Video",
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldLight,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Dark3DButton(
                                text = "Attach Video",
                                icon = Icons.Filled.AttachFile,
                                accentColor = ActionBlue,
                                onClick = {
                                    attachVideoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                height = 42.dp
                            )
                        }
                    }

                    // Draggable Solid Shapes
                    config.solidShapes.forEach { shape ->
                        val boxW = with(density) { (shape.widthFraction * canvasW).toDp() }
                        val boxH = with(density) { (shape.heightFraction * canvasH).toDp() }
                        val fillInt = AssSubtitleGenerator.parseColorSafe(shape.colorHex, android.graphics.Color.BLACK)
                        val isSelected = selectedOverlayId == shape.id
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (shape.xFraction * canvasW).roundToInt(),
                                        (shape.yFraction * canvasH).roundToInt()
                                    )
                                }
                                .size(boxW, boxH)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(fillInt))
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) GoldPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedOverlayId = shape.id }
                                .pointerInput(shape.id) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        selectedOverlayId = shape.id
                                        config = config.copy(
                                            solidShapes = config.solidShapes.map { item ->
                                                if (item.id == shape.id) {
                                                    item.copy(
                                                        xFraction = (item.xFraction + dragAmount.x / canvasW).coerceIn(0f, 1f - item.widthFraction),
                                                        yFraction = (item.yFraction + dragAmount.y / canvasH).coerceIn(0f, 1f - item.heightFraction)
                                                    )
                                                } else item
                                            }
                                        )
                                    }
                                }
                        )
                    }

                    // Draggable Blur Masks
                    config.blurMasks.forEach { mask ->
                        val boxW = with(density) { (mask.widthFraction * canvasW).toDp() }
                        val boxH = with(density) { (mask.heightFraction * canvasH).toDp() }
                        val isSelected = selectedOverlayId == mask.id
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (mask.xFraction * canvasW).roundToInt(),
                                        (mask.yFraction * canvasH).roundToInt()
                                    )
                                }
                                .size(boxW, boxH)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xD816120E))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GoldPrimary else GoldEdge.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedOverlayId = mask.id }
                                .pointerInput(mask.id) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        selectedOverlayId = mask.id
                                        config = config.copy(
                                            blurMasks = config.blurMasks.map { item ->
                                                if (item.id == mask.id) {
                                                    item.copy(
                                                        xFraction = (item.xFraction + dragAmount.x / canvasW).coerceIn(0f, 1f - item.widthFraction),
                                                        yFraction = (item.yFraction + dragAmount.y / canvasH).coerceIn(0f, 1f - item.heightFraction)
                                                    )
                                                } else item
                                            }
                                        )
                                    }
                                }
                        )
                    }

                    // Live Karaoke Subtitle Preview synced with ExoPlayer position
                    val activeChunk = remember(liveWordCues, currentPosMs) {
                        val chunks = liveWordCues.chunked(4)
                        chunks.firstOrNull { ch ->
                            ch.isNotEmpty() && currentPosMs in ch.first().startMs..ch.last().endMs
                        } ?: chunks.firstOrNull().orEmpty()
                    }
                    if (activeChunk.isNotEmpty()) {
                        val fontFamily = when (config.fontFamily) {
                            StudioFont.OUTFIT -> OutfitFontFamily
                            StudioFont.BEBAS_NEUE -> BebasNeueFontFamily
                            StudioFont.PLUS_JAKARTA_SANS -> JakartaSansFontFamily
                            StudioFont.JETBRAINS_MONO -> JetBrainsMonoFontFamily
                        }
                        val primaryC = Color(AssSubtitleGenerator.parseColorSafe(config.primaryColorHex, android.graphics.Color.WHITE))
                        val highlightC = Color(AssSubtitleGenerator.parseColorSafe(config.highlightColorHex, android.graphics.Color.YELLOW))
                        val yOffsetDp = with(density) { (canvasH * config.captionPositionY - 40f).toDp() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .offset(y = yOffsetDp)
                                .then(
                                    if (config.captionStyle == CaptionStyle.BOX) {
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BgMain.copy(alpha = 0.85f))
                                            .border(1.dp, highlightC, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    } else Modifier
                                ),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            activeChunk.forEach { cue ->
                                val active = currentPosMs in cue.startMs..cue.endMs || (!isPlaying && cue == activeChunk.first())
                                Text(
                                    text = "${cue.word} ",
                                    fontFamily = fontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = (config.fontSizeSp * 0.42f).sp,
                                    color = if (active) highlightC else primaryC
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5 Editor Tabs Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StudioTab.entries) { tab ->
                    SelectableGoldChip(
                        label = tab.label,
                        selected = activeTab == tab,
                        onClick = { activeTab = tab }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Tab Control Panel
            Luxury3DCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (activeTab) {
                        StudioTab.FRAMING -> StudioFramingTab(
                            config = config,
                            onUpdate = { fn -> config = fn(config) }
                        )
                        StudioTab.CAPTIONS -> StudioCaptionsTab(
                            config = config,
                            captionText = captionText,
                            onCaptionTextChange = { captionText = it },
                            onUpdate = { fn -> config = fn(config) }
                        )
                        StudioTab.AUDIO -> StudioAudioTab(
                            config = config,
                            hasVoiceFile = !record?.voicePath.isNullOrBlank(),
                            onPickMusic = { musicPickerLauncher.launch(arrayOf("audio/*")) },
                            onUpdate = { fn -> config = fn(config) }
                        )
                        StudioTab.SHAPES -> StudioShapesTab(
                            config = config,
                            selectedOverlayId = selectedOverlayId,
                            onSelectOverlay = { selectedOverlayId = it },
                            onUpdate = { fn -> config = fn(config) }
                        )
                        StudioTab.EXPORT -> {
                            val mp4File = record?.finalMp4Path?.let { File(it) }
                            val hasMp4 = mp4File != null && mp4File.exists()
                            StudioExportTab(
                                hasExportedMp4 = hasMp4,
                                exportProgress = exportProgress,
                                onSaveDraft = {
                                    viewModel.saveStudioDraft(
                                        shortId = shortId,
                                        config = config,
                                        title = record?.title ?: "Studio Short",
                                        caption = captionText
                                    )
                                },
                                onRenderExport = {
                                    viewModel.exportStudioShort(
                                        shortId = shortId,
                                        config = config,
                                        title = record?.title ?: "Studio Short",
                                        caption = captionText
                                    )
                                },
                                onCancelExport = { viewModel.cancelStudioExport() },
                                onDismissExportStatus = { viewModel.clearStudioExportProgress() },
                                onSaveGallery = {
                                    record?.let { viewModel.saveShortToGallery(it) }
                                },
                                onShare = {
                                    if (mp4File != null && mp4File.exists()) {
                                        val intent = viewModel.videoEngine.createShareIntent(
                                            mp4File,
                                            record?.title ?: "Brohi Short"
                                        )
                                        context.startActivity(Intent.createChooser(intent, "Share Short"))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
