package com.app.pingmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val isFavorite: Boolean = false,
    val reminderTime: Long? = null,
    val reminderNote: String? = null,
    val reminderTag: String? = null,
    /** System notification key for intent resolution from active notifications. */
    val notificationKey: String? = null,
    /** Base64-encoded PNG of the notification largeIcon (e.g. WhatsApp contact photo). Nullable. */
    val largeIconBase64: String? = null,
    /** Base64-encoded PNG of the notification big picture (e.g. BigPictureStyle image). Nullable. */
    val bigPictureBase64: String? = null
)
