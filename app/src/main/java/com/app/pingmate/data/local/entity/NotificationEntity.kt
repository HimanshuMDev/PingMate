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
    /** System notification key (e.g. from StatusBarNotification.key) to resolve contentIntent from active notifications when cache misses. */
    val notificationKey: String? = null
)
