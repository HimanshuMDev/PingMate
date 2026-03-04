package com.app.pingmate.widget

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.app.pingmate.data.local.PingMateDatabase
import com.app.pingmate.presentation.screen.dashboard.VoiceAssistantScreen
import kotlinx.coroutines.delay
import com.app.pingmate.ui.theme.PingMateTheme
import com.app.pingmate.utils.OfflineSummarizationEngine
import com.app.pingmate.utils.VoiceAiSoundHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.util.Log
import java.util.Locale

private enum class WidgetPhase { LISTENING, PROCESSING, RESULT }

class AiWidgetActivity : ComponentActivity() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var summarizationEngine: OfflineSummarizationEngine
    private var processingJob: kotlinx.coroutines.Job? = null

    private data class WidgetAiState(
        val phase: WidgetPhase = WidgetPhase.LISTENING,
        val transcribedText: String = "",
        val aiResponse: String? = null,
        val error: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                val radiusPx = (56 * resources.displayMetrics.density).toInt().coerceIn(80, 220)
                window.setBackgroundBlurRadius(radiusPx)
            } catch (e: Exception) {
                android.util.Log.w("AiWidgetActivity", "Window blur not available: ${e.message}")
            }
        }

        summarizationEngine = OfflineSummarizationEngine(
            applicationContext,
            PingMateDatabase.getInstance(applicationContext).notificationDao
        )
        val state = mutableStateOf(WidgetAiState())

        setContent {
            PingMateTheme {
                val currentState = state.value
                VoiceAssistantScreen(
                    transcribedText = currentState.transcribedText,
                    isListening = currentState.phase == WidgetPhase.LISTENING,
                    isProcessing = currentState.phase == WidgetPhase.PROCESSING,
                    aiResponse = currentState.aiResponse,
                    onStartListening = { startListening(state) },
                    onProcessPrompt = { runSummarize(state, it) },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun runSummarize(stateHolder: MutableState<WidgetAiState>, prompt: String) {
        stateHolder.value = stateHolder.value.copy(phase = WidgetPhase.PROCESSING)
        VoiceAiSoundHelper.playProcessingStarted(this)
        processingJob?.cancel()
        processingJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = summarizationEngine.summarize(prompt)
                withContext(Dispatchers.Main) {
                    stateHolder.value = stateHolder.value.copy(phase = WidgetPhase.RESULT, aiResponse = response)
                }
            } catch (e: Exception) {
                Log.e("AiWidgetActivity", "Summarize failed", e)
                withContext(Dispatchers.Main) {
                    stateHolder.value = stateHolder.value.copy(
                        phase = WidgetPhase.RESULT,
                        aiResponse = "Something went wrong: ${e.message}"
                    )
                }
            }
        }
    }

    private fun startListening(stateHolder: MutableState<WidgetAiState>) {
        VoiceAiSoundHelper.playListeningStarted(this)
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stateHolder.value = stateHolder.value.copy(
                phase = WidgetPhase.RESULT,
                transcribedText = "",
                aiResponse = "Speech recognition is not available on this device."
            )
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.e("AiWidgetActivity", "Speech error: $error")
                runOnUiThread {
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_CLIENT) {
                        stateHolder.value = stateHolder.value.copy(
                            phase = WidgetPhase.RESULT,
                            transcribedText = stateHolder.value.transcribedText,
                            aiResponse = "Could not hear clearly. Try again."
                        )
                    }
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                runOnUiThread {
                    if (text.isBlank()) {
                        stateHolder.value = stateHolder.value.copy(
                            phase = WidgetPhase.RESULT,
                            aiResponse = "No speech detected. Try again."
                        )
                        return@runOnUiThread
                    }
                    stateHolder.value = stateHolder.value.copy(
                        phase = WidgetPhase.PROCESSING,
                        transcribedText = text
                    )
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    runOnUiThread {
                        stateHolder.value = stateHolder.value.copy(transcribedText = text)
                    }
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    override fun onPause() {
        super.onPause()
        speechRecognizer?.stopListening()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        processingJob?.cancel()
        processingJob = null
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
