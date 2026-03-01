package com.app.pingmate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.pingmate.data.local.dao.NotificationDao
import com.app.pingmate.data.local.entity.NotificationEntity

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite stores column names as given. Room expects entity property name "notificationKey".
        db.execSQL("ALTER TABLE notifications ADD COLUMN notificationKey TEXT")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Rename notification_key -> notificationKey for users who ran the old migration.
        db.execSQL("ALTER TABLE notifications RENAME COLUMN notification_key TO notificationKey")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notifications ADD COLUMN reminderNote TEXT")
        db.execSQL("ALTER TABLE notifications ADD COLUMN reminderTag TEXT")
    }
}

@Database(
    entities = [NotificationEntity::class],
    version = 4,
    exportSchema = false
)
abstract class PingMateDatabase : RoomDatabase() {
    abstract val notificationDao: NotificationDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
