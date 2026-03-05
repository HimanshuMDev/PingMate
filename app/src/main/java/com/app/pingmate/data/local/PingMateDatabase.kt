package com.app.pingmate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.pingmate.data.local.dao.GeneralReminderDao
import com.app.pingmate.data.local.dao.NotificationDao
import com.app.pingmate.data.local.entity.GeneralReminderEntity
import com.app.pingmate.data.local.entity.NotificationEntity

@Database(
    entities = [NotificationEntity::class, GeneralReminderEntity::class],
    version = 8,
    exportSchema = false
)
abstract class PingMateDatabase : RoomDatabase() {
    abstract val notificationDao: NotificationDao
    abstract val generalReminderDao: GeneralReminderDao

    companion object {
        const val DATABASE_NAME = "pingmate_db"

        @Volatile
        private var INSTANCE: PingMateDatabase? = null

        fun getInstance(context: android.content.Context): PingMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    PingMateDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
