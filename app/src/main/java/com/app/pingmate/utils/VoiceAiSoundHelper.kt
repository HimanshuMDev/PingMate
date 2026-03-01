package com.app.pingmate.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Plays short feedback sounds for Voice AI: when user taps to start listening,
 * and when speech ends and analysis starts.
 */
object VoiceAiSoundHelper {
    private const val TAG = "VoiceAiSound"
    private const val TONE_DURATION_MS = 180

    /** Call when user taps AI assistant and listening is about to start. */
    fun playListeningStarted(context: Context) {
        playTone(context, ToneGenerator.TONE_PROP_PROMPT)
    }

    /** Call when speech has ended and we are about to analyze (prompting/understanding starts). */
    fun playProcessingStarted(context: Context) {
        playTone(context, ToneGenerator.TONE_CDMA_CONFIRM)
    }

    private fun playTone(context: Context, toneType: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val volume = audioManager?.getStreamVolume(AudioManager.STREAM_NOTIFICATION) ?: 5
            val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) ?: 7
            val volumeRatio = if (maxVol > 0) volume.toFloat() / maxVol else 0.5f
            val volumePct: Int = (volumeRatio * 100).toInt().coerceIn(1, 100)
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volumePct)
            toneGen.startTone(toneType, TONE_DURATION_MS)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    toneGen.release()
                } catch (e: Exception) {
                    Log.w(TAG, "ToneGenerator release: ${e.message}")
                }
            }, (TONE_DURATION_MS + 50).toLong())
        } catch (e: Exception) {
            Log.w(TAG, "Play tone failed: ${e.message}")
        }
    }
}
