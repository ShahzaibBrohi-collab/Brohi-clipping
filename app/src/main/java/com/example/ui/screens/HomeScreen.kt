package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.db.ShortRecordEntity
import com.example.ui.components.Dark3DButton
import com.example.ui.components.Gold3DButton
import com.example.ui.components.Gold3DIconBadge
import com.example.ui.components.Gold3DIconButton
import com.example.ui.components.Hawk3DEmblem
import com.example.ui.components.Luxury3DCard
import com.example.ui.components.SectionHeader3D
import com.example.ui.components.StatusPill
import com.example.ui.theme.ActionBlue
import com.example.ui.theme.BgMain
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.CardSecondary
import com.example.ui.theme.CardSurface
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldEdge
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BrohiViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: BrohiViewModel,
    onOpenConfig: () -> Unit,
    onOpenLiveDiscovery: (category: String) -> Unit,
    onOpenStudio: (shortId: String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val isValidating by viewModel.isValidatingUrl.collectAsStateWithLifecycle()
    val urlError by viewModel.urlError.collectAsStateWithLifecycle()
    val vaultShorts by viewModel.vaultShortsFlow.collectAsStateWithLifecycle()

    var pendingDeleteShort by remember { mutableStateOf<ShortRecordEntity?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.importLocalVideoUri(uri, onSuccess = onOpenConfig)
            }
        }
    )

    val categories = remember {
        listOf("GTA V", "Valorant", "Minecraft", "Just Chatting")
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Luxury 3D Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Hawk3DEmblem(size = 48.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "BROHI CLIPPING",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    brush = Brush.linearGradient(listOf(GoldLight, GoldPrimary, GoldDark))
                                )
                            )
                            Text(
                                text = "3D AI Shorts Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Gold3DIconButton(
                            icon = Icons.Filled.LiveTv,
                            contentDescription = "Live Streams Discovery",
                            onClick = { onOpenLiveDiscovery("GTA V") },
                            accentColor = ActionBlue,
                            testTag = "home_live_streams_button"
                        )
                        Gold3DIconButton(
                            icon = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            onClick = onOpenSettings,
                            accentColor = GoldPrimary,
                            testTag = "home_settings_button"
                        )
                    }
                }
            }

            // URL Input & Import Card
            item {
                Luxury3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    goldBorder = true
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clip Source Studio",
                                style = MaterialTheme.typography.titleLarge,
                                color = GoldLight
                            )
                            StatusPill(label = "Gemini Ready", color = StatusSuccess)
                        }

                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { viewModel.updateUrlInput(it) },
                            placeholder = {
                                Text(
                                    "Paste YouTube, Twitch, or MP4 URL...",
                                    color = TextSecondary.copy(alpha = 0.65f)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_url_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = GoldEdge.copy(alpha = 0.6f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = GoldPrimary,
                                focusedContainerColor = BgMain.copy(alpha = 0.7f),
                                unfocusedContainerColor = BgMain.copy(alpha = 0.55f)
                            )
                        )

                        if (!urlError.isNullOrBlank()) {
                            Text(
                                text = urlError.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusError
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Dark3DButton(
                                text = "Paste URL",
                                icon = Icons.Filled.ContentPaste,
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                                    if (!clipText.isNullOrBlank()) {
                                        viewModel.updateUrlInput(clipText.trim())
                                    } else {
                                        viewModel.showToast("Clipboard is empty.")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "home_paste_url_button"
                            )
                            Dark3DButton(
                                text = "Import Video",
                                icon = Icons.Filled.VideoLibrary,
                                accentColor = ActionBlue,
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "home_import_video_button"
                            )
                        }

                        if (isValidating) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = GoldPrimary,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Validating media source...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GoldLight
                                )
                            }
                        } else {
                            Gold3DButton(
                                text = "Analyze & Configure",
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                onClick = {
                                    viewModel.inspectAndProceedUrl(urlInput, onSuccess = onOpenConfig)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "home_proceed_button"
                            )
                        }
                    }
                }
            }

            // Gaming Category Shortcuts
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader3D(
                        title = "Live Stream Categories",
                        subtitle = "Tap a category to browse live Twitch broadcasts",
                        trailing = {
                            TextButton(onClick = { onOpenLiveDiscovery("GTA V") }) {
                                Text("See All", color = ActionBlue)
                            }
                        }
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(categories) { cat ->
                            Luxury3DCard(
                                modifier = Modifier.testTag("category_chip_${cat.lowercase().replace(" ", "_")}"),
                                onClick = { onOpenLiveDiscovery(cat) },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Gold3DIconBadge(
                                        icon = Icons.Filled.LiveTv,
                                        contentDescription = cat,
                                        size = 30.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // My Shorts Vault Header
            item {
                SectionHeader3D(
                    title = "My Shorts Vault",
                    subtitle = "${vaultShorts.size} saved vertical short(s)"
                )
            }

            if (vaultShorts.isEmpty()) {
                item {
                    Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_vault_empty_art),
                                contentDescription = "Empty Shorts Vault",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(155.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, GoldEdge.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Your Shorts Vault is Empty",
                                style = MaterialTheme.typography.titleLarge,
                                color = GoldLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Paste a stream URL or import a local clip above to let Gemini extract viral highlights and render 9:16 shorts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(vaultShorts, key = { it.id }) { shortItem ->
                    VaultShortCard(
                        record = shortItem,
                        onOpenStudio = { onOpenStudio(shortItem.id) },
                        onSaveToGallery = { viewModel.saveShortToGallery(shortItem) },
                        onShare = {
                            val mp4 = shortItem.finalMp4Path?.let { File(it) }
                            if (mp4 != null && mp4.exists()) {
                                val intent = viewModel.videoEngine.createShareIntent(mp4, shortItem.title)
                                context.startActivity(Intent.createChooser(intent, "Share Short"))
                            } else {
                                viewModel.showToast("Open in Studio and export MP4 first.")
                            }
                        },
                        onDelete = { pendingDeleteShort = shortItem }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    pendingDeleteShort?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteShort = null },
            containerColor = CardSurface,
            title = { Text("Delete Short?", color = GoldLight) },
            text = {
                Text(
                    "Remove \"${target.title}\" and delete its rendered MP4, audio, and thumbnail from internal storage?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteShort(target.id)
                    pendingDeleteShort = null
                }) {
                    Text("Delete", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteShort = null }) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
private fun VaultShortCard(
    record: ShortRecordEntity,
    onOpenStudio: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val durationSec = ((record.endTimeMs - record.startTimeMs) / 1000L).coerceAtLeast(1L)
    val hasMp4 = !record.finalMp4Path.isNullOrBlank() && File(record.finalMp4Path).exists()
    val dateLabel = remember(record.createdTimestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(record.createdTimestamp))
    }

    Luxury3DCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_card_${record.id}"),
        onClick = onOpenStudio
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgMain)
                    .border(1.dp, GoldEdge.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val thumbFile = record.thumbnailPath?.let { File(it) }?.takeIf { it.exists() }
                if (thumbFile != null) {
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = record.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = GoldEdge,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgMain.copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${durationSec}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(
                        label = if (hasMp4) "MP4 Ready" else "Draft",
                        color = if (hasMp4) StatusSuccess else ActionBlue
                    )
                    Text(
                        text = "Score ${record.viralScore}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${record.streamer} • $dateLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenStudio,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (hasMp4) Icons.Filled.PlayArrow else Icons.Filled.Edit,
                            contentDescription = "Open in 3D Video Studio",
                            tint = GoldPrimary
                        )
                    }
                    IconButton(
                        onClick = onSaveToGallery,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Save to Gallery",
                            tint = StatusSuccess
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share Video",
                            tint = ActionBlue
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete Video",
                            tint = StatusError
                        )
                    }
                }
            }
        }
    }
}
