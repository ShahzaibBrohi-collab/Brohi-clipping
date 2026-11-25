package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.TwitchStreamInfo
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.BrohiViewModel
import com.example.ui.viewmodel.DiscoveryUiState

@Composable
fun LiveDiscoveryScreen(
    viewModel: BrohiViewModel,
    initialCategory: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onStreamSelected: () -> Unit
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val discoveryState by viewModel.discoveryState.collectAsStateWithLifecycle()

    LaunchedEffect(initialCategory) {
        viewModel.loadTwitchCategory(initialCategory)
    }

    val categories = listOf("GTA V", "Valorant", "Minecraft", "Just Chatting")

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Gold3DIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                            testTag = "discovery_back_button"
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Live Discovery",
                                style = MaterialTheme.typography.headlineMedium,
                                color = GoldLight
                            )
                            Text(
                                text = "Real Twitch Helix API",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Gold3DIconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = "Refresh Streams",
                        onClick = { viewModel.loadTwitchCategory(selectedCategory) },
                        accentColor = ActionBlue,
                        testTag = "discovery_refresh_button"
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, GoldEdge.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_discovery_banner),
                        contentDescription = "Live Discovery Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BgMain.copy(alpha = 0.85f), Color.Transparent)
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                text = "TWITCH HELIX LIVE",
                                style = MaterialTheme.typography.titleLarge,
                                color = GoldLight
                            )
                            Text(
                                text = "Select any live creator to configure AI clipping",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val active = cat.equals(selectedCategory, ignoreCase = true)
                        Luxury3DCard(
                            goldBorder = active,
                            onClick = { viewModel.loadTwitchCategory(cat) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) GoldLight else TextSecondary
                            )
                        }
                    }
                }
            }

            when (val state = discoveryState) {
                is DiscoveryUiState.Idle, is DiscoveryUiState.Loading -> {
                    item {
                        Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = GoldPrimary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Fetching live $selectedCategory streams from Twitch...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                is DiscoveryUiState.MissingCredentials -> {
                    item {
                        Luxury3DCard(
                            modifier = Modifier.fillMaxWidth(),
                            goldBorder = true
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Twitch Developer Credentials Required",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = GoldLight
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Gold3DButton(
                                    text = "Open Settings to Connect Twitch",
                                    icon = Icons.Filled.Key,
                                    onClick = onOpenSettings,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "discovery_open_settings_button"
                                )
                            }
                        }
                    }
                }

                is DiscoveryUiState.Error -> {
                    item {
                        Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Twitch API Error", style = MaterialTheme.typography.titleLarge, color = StatusError)
                                Text(state.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Gold3DButton(
                                    text = "Retry",
                                    icon = Icons.Filled.Refresh,
                                    onClick = { viewModel.loadTwitchCategory(selectedCategory) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                is DiscoveryUiState.Loaded -> {
                    if (state.streams.isEmpty()) {
                        item {
                            Luxury3DCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "No active live streams returned for $selectedCategory right now.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        items(state.streams, key = { it.id }) { stream ->
                            TwitchStreamCard(
                                stream = stream,
                                onClick = {
                                    viewModel.selectTwitchStream(stream, onReady = onStreamSelected)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TwitchStreamCard(
    stream: TwitchStreamInfo,
    onClick: () -> Unit
) {
    Luxury3DCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("twitch_stream_card_${stream.id}"),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardSecondary)
            ) {
                AsyncImage(
                    model = stream.thumbnailUrl,
                    contentDescription = stream.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(label = "LIVE", color = StatusError)
                    StatusPill(label = stream.language, color = GoldPrimary)
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgMain.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stream.viewerCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = stream.avatarUrl,
                    contentDescription = stream.userName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.dp, GoldDark, CircleShape)
                        .background(CardSecondary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stream.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${stream.userName} • ${stream.gameName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
