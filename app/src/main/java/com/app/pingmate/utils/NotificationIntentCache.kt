package com.app.pingmate.utils

import android.app.PendingIntent
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache mapping our notification DB id to the original app's
 * contentIntent (PendingIntent). When the user taps a notification in PingMate,
 * we can send this PendingIntent to open the specific chat/screen in WhatsApp,
 * Instagram, etc., instead of just launching the app.
 *
 * Only works for notifications that were received while the app was running;
 * cache is lost when the process is killed.
 */
object NotificationIntentCache {

    private const val MAX_SIZE = 500

    private val cache = ConcurrentHashMap<Int, PendingIntent>()

    fun put(notificationId: Int, pendingIntent: PendingIntent?) {
        if (pendingIntent == null) return
        if (cache.size >= MAX_SIZE) {
            // Remove oldest entries (arbitrary: remove first key)
            cache.keys.firstOrNull()?.let { cache.remove(it) }
        }
        cache[notificationId] = pendingIntent
    }

    fun get(notificationId: Int): PendingIntent? = cache[notificationId]

    fun remove(notificationId: Int) {
        cache.remove(notificationId)
    }
}
