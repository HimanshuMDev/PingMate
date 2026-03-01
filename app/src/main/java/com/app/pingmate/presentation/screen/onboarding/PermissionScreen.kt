package com.app.pingmate.presentation.screen.onboarding

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pingmate.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PermissionScreen(
    onNavigateNext: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }

    // Smooth elapsed-based entrance (same approach as WelcomeScreen)
    var elapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (elapsedMs < 1200L) {
            delay(16L)
            elapsedMs = System.currentTimeMillis() - start
        }
    }
    fun alphaAt(startMs: Long, dur: Long = 480L): Float {
        val t = ((elapsedMs - startMs).toFloat() / dur).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
    fun slideAt(startMs: Long, dur: Long = 480L): Float = (1f - alphaAt(startMs, dur)) * 34f

    val iconA  = alphaAt(0L)
    val titleA = alphaAt(200L)
    val bodyA  = alphaAt(360L)
    val cardA  = alphaAt(500L)
    val btnA   = alphaAt(680L)

    // Permission polling
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            while (true) {
                delay(1000)
                if (isNotificationServiceEnabled(context)) {
                    hasPermission = true
                    onNavigateNext()
                    break
                }
            }
        } else {
            onNavigateNext()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = isNotificationServiceEnabled(context)
        if (hasPermission) onNavigateNext()
    }

    // Breathing icon animation
    val pulse = rememberInfiniteTransition(label = "breathe")
    val breatheScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.11f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "bs"
    )
    val glowA by pulse.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "ga"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF090910), Color(0xFF0D0D18), Color(0xFF0B0B16)))
            )
    ) {
        // Dynamic ambient glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-60).dp)
                .graphicsLayer { alpha = glowA }
                .background(
                    Brush.radialGradient(listOf(VipPurple.copy(alpha = 0.5f), Color.Transparent)),
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
            // Breathing animated Bell Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        alpha = iconA
                        scaleX = 0.55f + iconA * 0.45f
                        scaleY = 0.55f + iconA * 0.45f
                    }
            ) {
                // Outer breathing glow ring
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer { scaleX = breatheScale; scaleY = breatheScale }
                        .background(
                            Brush.radialGradient(listOf(VipPurple.copy(alpha = 0.22f), Color.Transparent)),
                            CircleShape
                        )
                )
                // Icon container
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF1E0A40), Color(0xFF120830))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = VipPurple,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Title block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = titleA
                    translationY = slideAt(200L)
                }
            ) {
                Text(
                    text = "Allow Notification\nAccess",
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 37.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(listOf(VipPurple, NotiBlue)),
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body text
            Text(
                text = "PingMate needs permission to read your notifications and build your intelligent message feed.",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = bodyA
                    translationY = slideAt(360L)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Step-by-step guide card
            Surface(
                color = Color(0xFF10101C),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C1C32)),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = cardA
                        translationY = slideAt(500L)
                    }
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "HOW TO GRANT ACCESS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    PermissionStep("1", "Tap  \"Grant Access\"  below")
                    PermissionStep("2", "Find  PingMate  in the list")
                    PermissionStep("3", "Toggle the switch to  ON")
                    PermissionStep("4", "Tap  \"Allow\"  to confirm")
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Action button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.graphicsLayer {
                    alpha = btnA
                    translationY = slideAt(680L)
                }
            ) {
                GradientButton(
                    text = "Grant Access",
                    onClick = {
                        launcher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
                Text(
                    text = "⚡  Waiting… auto-advances once access is granted",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PermissionStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(VipPurple.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = VipPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(text, color = TextSecondary, fontSize = 14.sp, lineHeight = 19.sp)
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!flat.isNullOrEmpty()) {
        flat.split(":").forEach { name ->
            val cn = ComponentName.unflattenFromString(name)
            if (cn?.packageName == pkgName) return true
        }
    }
    return false
}
