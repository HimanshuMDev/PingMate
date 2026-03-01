package com.app.pingmate.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.app.pingmate.data.local.PingMateDatabase
import com.app.pingmate.data.local.entity.NotificationEntity
import com.app.pingmate.utils.NotificationIntentCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PingMateNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: PingMateDatabase

    companion object {
        @Volatile
        private var instance: PingMateNotificationService? = null

        fun getInstance(): PingMateNotificationService? = instance

        /**
         * Tries to resolve the contentIntent for a notification from currently active status bar
         * notifications (e.g. after app restart). If found, puts it in [NotificationIntentCache]
         * so the app can send it. Returns true if resolved.
         */
        fun resolveIntentFromActiveNotifications(notificationId: Int, notificationKey: String?): Boolean {
            val service = instance ?: return false
            if (notificationKey.isNullOrBlank()) return false
            return try {
                val active = service.activeNotifications
                val sbn = active?.find { it.key == notificationKey }
                val intent = sbn?.notification?.contentIntent
                if (intent != null) {
                    NotificationIntentCache.put(notificationId, intent)
                    true
                } else false
            } catch (e: Exception) {
                Log.w("PingMateService", "resolveIntentFromActiveNotifications failed", e)
                false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = PingMateDatabase.getInstance(applicationContext)
        Log.d("PingMateService", "Notification Listener Created")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        sbn?.let { notification ->
            val packageName = notification.packageName
            val isGroupSummary = (notification.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0
            
            // Ignore group summary notifications (e.g., "2 new messages") because 
            // the individual messages are delivered as separate notifications.
            if (isGroupSummary) {
                Log.d("PingMateService", "Ignoring group summary from $packageName")
                return
            }

            val extras = notification.notification.extras
            
            var title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
            var text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
            val postTime = notification.postTime

            // Sometimes the title is hidden inside EXTRA_TITLE_BIG
            val titleBig = extras.getString(android.app.Notification.EXTRA_TITLE_BIG)
            if (!titleBig.isNullOrBlank()) {
                title = titleBig
            }

            // Extract InboxStyle text lines (grouped messages in a single chat)
            val textLines = extras.getCharSequenceArray(android.app.Notification.EXTRA_TEXT_LINES)
            if (textLines != null && textLines.isNotEmpty()) {
                text = textLines.joinToString("\n")
            }

            // Check if this package is tracked by the user in SharedPreferences
            val prefs = applicationContext.getSharedPreferences("PingMatePrefs", Context.MODE_PRIVATE)
            val trackedApps = prefs.getStringSet("tracked_apps", emptySet()) ?: emptySet()
            
            if (trackedApps.contains(packageName) && text.isNotBlank()) {
                Log.d("PingMateService", "Intercepted tracked notification from: $packageName")
                val contentIntent = notification.notification.contentIntent
                val notificationKey = notification.key

                serviceScope.launch {
                    val existing = db.notificationDao.getRecentNotification(packageName, title, text)
                    if (existing != null) {
                        // Prevent duplicates by just updating the timestamp; keep key for intent resolution
                        val updated = existing.copy(timestamp = postTime, notificationKey = notificationKey)
                        db.notificationDao.updateNotification(updated)
                        contentIntent?.let { NotificationIntentCache.put(updated.id, it) }
                    } else {
                        val entity = NotificationEntity(
                            packageName = packageName,
                            title = title,
                            content = text,
                            timestamp = postTime,
                            isFavorite = false,
                            notificationKey = notificationKey
                        )
                        val insertedId = db.notificationDao.insertNotification(entity).toInt()
                        contentIntent?.let { NotificationIntentCache.put(insertedId, it) }
                    }
                }
            } else {
                 Log.v("PingMateService", "Ignored notification from untracked package or empty text: $packageName")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // We will keep notifications in our DB even if user clears them from the notification panel.
        // The DB is acting as an archive/feed that only auto-clears after 24 hrs.
    }
}
