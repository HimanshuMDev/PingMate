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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

@Composable
fun VoiceAiDialog(
    onDismissRequest: () -> Unit,
    transcribedText: String,
    aiResponse: String? = null
) {
    // Entrance animations
    val enterScale = remember { Animatable(0.9f) }
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
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(enabled = true, onClick = onDismissRequest),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .scale(enterScale.value)
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
                    // Top Header
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
                        IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Structured Result Area
                    StructuredAiResult(
                        prompt = transcribedText,
                        response = aiResponse ?: ""
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}


@Composable
fun StructuredAiResult(prompt: String, response: String) {
    val scrollState = rememberScrollState()
    
    // Staggered entrance states
    var showPrompt by remember { mutableStateOf(false) }
    var showSummaryHeader by remember { mutableStateOf(false) }
    var showSummaryContent by remember { mutableStateOf(false) }
    
    // Typewriter effect logic
    var displayedText by remember { mutableStateOf("") }
    
    LaunchedEffect(response) {
        // Step 1: Show Prompt
        delay(300)
        showPrompt = true
        
        // Step 2: Show Summary Header
        delay(500)
        showSummaryHeader = true
        
        // Step 3: Show Summary Content Surface
        delay(400)
        showSummaryContent = true
        
        // Step 4: Start typewriter
        displayedText = ""
        if (response.isNotBlank()) {
            delay(200)
            response.forEachIndexed { index, _ ->
                displayedText = response.substring(0, index + 1)
                delay(15) // Typewriter speed
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
            .heightIn(max = 480.dp)
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = NotiBlue.copy(alpha = 0.8f),
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = prompt,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
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
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "PINGMATE INTELLIGENCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = OtpGold,
                    letterSpacing = 1.5.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // SUMMARY CONTENT SECTION
        androidx.compose.animation.AnimatedVisibility(
            visible = showSummaryContent,
            enter = fadeIn(tween(800)) + scaleIn(tween(800, easing = EaseOutBack), initialScale = 0.95f)
        ) {
            Surface(
                color = Color(0xFF0D0D15),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, shimmerBrush),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = displayedText,
                        color = Color(0xFFE4E4EB),
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (displayedText.length < response.length) {
                        // Dynamic Cursor
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(width = 10.dp, height = 20.dp)
                                .background(NotiBlue, RoundedCornerShape(2.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Premium Metadata Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Summarized contextually",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Box(
                            modifier = Modifier
                                .background(NotiBlue.copy(alpha = 0.12f), CircleShape)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome, 
                                    null, 
                                    tint = NotiBlue, 
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Processed Locally",
                                    fontSize = 10.sp,
                                    color = NotiBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

