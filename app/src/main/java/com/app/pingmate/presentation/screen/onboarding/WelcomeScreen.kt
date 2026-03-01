package com.app.pingmate.presentation.screen.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pingmate.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Smooth easing curve shared across entrances
private val EntranceEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f) // expo-out

@Composable
fun WelcomeScreen(
    onNavigateNext: () -> Unit
) {
    // One Animatable per element – Compose handles these on the animation thread, zero recompose jank
    val logoAlpha    = remember { Animatable(0f) }
    val logoScale    = remember { Animatable(0.55f) }
    val titleAlpha   = remember { Animatable(0f) }
    val titleSlide   = remember { Animatable(30f) }
    val subAlpha     = remember { Animatable(0f) }
    val subSlide     = remember { Animatable(20f) }
    val cardsAlpha   = remember { Animatable(0f) }
    val cardsSlide   = remember { Animatable(24f) }
    val buttonAlpha  = remember { Animatable(0f) }
    val buttonSlide  = remember { Animatable(20f) }

    val spec450 = tween<Float>(450, easing = EntranceEasing)
    val spec400 = tween<Float>(400, easing = EntranceEasing)
    val spec380 = tween<Float>(380, easing = EntranceEasing)

    LaunchedEffect(Unit) {
        // Logo: scale + fade in together
        launch { logoAlpha.animateTo(1f, spec450) }
        launch { logoScale.animateTo(1f, tween(600, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))) }
        delay(160)
        // Title
        launch { titleAlpha.animateTo(1f, spec400) }
        launch { titleSlide.animateTo(0f, spec400) }
        delay(140)
        // Subtitle
        launch { subAlpha.animateTo(1f, spec380) }
        launch { subSlide.animateTo(0f, spec380) }
        delay(130)
        // Feature cards
        launch { cardsAlpha.animateTo(1f, spec450) }
        launch { cardsSlide.animateTo(0f, spec450) }
        delay(160)
        // Button
        launch { buttonAlpha.animateTo(1f, spec380) }
        launch { buttonSlide.animateTo(0f, spec380) }
    }

    // Slow ambient glow pulse — completely separate from entrance
    val pulse = rememberInfiniteTransition(label = "glow")
    val glowScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "gs"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF08080F), Color(0xFF0D0D18), Color(0xFF101020))
                )
            )
    ) {
        // Static ambient top glow (no animation = no jank)
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-70).dp)
                .background(
                    Brush.radialGradient(listOf(NotiBlue.copy(alpha = 0.10f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Logo ──
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                    .scale(glowScale),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .background(
                            Brush.radialGradient(listOf(NotiBlue.copy(alpha = 0.20f), Color.Transparent)),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF152040), Color(0xFF0A1020))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Bolt, null,
                        tint = NotiBlue,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // ── Title ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleSlide.value
                }
            ) {
                Text(
                    text = "PingMate AI",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1.5).sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(listOf(NotiBlue, VipPurple)),
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Subtitle ──
            Text(
                text = "Your smart, AI-powered notification manager.\nOrganizes, groups & summarizes your alerts.",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 23.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = subAlpha.value
                    translationY = subSlide.value
                }
            )

            Spacer(modifier = Modifier.height(34.dp))

            // ── Feature Cards ──
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.graphicsLayer {
                    alpha = cardsAlpha.value
                    translationY = cardsSlide.value
                }
            ) {
                WelcomeFeatureRow(
                    icon = Icons.Default.AutoAwesome,
                    iconTint = NotiBlue,
                    title = "AI-Powered Summaries",
                    subtitle = "Instantly condense any app's notifications using on-device AI."
                )
                WelcomeFeatureRow(
                    icon = Icons.Default.Notifications,
                    iconTint = VipPurple,
                    title = "Smart Notification Feed",
                    subtitle = "All your alerts in one beautiful, prioritized dark-mode feed."
                )
                WelcomeFeatureRow(
                    icon = Icons.Default.Lock,
                    iconTint = SuccessGreen,
                    title = "Privacy First",
                    subtitle = "Your data stays on your device. No accounts, no tracking."
                )
            }

            Spacer(modifier = Modifier.height(38.dp))

            // ── Button ──
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = buttonAlpha.value
                    translationY = buttonSlide.value
                }
            ) {
                GradientButton(text = "Get Started  →", onClick = onNavigateNext)
            }
        }
    }
}

@Composable
private fun WelcomeFeatureRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Surface(
        color = Color(0xFF0F0F1C),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C1C30)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconTint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                Text(subtitle, fontSize = 12.sp, color = TextMuted, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(Color(0xFF3A6EF0), Color(0xFF8B50F5)))
                else Brush.horizontalGradient(listOf(Color(0xFF252535), Color(0xFF1C1C2C))),
                RoundedCornerShape(28.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            elevation = null
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = if (enabled) Color.White else TextMuted
            )
        }
    }
}
