package com.app.pingmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.pingmate.data.local.entity.GeneralReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneralReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: GeneralReminderEntity): Long

    @Query("SELECT * FROM general_reminders ORDER BY reminderTimeMillis ASC")
    fun getAllFlow(): Flow<List<GeneralReminderEntity>>

    @Query("SELECT * FROM general_reminders WHERE id = :id")
    suspend fun getById(id: Int): GeneralReminderEntity?

    @Query("DELETE FROM general_reminders WHERE id = :id")
    suspend fun delete(id: Int)
}
