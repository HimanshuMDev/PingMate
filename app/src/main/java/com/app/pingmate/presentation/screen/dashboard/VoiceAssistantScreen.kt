package com.app.pingmate.presentation.screen.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.pingmate.R
import com.app.pingmate.ui.theme.NotiBlue
import com.app.pingmate.ui.theme.VipPurple
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.clickable

/**
 * Assistant overlay: handles both listening and the single fluid result view natively.
 */
@Composable
fun VoiceAssistantScreen(
    transcribedText: String,
    isListening: Boolean,
    isProcessing: Boolean,
    aiResponse: String?,
    onStartListening: () -> Unit,
    onProcessPrompt: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    var didStart by remember { mutableStateOf(false) }
    var hasListeningStarted by remember { mutableStateOf(false) }
    LaunchedEffect(isListening) {
        if (isListening) hasListeningStarted = true
    }
    LaunchedEffect(Unit) {
        if (!didStart) {
            didStart = true
            onStartListening()
        }
    }

    LaunchedEffect(isListening) {
        if (!isListening && transcribedText.isNotBlank()) {
            onProcessPrompt(transcribedText)
        }
    }

    val isResultPhase = isProcessing || aiResponse != null
    val lottieSize by animateDpAsState(
        targetValue = if (isResultPhase) 100.dp else 220.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "lottieSize"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12).copy(alpha = 0.92f))
            .clickable(enabled = true, onClick = onDismiss)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .statusBarsPadding()
                .clickable(enabled = true, onClick = {})
        ) {
            val listeningBlockHeight = 220.dp + 24.dp + 60.dp
            val centerTopPadding = (maxHeight - listeningBlockHeight) / 2
            val topPaddingTarget = if (isResultPhase) 16.dp else centerTopPadding.coerceAtLeast(0.dp)
            val topPadding by animateDpAsState(
                targetValue = topPaddingTarget,
                animationSpec = tween(durationMillis = 380, easing = EaseInOutSine),
                label = "topPadding"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.size(topPadding))

                val infiniteTransition = rememberInfiniteTransition(label = "glow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.06f,
                    targetValue = 0.22f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(12.dp)
                        .size(lottieSize)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.88f)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        NotiBlue.copy(alpha = glowAlpha),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.ai_animation)
                    )
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = LottieConstants.IterateForever
                    )
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!isResultPhase) {
                    Spacer(modifier = Modifier.size(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .clickable(enabled = !isListening) {
                                if (!isListening) onStartListening()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = "Microphone",
                            tint = if (isListening) NotiBlue else Color(0xFF7A7E8E),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        val textToDisplay = when {
                            transcribedText.isNotBlank() -> transcribedText
                            isListening -> "Listening…"
                            !hasListeningStarted -> "Starting…"
                            else -> "Didn't catch that. Tap to try again."
                        }
                        val textColor = when {
                            transcribedText.isNotBlank() -> Color(0xFFE8EAEF)
                            isListening -> Color(0xFF9A9EAC)
                            !hasListeningStarted -> Color(0xFF9A9EAC)
                            else -> Color(0xFFA85A66)
                        }
                        Text(
                            text = textToDisplay,
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = 3
                        )
                    }
                }

                if (isResultPhase) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(0.95f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0F1016),
                            border = BorderStroke(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(NotiBlue.copy(alpha = 0.25f), VipPurple.copy(alpha = 0.2f))
                                )
                            ),
                            shadowElevation = 16.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StructuredAiResult(
                                    prompt = transcribedText,
                                    response = aiResponse ?: "",
                                    isSummarizing = isProcessing && aiResponse == null
                                )
                            }
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(20.dp)
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

