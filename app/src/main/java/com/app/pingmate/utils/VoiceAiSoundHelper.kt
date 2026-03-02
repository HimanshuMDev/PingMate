package com.app.pingmate.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Plays short, pleasant feedback sounds for Voice AI (softer than system beeps).
 */
object VoiceAiSoundHelper {
    private const val TAG = "VoiceAiSound"
    private const val TONE_DURATION_MS = 120

    /** Short, soft tone when user taps AI assistant and listening is about to start. */
    fun playListeningStarted(context: Context) {
        playTone(context, ToneGenerator.TONE_PROP_PROMPT, 0.45f)
    }

    /** Gentle confirm when speech ended and we are about to analyze. */
    fun playProcessingStarted(context: Context) {
        playTone(context, ToneGenerator.TONE_CDMA_CONFIRM, 0.4f)
    }

    private fun playTone(context: Context, toneType: Int, volumeScale: Float = 0.5f) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val streamVol = audioManager?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 5
            val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) ?: 7
            val ratio = if (maxVol > 0) streamVol.toFloat() / maxVol else 0.5f
            val volumePct = (ratio * volumeScale * 100).toInt().coerceIn(1, 80)
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volumePct)
            toneGen.startTone(toneType, TONE_DURATION_MS)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    toneGen.release()
                } catch (e: Exception) {
                    Log.w(TAG, "ToneGenerator release: ${e.message}")
                }
            }, (TONE_DURATION_MS + 80).toLong())
        } catch (e: Exception) {
            Log.w(TAG, "Play tone failed: ${e.message}")
        }
    }
}
