package com.app.pingmate.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
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

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Ensure that the OS tries to restart the service if there is enough memory
        return android.app.Service.START_STICKY
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("PingMateService", "Notification Listener Disconnected")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Attempt to rebind if the system drops the connection
            requestRebind(android.content.ComponentName(this, PingMateNotificationService::class.java))
        }
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

            // Prefer EXTRA_BIG_TEXT when present (BigTextStyle) so we store full/large content
            val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()
            if (!bigText.isNullOrBlank()) {
                text = bigText
            } else {
                // Fallback: InboxStyle text lines (grouped messages in a single chat)
                val textLines = extras.getCharSequenceArray(android.app.Notification.EXTRA_TEXT_LINES)
                if (textLines != null && textLines.isNotEmpty()) {
                    text = textLines.joinToString("\n")
                }
            }

            // Check if this package is tracked by the user in SharedPreferences
            val prefs = applicationContext.getSharedPreferences("PingMatePrefs", Context.MODE_PRIVATE)
            val trackedApps = prefs.getStringSet("tracked_apps", emptySet()) ?: emptySet()
            
            if (trackedApps.contains(packageName) && text.isNotBlank()) {
                Log.d("PingMateService", "Intercepted tracked notification from: $packageName")
                val contentIntent = notification.notification.contentIntent
                val notificationKey = notification.key

                // Extract user profile / large icon (WhatsApp, Instagram, etc. sender avatar)
                // On Android 10+ extras often contain Icon, not Bitmap; and we must use Notification.largeIcon (Icon) and convert.
                val largeIconBase64: String? = try {
                    val notif = notification.notification
                    var bmp: Bitmap? = null

                    // 1) Try Bitmap from extras (older apps / some OEMs)
                    @Suppress("DEPRECATION")
                    bmp = extras.getParcelable(android.app.Notification.EXTRA_LARGE_ICON_BIG) as? Bitmap
                        ?: extras.getParcelable(android.app.Notification.EXTRA_LARGE_ICON) as? Bitmap
                    if (bmp == null) bmp = extras.get(android.app.Notification.EXTRA_LARGE_ICON) as? Bitmap
                    if (bmp == null && Build.VERSION.SDK_INT >= 33) {
                        bmp = extras.getParcelable(android.app.Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
                            ?: extras.getParcelable(android.app.Notification.EXTRA_LARGE_ICON_BIG, Bitmap::class.java)
                    }

                    // 2) Try Icon from extras (common on API 23+; extras may hold Icon, not Bitmap)
                    if (bmp == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        @Suppress("DEPRECATION")
                        val iconFromExtras = extras.getParcelable(android.app.Notification.EXTRA_LARGE_ICON) as? android.graphics.drawable.Icon
                            ?: extras.getParcelable(android.app.Notification.EXTRA_LARGE_ICON_BIG) as? android.graphics.drawable.Icon
                        if (iconFromExtras != null) {
                            bmp = iconToBitmap(iconFromExtras, applicationContext, packageName)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= 33) {
                        val iconFromExtras = extras.getParcelable(android.app.Notification.EXTRA_LARGE_ICON, android.graphics.drawable.Icon::class.java)
                            ?: extras.getParcelable(android.app.Notification.EXTRA_LARGE_ICON_BIG, android.graphics.drawable.Icon::class.java)
                        if (bmp == null && iconFromExtras != null) {
                            bmp = iconToBitmap(iconFromExtras, applicationContext, packageName)
                        }
                    }

                    // 3) Primary: Notification.largeIcon (Icon) - most reliable on API 23+
                    if (bmp == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        @Suppress("DEPRECATION")
                        val largeIcon = notif.largeIcon
                        if (largeIcon is Bitmap) {
                            bmp = largeIcon
                        } else if (largeIcon != null) {
                            bmp = iconToBitmap(largeIcon as android.graphics.drawable.Icon, applicationContext, packageName)
                        }
                    }

                    bmp?.let { bitmap ->
                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                        android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    Log.w("PingMateService", "Could not extract largeIcon (user profile): ${e.message}", e)
                    null
                }

                // Removing big picture extraction to save memory and avoid storing large images
                val bigPictureBase64: String? = null

                serviceScope.launch {
                    val entity = NotificationEntity(
                        packageName = packageName,
                        title = title,
                        content = text,
                        timestamp = postTime,
                        isFavorite = false,
                        notificationKey = notificationKey,
                        largeIconBase64 = largeIconBase64
                    )
                    val insertedId = db.notificationDao.insertNotification(entity).toInt()
                    contentIntent?.let { NotificationIntentCache.put(insertedId, it) }
                }

            } else {
                 Log.v("PingMateService", "Ignored notification from untracked package or empty text: $packageName")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // We will keep notifications in our DB even if user clears them from the notification panel.
        // The DB is acting as an archive/feed.
    }

    /**
     * Converts notification Icon (API 23+) to Bitmap. Tries applicationContext first, then package context
     * so resource/URI icons from the notifying app can be loaded.
     */
    private fun iconToBitmap(
        icon: android.graphics.drawable.Icon,
        context: Context,
        packageName: String?
    ): Bitmap? {
        var drawable: Drawable? = null
        try {
            drawable = icon.loadDrawable(context)
        } catch (e: Exception) {
            Log.v("PingMateService", "loadDrawable(appContext) failed: ${e.message}")
        }
        if (drawable == null && !packageName.isNullOrBlank()) {
            try {
                val pkgContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
                drawable = icon.loadDrawable(pkgContext)
            } catch (e: Exception) {
                Log.v("PingMateService", "loadDrawable(packageContext) failed: ${e.message}")
            }
        }
        val d = drawable ?: return null
        var w = d.intrinsicWidth
        var h = d.intrinsicHeight
        if (w <= 0) w = 96
        if (h <= 0) h = 96
        w = w.coerceIn(1, 512)
        h = h.coerceIn(1, 512)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        d.setBounds(0, 0, canvas.width, canvas.height)
        d.draw(canvas)
        return bitmap
    }
}
