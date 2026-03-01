package com.app.pingmate.utils

import android.content.Context
import android.util.Log
import com.app.pingmate.data.local.dao.NotificationDao
import com.app.pingmate.data.local.entity.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Result of a single Gemini API call. */
private data class GeminiCallResult(val text: String?, val quotaExceeded: Boolean = false)

/**
 * Calls Gemini API with notification context and user prompt.
 * Logs each step to Logcat (filter by "PingMateAI") so you can see what's happening.
 */
class OfflineSummarizationEngine(
    private val context: Context,
    private val notificationDao: NotificationDao
) {
    companion object {
        private const val TAG = "PingMateAI"

        const val PREFS_NAME = "PingMatePrefs"
        const val KEY_GEMINI_API = "gemini_api_key"
        /** Apps in this set are excluded from AI context (user chose "don't analyze"). */
        const val KEY_AI_EXCLUDED_PACKAGES = "ai_excluded_packages"

        fun getAiExcludedPackages(context: Context): Set<String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getStringSet(KEY_AI_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
        }

        fun setAiExcludedPackages(context: Context, packageNames: Set<String>) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_AI_EXCLUDED_PACKAGES, packageNames)
                .apply()
        }
        // Models that support generateContent on v1beta (generativelanguage.googleapis.com)
        private val GEMINI_MODELS = listOf(
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.5-pro"
        )
        private const val GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MAX_MESSAGES_IN_CONTEXT = 80
        private const val MAX_CONTEXT_CHARS = 10_000
    }

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    private fun getApiKey(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = prefs.getString(KEY_GEMINI_API, null)?.trim()?.takeIf { it.isNotEmpty() }
        Log.d(TAG, "getApiKey: found=${key != null}, length=${key?.length ?: 0}")
        return key
    }

    suspend fun summarize(prompt: String): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== summarize() started ==========")
        Log.d(TAG, "User prompt: \"$prompt\"")

        val rawList = notificationDao.getAllNotifications().first()
        val excludedPackages = getAiExcludedPackages(context)
        val list = rawList.filter { it.packageName !in excludedPackages }
        Log.d(TAG, "Notifications loaded: ${rawList.size} total, ${list.size} after excluding ${excludedPackages.size} apps from AI")

        if (list.isEmpty()) {
            Log.d(TAG, "No notifications (or all excluded) -> returning empty message")
            return@withContext if (rawList.isEmpty()) {
                "You have no notifications yet. Summaries will appear here once you have some messages."
            } else {
                "No notifications are available for AI. You may have excluded all apps from AI analysis in Settings."
            }
        }

        val contextBlock = buildContextFromNotifications(list)
        Log.d(TAG, "Context block length: ${contextBlock.length} chars (first 200 chars: ${contextBlock.take(200)}...)")

        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "No API key in Settings -> using fallback list")
            return@withContext buildBriefListFallback(list, prompt, apiKeyMissing = true)
        }

        Log.d(TAG, "API key present (length ${apiKey.length}), calling Gemini...")

        var lastError: String? = null
        var quotaExceeded = false
        for (model in GEMINI_MODELS) {
            val result = callGemini(apiKey, contextBlock, prompt, model)
            when {
                result.text != null -> {
                    Log.d(TAG, "Gemini ($model) succeeded, reply length: ${result.text.length}")
                    return@withContext result.text
                }
                result.quotaExceeded -> {
                    Log.w(TAG, "Model $model: quota exceeded, trying next model")
                    quotaExceeded = true
                    lastError = "Quota exceeded for $model"
                }
                else -> lastError = "Model $model failed"
            }
        }

        Log.e(TAG, "All models failed. Last error: $lastError, quotaExceeded=$quotaExceeded")
        if (quotaExceeded) {
            return@withContext """Gemini free-tier quota exceeded. You can:
• Wait a minute and try again
• Check usage: https://ai.google.dev/gemini-api/docs/rate-limits

Your recent messages:
${list.take(15).joinToString("\n") { "• ${it.title} (${timeFormat.format(Date(it.timestamp))}): ${it.content.take(60)}${if (it.content.length > 60) "…" else ""}" }}"""
        }
        buildBriefListFallback(list, prompt, apiKeyMissing = false, lastError = lastError)
    }

    private fun buildContextFromNotifications(list: List<NotificationEntity>): String {
        if (list.isEmpty()) return "(No notifications yet.)"
        val sb = StringBuilder()
        list.take(MAX_MESSAGES_IN_CONTEXT).forEachIndexed { i, n ->
            val app = n.packageName.substringAfterLast(".").uppercase()
            val sender = n.title.ifBlank { "Unknown" }
            sb.append("${i + 1}. [$app] $sender at ${timeFormat.format(Date(n.timestamp))}: ${n.content}\n")
        }
        val s = sb.toString().trim()
        return if (s.length > MAX_CONTEXT_CHARS) s.take(MAX_CONTEXT_CHARS) + "\n…" else s
    }

    private fun callGemini(apiKey: String, contextBlock: String, userPrompt: String, model: String): GeminiCallResult {
        Log.d(TAG, "Calling Gemini: model=$model (key length=${apiKey.length})")

        return try {
            val systemInstruction = """You are a summary assistant for someone's notifications. Give a SHORT, CLEAR summary or answer based only on the notification list.

RULES:
- Summary request: 2-4 sentences or bullet points. Do NOT list every message.
- Who sent / what did X say: 1-2 sentences.
- Use only the notifications given. Keep under 120 words. No filler."""
            val userText = """NOTIFICATIONS (app, sender, time, message):
$contextBlock

USER REQUEST: $userPrompt

Give a brief, direct summary or answer:"""

            val body = """
                {
                  "systemInstruction": {"parts": [{"text": ${escapeJson(systemInstruction)}}]},
                  "contents": [{"parts": [{"text": ${escapeJson(userText)}}]}],
                  "generationConfig": {"maxOutputTokens": 512, "temperature": 0.2}
                }
            """.trimIndent()

            val url = URL("$GEMINI_BASE/$model:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 25_000
            conn.readTimeout = 45_000

            conn.outputStream.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { it.write(body) }
            }

            val code = conn.responseCode
            Log.d(TAG, "Gemini HTTP response code: $code")

            if (code != 200) {
                val errStream = conn.errorStream
                val errBody = errStream?.readBytes()?.toString(Charsets.UTF_8) ?: "(no body)"
                Log.e(TAG, "Gemini API error. Code=$code body=$errBody")
                val quotaExceeded = code == 429 || errBody.contains("RESOURCE_EXHAUSTED") || errBody.contains("quota")
                return GeminiCallResult(null, quotaExceeded)
            }

            val response = conn.inputStream.readBytes().toString(Charsets.UTF_8)
            Log.d(TAG, "Gemini response length: ${response.length}. First 400 chars: ${response.take(400)}")

            val parsed = parseGeminiResponse(response)
            if (parsed == null) {
                Log.e(TAG, "parseGeminiResponse returned null. Full response (first 800 chars): ${response.take(800)}")
                GeminiCallResult(null, false)
            } else {
                Log.d(TAG, "Parsed reply (first 150 chars): ${parsed.take(150)}...")
                GeminiCallResult(parsed, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed", e)
            Log.e(TAG, "Exception message: ${e.message}")
            GeminiCallResult(null, false)
        }
    }

    private fun escapeJson(s: String): String {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""
    }

    private fun parseGeminiResponse(json: String): String? {
        return try {
            val root = org.json.JSONObject(json)
            if (root.has("error")) {
                val err = root.optJSONObject("error")
                val msg = err?.optString("message", "Unknown error") ?: "Unknown error"
                Log.e(TAG, "Gemini API returned error object: $msg")
                return null
            }
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                Log.e(TAG, "No 'candidates' in response or empty. Keys: ${root.keys().asSequence().toList()}")
                return null
            }
            val first = candidates.getJSONObject(0)
            val finishReason = first.optString("finishReason", "")
            if (finishReason == "SAFETY") {
                Log.w(TAG, "finishReason=SAFETY, content blocked")
                return null
            }
            val content = first.optJSONObject("content")
            if (content == null) {
                Log.e(TAG, "No 'content' in first candidate. Candidate keys: ${first.keys().asSequence().toList()}")
                return null
            }
            val parts = content.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                Log.e(TAG, "No 'parts' in content or empty")
                return null
            }
            val raw = parts.getJSONObject(0).optString("text", "").trim()
            if (raw.isEmpty()) {
                Log.e(TAG, "First part has empty 'text'")
                return null
            }
            cleanSummaryResponse(raw)
        } catch (e: Exception) {
            Log.e(TAG, "parseGeminiResponse threw", e)
            null
        }
    }

    private fun cleanSummaryResponse(raw: String): String {
        return raw
            .replace(Regex("^(Based on (the )?(notifications?|messages?),?\\s*)", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifEmpty { raw.trim() }
    }

    private fun buildBriefListFallback(
        list: List<NotificationEntity>,
        userPrompt: String,
        apiKeyMissing: Boolean,
        lastError: String? = null
    ): String {
        val sb = StringBuilder()
        if (apiKeyMissing) {
            sb.append("No API key found. Open Settings (gear icon on home) → paste your Gemini key from aistudio.google.com/apikey → tap Save. Then try again.\n\n")
        } else {
            sb.append("AI couldn't return a result ($lastError). Check Logcat with tag \"PingMateAI\" for details.\n\n")
        }
        sb.append("Your recent messages (${list.size}):\n\n")
        list.take(30).forEach { n ->
            val sender = n.title.ifBlank { "Unknown" }
            sb.append("• $sender (${timeFormat.format(Date(n.timestamp))}): ${n.content.take(80)}${if (n.content.length > 80) "…" else ""}\n")
        }
        if (list.size > 30) sb.append("\n… and ${list.size - 30} more.")
        return sb.toString().trim()
    }
}
