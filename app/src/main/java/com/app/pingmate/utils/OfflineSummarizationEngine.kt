package com.app.pingmate.utils

import android.content.Context
import android.util.Log
import com.app.pingmate.data.local.dao.NotificationDao
import com.app.pingmate.data.local.entity.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Result of a single Gemini API call. */
private data class GeminiCallResult(
    val text: String?,
    val quotaExceeded: Boolean = false,
    val isNetworkError: Boolean = false,
    val isNotFound: Boolean = false  // HTTP 404 — model not available; skip to next immediately
)

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

        /** SharedPreferences key for the user's Gemini API key (set in Settings). Never use hardcoded key. */
        const val KEY_GEMINI_API_KEY = "gemini_api_key"

        /** Apps in this set are excluded from AI context (user chose "don't analyze"). */
        const val KEY_AI_EXCLUDED_PACKAGES = "ai_excluded_packages"

        fun getAiExcludedPackages(context: Context): Set<String> {
            val prefs = context.getSharedPreferences("PingMatePrefs", Context.MODE_PRIVATE)
            return prefs.getStringSet(KEY_AI_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
        }

        fun setAiExcludedPackages(context: Context, packageNames: Set<String>) {
            context.getSharedPreferences("PingMatePrefs", Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_AI_EXCLUDED_PACKAGES, packageNames)
                .apply()
        }
        // Try multiple models so if one hits quota (429), we can use another with separate free-tier quota.
        // gemini-1.5-flash and gemini-1.5-flash-8b return HTTP 404 on v1beta — excluded.
        private val GEMINI_MODELS = listOf(
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.5-pro"
        )
        private const val GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        /** Send only minimal data to API: app, time, sender, content. Cap total context so model can respond fully. */
        private const val MAX_CONTEXT_CHARS = 4_200
        /** Per-notification content length — only what AI needs to summarize. */
        private const val MAX_CONTENT_CHARS_PER_MSG = 80
        /** Max notifications in one request so payload stays lean and response is not truncated. */
        private const val MAX_MESSAGES_SAFETY = 80
        /** Allow longer AI response so it can process and summarize fully (not "too low"). */
        private const val MAX_OUTPUT_TOKENS = 1024
        private const val CONNECT_TIMEOUT_MS = 35_000
        private const val READ_TIMEOUT_MS = 90_000
        private const val MAX_RETRIES = 2
    }

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    private fun getApiKey(): String? {
        val prefs = context.getSharedPreferences("PingMatePrefs", Context.MODE_PRIVATE)
        val key = prefs.getString(KEY_GEMINI_API_KEY, null)?.trim()?.takeIf { it.isNotEmpty() }
        if (key != null) {
            Log.d(TAG, "getApiKey(Settings): using user-provided key, length=${key.length}")
        } else {
            Log.d(TAG, "getApiKey(Settings): no key found – user must set Gemini API key in Settings")
        }
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

        val filteredList = filterNotificationsByPrompt(list, prompt)
        val listForContext = if (filteredList.isEmpty()) list else filteredList
        Log.d(TAG, "Context list size: ${listForContext.size} (filtered from ${list.size} by prompt)")

        val contextBlock = buildContextFromNotifications(listForContext)
        Log.d(TAG, "Context block length: ${contextBlock.length} chars (first 200 chars: ${contextBlock.take(200)}...)")

        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "No API key in Settings -> using fallback list")
            return@withContext buildBriefListFallback(list, prompt, apiKeyMissing = true)
        }

        Log.d(TAG, "API key present (length ${apiKey}), calling Gemini...")

        var lastError: String? = null
        var quotaExceeded = false
        var networkError = false
        
        for (model in GEMINI_MODELS) {
            if (networkError) break // Fail fast without trying other models if device is offline
            
            var result = callGemini(apiKey, contextBlock, prompt, model)
            // Only retry on transient failures — skip retries for quota (429) or not-found (404)
            if (result.text == null && !result.quotaExceeded && !result.isNetworkError && !result.isNotFound) {
                repeat(MAX_RETRIES) { attempt ->
                    if (result.text != null) return@repeat
                    Log.w(TAG, "Retry ${attempt + 1}/$MAX_RETRIES for $model")
                    kotlinx.coroutines.delay(800L * (attempt + 1))
                    result = callGemini(apiKey, contextBlock, prompt, model)
                    if (result.isNetworkError || result.isNotFound) return@repeat
                }
            }
            when {
                result.text != null -> {
                    Log.d(TAG, "Gemini ($model) succeeded, reply length: ${result.text.length}")
                    return@withContext result.text
                }
                result.isNetworkError -> {
                    Log.w(TAG, "Network error encountered on $model")
                    networkError = true
                    lastError = "Network error. Please check your internet connection."
                }
                result.isNotFound -> {
                    Log.w(TAG, "Model $model not available on this API version — skipping")
                    lastError = "Model $model not found (404)"
                }
                result.quotaExceeded -> {
                    Log.w(TAG, "Model $model: quota exceeded, trying next model")
                    quotaExceeded = true
                    lastError = "Quota exceeded for $model"
                }
                else -> lastError = "Model $model failed"
            }
        }

        Log.e(TAG, "All models failed. Last error: $lastError, quotaExceeded=$quotaExceeded, networkError=$networkError")
        if (networkError) {
            return@withContext "You seem to be offline. Please check your internet connection and try again."
        }
        if (quotaExceeded) {
            return@withContext """Gemini API quota exceeded.

Your free-tier daily limit is used up for today. You can:
• Wait until tomorrow (daily limits reset at midnight UTC).
• Check usage & reset time: https://ai.dev/rate-limit
• Use a different Gemini API key in Settings.

Your recent messages:
${list.take(15).joinToString("\n") { "• ${it.title} (${timeFormat.format(Date(it.timestamp))}): ${it.content.take(60)}${if (it.content.length > 60) "…" else ""}" }}"""
        }
        buildBriefListFallback(list, prompt, apiKeyMissing = false, lastError = lastError)
    }

    /**
     * Filter notifications by prompt intent: e.g. "today" -> today's only,
     * "WhatsApp" -> package contains "whatsapp". List is already sorted by timestamp DESC.
     */
    private fun filterNotificationsByPrompt(list: List<NotificationEntity>, prompt: String): List<NotificationEntity> {
        val lower = prompt.trim().lowercase()
        if (lower.isBlank()) return list

        var result = list
        val cal = Calendar.getInstance(Locale.getDefault())

        // Date filter: "today" / "todays" / "today's"
        if (lower.contains("today")) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startToday = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val endToday = cal.timeInMillis
            result = result.filter { it.timestamp in startToday until endToday }
        }
        if (lower.contains("yesterday")) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.DAY_OF_MONTH, -1)
            val startYesterday = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val endYesterday = cal.timeInMillis
            result = result.filter { it.timestamp in startYesterday until endYesterday }
        }

        // App filter: match prompt words to package name (e.g. "whatsapp" -> com.whatsapp)
        val words = lower.split(Regex("\\s+")).filter { it.length > 2 }
        for (word in words) {
            val match = result.filter { it.packageName.contains(word, ignoreCase = true) }
            if (match.isNotEmpty()) {
                result = match
                break
            }
        }
        return result
    }

    /**
     * Build minimal payload for API: only what AI needs — app name, time, sender, content.
     * Excluded apps are already filtered out before this is called. One line per notification.
     */
    private fun buildContextFromNotifications(list: List<NotificationEntity>): String {
        if (list.isEmpty()) return "(No notifications.)"
        val sb = StringBuilder()
        for ((i, n) in list.withIndex()) {
            if (i >= MAX_MESSAGES_SAFETY) break
            if (sb.length >= MAX_CONTEXT_CHARS) break
            val app = n.packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            val time = timeFormat.format(Date(n.timestamp))
            val sender = n.title.ifBlank { "—" }.replace("\n", " ").take(40)
            val content = n.content
                .replace("\n", " ")
                .trim()
                .take(MAX_CONTENT_CHARS_PER_MSG)
                .let { if (n.content.length > MAX_CONTENT_CHARS_PER_MSG) "$it…" else it }
            val line = "$app\t$time\t$sender\t$content\n"
            if (sb.length + line.length > MAX_CONTEXT_CHARS) break
            sb.append(line)
        }
        return sb.toString().trim().ifEmpty { "(No notifications.)" }
    }

    private fun callGemini(apiKey: String, contextBlock: String, userPrompt: String, model: String): GeminiCallResult {
        Log.d(TAG, "Calling Gemini: model=$model (key length=${apiKey.length})")

        return try {
            val systemInstruction = """You are a summary assistant. You receive a minimal list: each line is app name, time, sender, content (tab-separated). Excluded apps are already filtered out — only relevant notifications are sent.

RULES:
- Answer based only on this list. Give a clear, complete summary or answer.
- For "summarize" or "what did X say": cover the main points; 3–6 sentences or bullet points is fine.
- For "who said Y": name the sender(s) and context. Be thorough but concise.
- Do not say "based on the notifications" — just answer. No filler."""
            val userText = """NOTIFICATIONS (columns: app, time, sender, content):
$contextBlock

USER REQUEST: $userPrompt

Answer:"""

            val body = """
                {
                  "systemInstruction": {"parts": [{"text": ${escapeJson(systemInstruction)}}]},
                  "contents": [{"parts": [{"text": ${escapeJson(userText)}}]}],
                  "generationConfig": {"maxOutputTokens": $MAX_OUTPUT_TOKENS, "temperature": 0.2}
                }
            """.trimIndent()

            val url = URL("$GEMINI_BASE/$model:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS

            conn.outputStream.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { it.write(body) }
            }

            val code = conn.responseCode
            Log.d(TAG, "Gemini HTTP response code: $code")

            if (code != 200) {
                val errStream = conn.errorStream
                val errBody = errStream?.readBytes()?.toString(Charsets.UTF_8) ?: "(no body)"
                Log.e(TAG, "Gemini API error. Code=$code body=$errBody")
                val isNotFound = code == 404
                val quotaExceeded = code == 429 || errBody.contains("RESOURCE_EXHAUSTED") || errBody.contains("quota")
                return GeminiCallResult(null, quotaExceeded, isNotFound = isNotFound)
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
            val isNetworkErr = e is java.net.UnknownHostException || e is java.net.ConnectException || e is java.net.SocketTimeoutException
            GeminiCallResult(null, false, isNetworkError = isNetworkErr)
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
            if (finishReason == "MAX_TOKENS") {
                Log.w(TAG, "finishReason=MAX_TOKENS — response may be truncated; consider increasing maxOutputTokens")
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
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val text = parts.getJSONObject(i).optString("text", "")
                if (text.isNotEmpty()) sb.append(text)
            }
            val raw = sb.toString().trim()
            if (raw.isEmpty()) {
                Log.e(TAG, "All parts have empty 'text'")
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
