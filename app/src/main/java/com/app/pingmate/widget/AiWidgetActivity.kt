package com.app.pingmate.widget

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.util.Log
import java.util.Locale

class AiWidgetActivity : ComponentActivity() {

    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the activity window transparent
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            AiWidgetOverlay(
                onDismiss = { finish() },
                onStartListening = { startSpeechRecognition() },
                onStopListening = { speechRecognizer?.stopListening() }
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // Dismiss if user navigates away
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.e("AiWidgetActivity", "Speech error: $error")
            }
            override fun onResults(results: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }
}

@Composable
private fun AiWidgetOverlay(
    onDismiss: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    var isListening by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Tap the mic and speak your command") }

    // Pulsing animation for the listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Prevent click-through to dismiss when tapping the card itself
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF141518),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PingMate AI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFB0B3B8))
                    }
                }

                // Mic button with pulse animation when active
                Box(contentAlignment = Alignment.Center) {
                    if (isListening) {
                        // Outer pulse ring
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(pulseScale)
                                .background(
                                    brush = Brush.radialGradient(listOf(Color(0xFF4A84F6).copy(alpha = 0.3f), Color.Transparent)),
                                    shape = CircleShape
                                )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Brush.linearGradient(listOf(Color(0xFF4A84F6), Color(0xFF9B59B6)))
                                else Brush.linearGradient(listOf(Color(0xFF2C2D31), Color(0xFF1E1F24)))
                            )
                            .clickable {
                                isListening = !isListening
                                if (isListening) {
                                    statusText = "Listening… speak now"
                                    onStartListening()
                                } else {
                                    statusText = "Tap the mic and speak your command"
                                    onStopListening()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (isListening) Color.White else Color(0xFFB0B3B8),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Status text
                AnimatedContent(targetState = statusText, label = "statusText") { text ->
                    Text(
                        text = text,
                        color = if (isListening) Color(0xFF4A84F6) else Color(0xFFB0B3B8),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Suggestion chips
                if (!isListening) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Summarize today's WhatsApp",
                            "Show unread messages",
                            "What's urgent today?"
                        ).forEach { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E1F24),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2D31)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { statusText = "\"$suggestion\" — open the app to view results!" }
                            ) {
                                Text(
                                    text = suggestion,
                                    color = Color(0xFFB0B3B8),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
