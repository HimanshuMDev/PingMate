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
import com.app.pingmate.data.local.entity.GeneralReminderEntity
import com.app.pingmate.presentation.screen.dashboard.VoiceAiDialog
import com.app.pingmate.presentation.screen.dashboard.VoiceAiFullscreenOverlay
import com.app.pingmate.ui.theme.PingMateTheme
import com.app.pingmate.utils.OfflineSummarizationEngine
import com.app.pingmate.utils.VoiceAiSoundHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.util.Log
import java.util.Calendar
import java.util.Locale

private enum class WidgetPhase { LISTENING, PROCESSING, RESULT }

/** Parse "remind me at 2:30 pm" / "set reminder for 2:30 p.m."; returns (timeMillis, note) or null. */
private fun parseReminderFromPrompt(prompt: String): Pair<Long, String>? {
    val lower = prompt.trim().lowercase()
    if (!lower.contains("remind") && !lower.contains("reminder")) return null
    val timePattern = Regex("""(?:at|for|@)\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)?|(\d{1,2}):(\d{2})\s*(am|pm|a\.m\.|p\.m\.)?""", RegexOption.IGNORE_CASE)
    val match = timePattern.find(lower) ?: return null
    val (h1, m1, ap1, h2, m2, ap2) = match.destructured
    val hourStr = h1.ifBlank { h2 }
    val minStr = (m1.ifBlank { m2 }).ifBlank { "0" }
    val amPm = ap1.ifBlank { ap2 }.lowercase().trim()
    var hour = hourStr.toIntOrNull() ?: return null
    val minute = minStr.toIntOrNull() ?: 0
    if (amPm.contains("p") && hour in 1..11) hour += 12
    else if (amPm.contains("a") && hour == 12) hour = 0
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_MONTH, 1)
    val note = prompt.replace(Regex("""(?i)(set\s+)?(a\s+)?reminder\s+(for|at)?\s*\d{1,2}(:\d{2})?\s*(am|pm|a\.m\.|p\.m\.)?"""), "").trim().take(200)
    return Pair(cal.timeInMillis, note)
}

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
                val radiusPx = (48 * resources.displayMetrics.density).toInt().coerceIn(80, 200)
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
                when (currentState.phase) {
                    WidgetPhase.LISTENING, WidgetPhase.PROCESSING -> {
                        // Same overlay as in-app: Lottie + listening/transcription card
                        LaunchedEffect(Unit) {
                            if (currentState.phase == WidgetPhase.LISTENING) startListening(state)
                        }
                        VoiceAiFullscreenOverlay(
                            transcribedText = currentState.transcribedText,
                            isThinking = currentState.phase == WidgetPhase.PROCESSING,
                            onDismiss = { finish() }
                        )
                    }
                    WidgetPhase.RESULT -> {
                        VoiceAiDialog(
                            onDismissRequest = { finish() },
                            transcribedText = currentState.transcribedText,
                            aiResponse = currentState.aiResponse
                        )
                    }
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
                    VoiceAiSoundHelper.playProcessingStarted(this@AiWidgetActivity)
                    val reminderParsed = parseReminderFromPrompt(text)
                    processingJob?.cancel()
                    processingJob = lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val response = if (reminderParsed != null) {
                                val (timeMillis, note) = reminderParsed
                                val db = PingMateDatabase.getInstance(this@AiWidgetActivity)
                                val entity = GeneralReminderEntity(reminderTimeMillis = timeMillis, note = note)
                                val id = db.generalReminderDao.insert(entity).toInt()
                                val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                                val intent = android.content.Intent(this@AiWidgetActivity, com.app.pingmate.receiver.ReminderReceiver::class.java).apply {
                                    putExtra("EXTRA_TITLE", "PingMate Reminder")
                                    putExtra("EXTRA_MESSAGE", note.ifBlank { "You have a scheduled reminder" })
                                    putExtra("EXTRA_GENERAL_REMINDER_ID", id)
                                }
                                val pendingIntent = android.app.PendingIntent.getBroadcast(
                                    this@AiWidgetActivity,
                                    com.app.pingmate.receiver.ReminderReceiver.REQUEST_CODE_GENERAL_BASE + id,
                                    intent,
                                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                                )
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    if (alarmManager.canScheduleExactAlarms()) {
                                        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                                    } else {
                                        alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                                    }
                                } else {
                                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                                }
                                val timeStr = java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date(timeMillis))
                                "Reminder set for $timeStr. I'll remind you then."
                            } else {
                                summarizationEngine.summarize(text)
                            }
                            withContext(Dispatchers.Main) {
                                stateHolder.value = stateHolder.value.copy(
                                    phase = WidgetPhase.RESULT,
                                    aiResponse = response
                                )
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
