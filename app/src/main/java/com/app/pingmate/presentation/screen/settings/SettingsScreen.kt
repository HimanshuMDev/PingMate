package com.app.pingmate.presentation.screen.settings

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pingmate.ui.theme.*
import com.app.pingmate.utils.OfflineSummarizationEngine

private val EntranceEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(OfflineSummarizationEngine.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var apiKey by remember { mutableStateOf(prefs.getString(OfflineSummarizationEngine.KEY_GEMINI_API, "") ?: "") }
    var saved by remember { mutableStateOf(false) }
    var keyVisible by remember { mutableStateOf(false) }

    // Smooth entrance
    val headerAlpha = remember { Animatable(0f) }
    val headerSlide = remember { Animatable(20f) }
    val cardAlpha   = remember { Animatable(0f) }
    val cardSlide   = remember { Animatable(24f) }
    val spec = tween<Float>(420, easing = EntranceEasing)

    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, spec)
        headerSlide.animateTo(0f, spec)
        kotlinx.coroutines.delay(80)
        cardAlpha.animateTo(1f, spec)
        cardSlide.animateTo(0f, spec)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF08080F), Color(0xFF0D0D18))))
    ) {
        // Ambient top glow
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-60).dp)
                .background(
                    Brush.radialGradient(listOf(NotiBlue.copy(alpha = 0.08f), Color.Transparent)),
                    CircleShape
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Settings",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF14141E), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section header
                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = headerAlpha.value
                        translationY = headerSlide.value
                    }
                ) {
                    Text(
                        text = "AI Configuration",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Info card
                    Surface(
                        color = Color(0xFF0F0F1C),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C1C30)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(NotiBlue.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = NotiBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Gemini API Key",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Add your free API key from Google AI Studio to enable intelligent voice summaries. You can understand any way you ask.",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = NotiBlue.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "FREE at aistudio.google.com",
                                        fontSize = 10.sp,
                                        color = NotiBlue,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // API Key input card
                Surface(
                    color = Color(0xFF0F0F1C),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C1C30)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = cardAlpha.value
                            translationY = cardSlide.value
                        }
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "API KEY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it; saved = false },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Paste your Gemini API key here", color = TextHint, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Key, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { keyVisible = !keyVisible }) {
                                    Icon(
                                        if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null, tint = TextMuted, modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = NotiBlue,
                                focusedBorderColor = NotiBlue,
                                unfocusedBorderColor = Color(0xFF1C1C30),
                                focusedContainerColor = Color(0xFF12121E),
                                unfocusedContainerColor = Color(0xFF12121E)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Save button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFF3A6EF0), Color(0xFF8B50F5)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = {
                                    val trimmed = apiKey.trim()
                                    val ok = prefs.edit().putString(OfflineSummarizationEngine.KEY_GEMINI_API, trimmed).commit()
                                    Log.d("PingMateAI", "API key saved: $ok, len=${trimmed.length}")
                                    saved = true
                                },
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                elevation = null
                            ) {
                                AnimatedContent(
                                    targetState = saved,
                                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                                    label = "saveState"
                                ) { isSaved ->
                                    if (isSaved) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Saved!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    } else {
                                        Text("Save API Key", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }
                        }

                        // Saved confirmation
                        AnimatedContent(
                            targetState = saved,
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                            label = "savedMsg"
                        ) { isSaved ->
                            if (isSaved) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Key saved · ${apiKey.trim().length} characters · Voice AI is ready",
                                            fontSize = 12.sp,
                                            color = SuccessGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.height(0.dp))
                            }
                        }
                    }
                }

                // Privacy note
                Text(
                    text = "Your API key is stored locally on this device only and never transmitted to our servers.",
                    fontSize = 11.sp,
                    color = TextHint,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .graphicsLayer { alpha = cardAlpha.value }
                )
            }
        }
    }
}
