package com.app.pingmate.presentation.screen.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.*
import com.app.pingmate.R
import com.app.pingmate.ui.theme.*
import kotlinx.coroutines.delay

/** Shared summary card (header + result). When response is null, shows prompt + blinking cursor (summarizing). */
@Composable
fun VoiceAiSummaryCard(
    prompt: String,
    response: String?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight()
            .clickable(enabled = false) {},
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF0A0A12),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(NotiBlue.copy(alpha = 0.3f), VipPurple.copy(alpha = 0.3f))
            )
        ),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(NotiBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = NotiBlue, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "PingMate Summary",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val text = (prompt.ifBlank { "Request" } + "\n\n" + (response ?: "")).trim()
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("PingMate Summary", text))
                            android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            StructuredAiResult(prompt = prompt, response = response ?: "", isSummarizing = response == null)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/** Bottom sheet content: prompt + response (or loading with blinking cursor). Clean layout, no black background. */
@Composable
fun SummaryBottomSheetContent(
    prompt: String,
    response: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color(0xFF0F1014).copy(alpha = 0.94f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Summary",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val text = (prompt.ifBlank { "Request" } + "\n\n" + (response ?: "")).trim()
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Summary", text))
                            android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, tint = TextMuted, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Your request", color = NotiBlue.copy(alpha = 0.95f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = prompt.ifBlank { "—" },
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text("Response", color = NotiBlue.copy(alpha = 0.95f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            if (response == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Summarizing…", color = Color(0xFFB0B4BC), fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    val cursorVisible by rememberInfiniteTransition(label = "blink").animateFloat(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(520),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "cursor"
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp, 20.dp)
                            .graphicsLayer { alpha = cursorVisible }
                            .background(NotiBlue, RoundedCornerShape(1.5.dp))
                    )
                }
            } else {
                Text(
                    text = response,
                    color = Color(0xFFE2E4E8),
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/** Legacy dialog entry point; prefer SummaryBottomSheetContent with ModalBottomSheet (transparent). */
@Composable
fun SummaryDialog(
    prompt: String,
    response: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)) {
        Box(Modifier.fillMaxSize().background(Color.Transparent).clickable(enabled = true, onClick = onDismiss), contentAlignment = Alignment.Center) {
            SummaryBottomSheetContent(prompt = prompt, response = response, onDismiss = onDismiss)
        }
    }
}

/** Full-screen overlay with transparent/blur-friendly dim and summary card. Use in-app so blurred Scaffold shows through. */
@Composable
fun VoiceAiResultOverlay(
    prompt: String,
    response: String,
    onDismissRequest: () -> Unit
) {
    val enterScale = remember { Animatable(0.92f) }
    val enterAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterScale.animateTo(1f, tween(350, easing = LinearOutSlowInEasing))
        enterAlpha.animateTo(1f, tween(280))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(enabled = true, onClick = onDismissRequest),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .scale(enterScale.value)
                .graphicsLayer { alpha = enterAlpha.value }
                .clickable(enabled = true) { /* consume tap so backdrop doesn't dismiss */ }
        ) {
            VoiceAiSummaryCard(
                prompt = prompt,
                response = response,
                onDismissRequest = onDismissRequest
            )
        }
    }
}

@Composable
fun VoiceAiDialog(
    onDismissRequest: () -> Unit,
    transcribedText: String,
    aiResponse: String? = null
) {
    val enterScale = remember { Animatable(0.92f) }
    val enterAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterScale.animateTo(1f, tween(400, easing = LinearOutSlowInEasing))
        enterAlpha.animateTo(1f, tween(300))
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = true, onClick = onDismissRequest),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(enterScale.value)
                    .graphicsLayer { alpha = enterAlpha.value }
            ) {
                VoiceAiSummaryCard(
                    prompt = transcribedText,
                    response = aiResponse ?: "",
                    onDismissRequest = onDismissRequest
                )
            }
        }
    }
}


@Composable
fun StructuredAiResult(prompt: String, response: String, isSummarizing: Boolean = false) {
    val scrollState = rememberScrollState()
    
    var showPrompt by remember { mutableStateOf(false) }
    var showSummaryHeader by remember { mutableStateOf(false) }
    var showSummaryContent by remember { mutableStateOf(false) }
    
    var displayedText by remember { mutableStateOf("") }
    
    LaunchedEffect(response, isSummarizing) {
        delay(200)
        showPrompt = true
        delay(300)
        showSummaryHeader = true
        delay(250)
        showSummaryContent = true
        displayedText = ""
        if (!isSummarizing && response.isNotBlank()) {
            delay(150)
            response.forEachIndexed { index, _ ->
                displayedText = response.substring(0, index + 1)
                delay(15)
            }
        }
    }

    // Shimmering border animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.05f),
            NotiBlue.copy(alpha = 0.2f),
            Color.White.copy(alpha = 0.05f)
        ),
        start = androidx.compose.ui.geometry.Offset(shimmerOffset, shimmerOffset),
        end = androidx.compose.ui.geometry.Offset(shimmerOffset + 200f, shimmerOffset + 200f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
            .verticalScroll(scrollState)
    ) {
        // PROMPT SECTION
        androidx.compose.animation.AnimatedVisibility(
            visible = showPrompt,
            enter = fadeIn(tween(600)) + expandVertically(tween(600, easing = EaseOutExpo))
        ) {
            Column {
                Text(
                    "YOUR REQUEST",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = NotiBlue.copy(alpha = 0.8f),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = prompt,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        // SUMMARY HEADER SECTION
        androidx.compose.animation.AnimatedVisibility(
            visible = showSummaryHeader,
            enter = fadeIn(tween(600)) + androidx.compose.animation.slideInVertically(tween(600)) { it / 2 }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = OtpGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "PINGMATE INTELLIGENCE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = OtpGold,
                    letterSpacing = 1.2.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // SUMMARY CONTENT SECTION (loading: blinking cursor; done: typewriter text)
        androidx.compose.animation.AnimatedVisibility(
            visible = showSummaryContent,
            enter = fadeIn(tween(800)) + scaleIn(tween(800, easing = EaseOutBack), initialScale = 0.95f)
        ) {
            Surface(
                color = Color(0xFF0D0D15),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, shimmerBrush),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                    if (isSummarizing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Summarizing…",
                                color = Color(0xFFB8BCC8),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                            val cursorVisible by rememberInfiniteTransition(label = "cursor").animateFloat(
                                initialValue = 1f,
                                targetValue = 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(530),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "blink"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(width = 10.dp, height = 20.dp)
                                    .graphicsLayer { alpha = cursorVisible }
                                    .background(NotiBlue, RoundedCornerShape(2.dp))
                            )
                        }
                    } else {
                        Text(
                            text = displayedText,
                            color = Color(0xFFE4E4EB),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (displayedText.length < response.length) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(width = 10.dp, height = 20.dp)
                                    .background(NotiBlue, RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isSummarizing) "Processing…" else "Summarized contextually",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        if (!isSummarizing) {
                            Box(
                                modifier = Modifier
                                    .background(NotiBlue.copy(alpha = 0.12f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = NotiBlue, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gemini AI", fontSize = 9.sp, color = NotiBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

