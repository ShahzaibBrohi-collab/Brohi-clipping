package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Dark3DButton
import com.example.ui.components.Gold3DButton
import com.example.ui.components.Gold3DIconBadge
import com.example.ui.components.Hawk3DEmblem
import com.example.ui.components.Luxury3DCard
import com.example.ui.theme.BgMain
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldEdge
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onboardingCompleted: Boolean,
    onNavigateNext: (showOnboarding: Boolean) -> Unit
) {
    val scale = remember { Animatable(0.76f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        scale.animateTo(1f, animationSpec = tween(750, easing = FastOutSlowInEasing))
        delay(1250L)
        onNavigateNext(!onboardingCompleted)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF282012), BgSecondary, BgMain),
                    radius = 1100f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .scale(scale.value)
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Hawk3DEmblem(size = 158.dp, useHeroArt = true)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "BROHI CLIPPING",
                style = MaterialTheme.typography.displayLarge.copy(
                    brush = Brush.linearGradient(listOf(GoldLight, GoldPrimary, GoldDark)),
                    letterSpacing = 3.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "3D AI VIRAL SHORTS STUDIO",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, GoldPrimary, GoldLight, GoldPrimary, Color.Transparent)
                        )
                    )
            )
        }
    }
}

private data class OnboardingStep(
    val stepNumber: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                "01",
                "Paste Link or Import Video",
                "Drop a YouTube or Twitch link, discover live gaming streams, or pick a local recording via Android Photo Picker.",
                Icons.Filled.Link
            ),
            OnboardingStep(
                "02",
                "AI Viral Moment Radar",
                "Gemini multimodal AI analyzes audio hype, reactions, and stream metadata to pinpoint peak viral timestamps.",
                Icons.Filled.AutoAwesome
            ),
            OnboardingStep(
                "03",
                "Generate Automatic Shorts",
                "Synthesize multilingual AI narration in 8 languages, generate ASS karaoke subtitles, and render 9:16 MP4 clips.",
                Icons.Filled.ContentCut
            ),
            OnboardingStep(
                "04",
                "3D Video Studio Editor",
                "Fine-tune Fill/Blur framing, pinch-zoom & pan, audio ducking, blur masks, and neon/bold word captions.",
                Icons.Filled.MovieCreation
            ),
            OnboardingStep(
                "05",
                "Export & Share Anywhere",
                "Save hardware-accelerated 1080×1920 MP4s directly to your Gallery or share instantly to Shorts, Reels, and TikTok.",
                Icons.Filled.Share
            )
        )
    }

    var currentPage by remember { mutableIntStateOf(0) }
    val step = steps[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1D1810), BgMain)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Hawk3DEmblem(size = 38.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "BROHI CLIPPING",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldLight
                    )
                }
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            AnimatedContent(targetState = step, label = "onboarding_step") { active ->
                Luxury3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    goldBorder = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(GoldPrimary.copy(alpha = 0.14f))
                                .border(1.dp, GoldEdge, RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "STEP ${active.stepNumber} OF 05",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldLight
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Gold3DIconBadge(
                            icon = active.icon,
                            contentDescription = active.title,
                            size = 82.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = active.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = active.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.indices.forEach { idx ->
                    val selected = idx == currentPage
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (selected) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) GoldPrimary else GoldEdge.copy(alpha = 0.4f))
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentPage > 0) {
                    Dark3DButton(
                        text = "Back",
                        onClick = { currentPage-- },
                        modifier = Modifier.weight(0.4f),
                        testTag = "onboarding_back_button"
                    )
                }
                Gold3DButton(
                    text = if (currentPage == steps.lastIndex) "Enter Studio" else "Next",
                    onClick = {
                        if (currentPage < steps.lastIndex) {
                            currentPage++
                        } else {
                            onFinish()
                        }
                    },
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.weight(1f),
                    testTag = "onboarding_next_button"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
