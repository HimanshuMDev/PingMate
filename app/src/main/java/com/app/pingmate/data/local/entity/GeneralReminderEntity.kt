package com.app.pingmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Standalone reminder (time only), e.g. set via AI "remind me at 2:30 PM". Not tied to a notification. */
@Entity(tableName = "general_reminders")
data class GeneralReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val reminderTimeMillis: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
