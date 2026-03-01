package com.app.pingmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.pingmate.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("SELECT DISTINCT packageName FROM notifications ORDER BY timestamp DESC")
    fun getDistinctPackageNames(): Flow<List<String>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE (:pkgName IS NULL OR packageName = :pkgName) AND (:isFavorite IS NULL OR isFavorite = :isFavorite) ORDER BY timestamp DESC")
    fun getAllNotificationsPaged(pkgName: String?, isFavorite: Boolean? = null): androidx.paging.PagingSource<Int, NotificationEntity>

    @Query("SELECT * FROM notifications WHERE timestamp >= :startTs AND timestamp < :endTs AND (:pkgName IS NULL OR packageName = :pkgName) AND (:isFavorite IS NULL OR isFavorite = :isFavorite) ORDER BY timestamp DESC")
    fun getNotificationsForDatePaged(startTs: Long, endTs: Long, pkgName: String?, isFavorite: Boolean? = null): androidx.paging.PagingSource<Int, NotificationEntity>

    @Query("SELECT * FROM notifications WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isFavorite = 1 AND (:pkgName IS NULL OR packageName = :pkgName) ORDER BY timestamp DESC")
    fun getFavoriteNotificationsPaged(pkgName: String?): androidx.paging.PagingSource<Int, NotificationEntity>

    @Query("SELECT * FROM notifications WHERE reminderTime IS NOT NULL ORDER BY reminderTime ASC")
    fun getNotificationsWithReminder(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE packageName = :pkgName ORDER BY timestamp DESC")
    fun getNotificationsByApp(pkgName: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE title LIKE '%' || :searchQuery || '%' OR content LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    fun searchNotifications(searchQuery: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE (title LIKE '%' || :searchQuery || '%' OR content LIKE '%' || :searchQuery || '%') AND (:pkgName IS NULL OR packageName = :pkgName) AND (:isFavorite IS NULL OR isFavorite = :isFavorite) ORDER BY timestamp DESC")
    fun searchNotificationsPaged(searchQuery: String, pkgName: String?, isFavorite: Boolean? = null): androidx.paging.PagingSource<Int, NotificationEntity>

    @Query("SELECT * FROM notifications WHERE packageName = :pkgName AND title = :title AND content = :content ORDER BY timestamp DESC LIMIT 1")
    suspend fun getRecentNotification(pkgName: String, title: String, content: String): NotificationEntity?

    @Update
    suspend fun updateNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int)

    // Auto-clear logic: delete anything older than X milliseconds
    @Query("DELETE FROM notifications WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
    
    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
