package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ActionBlue
import com.example.ui.theme.BgMain
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.CardSecondary
import com.example.ui.theme.CardSurface
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldEdge
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianRimLight
import com.example.ui.theme.ObsidianRimSubtle
import com.example.ui.theme.ObsidianShadow
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Luxury 3D skeuomorphic metallic gold button with physical bottom extrusion edge,
 * inner top highlight, soft ambient shadow, and 3dp press-down animation.
 */
@Composable
fun Gold3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String? = null,
    height: Dp = 54.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "gold_btn_press"
    )
    val shadowElev by animateDpAsState(
        targetValue = if (isPressed || !enabled) 2.dp else 10.dp,
        label = "gold_btn_shadow"
    )

    val shape = RoundedCornerShape(16.dp)
    val baseColor = if (enabled) GoldEdge else Color(0xFF3E3524)
    val faceGradient = if (enabled) {
        Brush.verticalGradient(
            colors = listOf(GoldLight, GoldPrimary, GoldDark)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF5A503E), Color(0xFF473E30))
        )
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = height + 4.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .shadow(
                elevation = shadowElev,
                shape = shape,
                ambientColor = if (enabled) GoldPrimary else ObsidianShadow,
                spotColor = if (enabled) GoldPrimary else ObsidianShadow
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        // Bottom 3D extrusion base
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = 4.dp)
                .background(baseColor, shape)
        )

        // Raised metallic gold face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = pressOffset)
                .background(faceGradient, shape)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (enabled) 0.55f else 0.15f),
                            GoldEdge.copy(alpha = 0.6f)
                        )
                    ),
                    shape = shape
                )
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) BgMain else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (enabled) BgMain else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Beveled dark obsidian 3D button with metallic rim and physical press depth.
 */
@Composable
fun Dark3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = GoldPrimary,
    enabled: Boolean = true,
    testTag: String? = null,
    height: Dp = 50.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "dark_btn_press"
    )
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = height + 4.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .shadow(if (isPressed) 2.dp else 8.dp, shape, ambientColor = ObsidianShadow)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = 3.dp)
                .background(Color(0xFF0A0806), shape)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = pressOffset)
                .background(
                    Brush.verticalGradient(listOf(CardSecondary, CardSurface, BgSecondary)),
                    shape
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(accentColor.copy(alpha = 0.5f), Color.Black.copy(alpha = 0.7f))
                    ),
                    shape = shape
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) accentColor else TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Multi-layered 3D beveled obsidian card with metallic rim highlights and deep drop shadow.
 */
@Composable
fun Luxury3DCard(
    modifier: Modifier = Modifier,
    goldBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val borderBrush = if (goldBorder) {
        Brush.linearGradient(
            listOf(GoldLight.copy(alpha = 0.7f), GoldEdge.copy(alpha = 0.45f), GoldDark.copy(alpha = 0.6f))
        )
    } else {
        Brush.verticalGradient(
            listOf(ObsidianRimLight, ObsidianRimSubtle, Color.Black.copy(alpha = 0.65f))
        )
    }

    val surfaceBrush = Brush.verticalGradient(
        listOf(CardSecondary.copy(alpha = 0.92f), CardSurface, BgSecondary)
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = if (goldBorder) 12.dp else 8.dp,
                shape = shape,
                ambientColor = if (goldBorder) GoldDark.copy(alpha = 0.4f) else ObsidianShadow,
                spotColor = ObsidianShadow
            )
            .clip(shape)
            .background(surfaceBrush, shape)
            .border(width = if (goldBorder) 1.3.dp else 1.dp, brush = borderBrush, shape = shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Dimensional embossed 3D gold icon badge.
 */
@Composable
fun Gold3DIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = BgMain
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, CircleShape, ambientColor = GoldDark)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(GoldLight, GoldPrimary, GoldDark, GoldEdge)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

/**
 * Raised 3D icon button for top app bars and compact controls (meets >=48dp touch target).
 */
@Composable
fun Gold3DIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = GoldPrimary,
    testTag: String? = null
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = ObsidianShadow)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(CardSecondary, CardSurface)))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accentColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 3D Hawk Emblem widget with subtle breathing golden halo.
 */
@Composable
fun Hawk3DEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    useHeroArt: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "hawk_glow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .shadow(10.dp, CircleShape, ambientColor = GoldPrimary.copy(alpha = glowAlpha))
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(GoldLight, GoldPrimary, GoldEdge)),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                id = if (useHeroArt) R.drawable.img_splash_hawk_hero else R.drawable.img_launcher_hawk
            ),
            contentDescription = "Brohi Clipping 3D Hawk Emblem",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Animated 3D golden scanning radar with rotating sweep beam, concentric rings,
 * pulsing core, and detected viral moment blips.
 */
@Composable
fun Radar3DScanner(
    isScanning: Boolean,
    detectedMomentAngles: List<Float>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_scan")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f * 0.9f

            // Outer metallic obsidian dish
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CardSecondary, BgMain, Color.Black),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // Concentric 3D radar rings
            listOf(0.25f, 0.5f, 0.75f, 1.0f).forEachIndexed { idx, factor ->
                drawCircle(
                    color = if (idx == 3) GoldPrimary.copy(alpha = 0.65f) else GoldEdge.copy(alpha = 0.4f),
                    radius = maxRadius * factor,
                    center = center,
                    style = Stroke(width = if (idx == 3) 3f else 1.5f)
                )
            }

            // Crosshair lines
            drawLine(
                color = GoldEdge.copy(alpha = 0.3f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.5f
            )
            drawLine(
                color = GoldEdge.copy(alpha = 0.3f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.5f
            )

            if (isScanning) {
                // Pulsing sonar ring
                drawCircle(
                    color = GoldPrimary.copy(alpha = (1f - pulseRadius) * 0.55f),
                    radius = maxRadius * pulseRadius,
                    center = center,
                    style = Stroke(width = 3f)
                )

                // Rotating golden sweep wedge
                rotate(degrees = sweepAngle, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.75f to Color.Transparent,
                            0.95f to GoldPrimary.copy(alpha = 0.32f),
                            1.0f to GoldLight.copy(alpha = 0.7f),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = true,
                        topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                        size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
                    )
                    drawLine(
                        color = GoldLight,
                        start = center,
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Detected viral moment blips
            detectedMomentAngles.forEachIndexed { index, angleDeg ->
                val rad = Math.toRadians(angleDeg.toDouble())
                val distFactor = 0.38f + ((index % 3) * 0.2f)
                val bx = center.x + (maxRadius * distFactor * cos(rad)).toFloat()
                val by = center.y + (maxRadius * distFactor * sin(rad)).toFloat()
                drawCircle(
                    color = StatusSuccess.copy(alpha = 0.28f),
                    radius = 18f,
                    center = Offset(bx, by)
                )
                drawCircle(
                    color = StatusSuccess,
                    radius = 8f,
                    center = Offset(bx, by)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(bx, by)
                )
            }

            // Center 3D golden emitter core
            drawCircle(
                brush = Brush.radialGradient(listOf(GoldLight, GoldPrimary, GoldEdge)),
                radius = 12f,
                center = center
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun SectionHeader3D(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = GoldLight
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}
