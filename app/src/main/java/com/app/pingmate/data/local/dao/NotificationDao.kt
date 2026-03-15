package com.app.pingmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.pingmate.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

data class PackageWithCount(val packageName: String, val count: Int)

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


    @Query("SELECT * FROM notifications WHERE notificationKey = :notificationKey LIMIT 1")
    suspend fun getByNotificationKey(notificationKey: String): NotificationEntity?

    @Query("""
        SELECT COUNT(*) FROM notifications
        WHERE (:pkgName IS NULL OR packageName = :pkgName)
        AND (:isFavorite IS NULL OR isFavorite = :isFavorite)
        AND (:dateStart IS NULL OR timestamp >= :dateStart)
        AND (:dateEnd IS NULL OR timestamp < :dateEnd)
        AND (:search IS NULL OR :search = '' OR title LIKE '%' || :search || '%' OR content LIKE '%' || :search || '%')
    """)
    suspend fun getNotificationCount(
        pkgName: String?,
        isFavorite: Boolean?,
        dateStart: Long?,
        dateEnd: Long?,
        search: String?
    ): Int

    @Update
    suspend fun updateNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int)

    
    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("SELECT packageName, COUNT(*) as count FROM notifications GROUP BY packageName ORDER BY count DESC")
    suspend fun getPackageNamesWithCounts(): List<PackageWithCount>

    @Query("DELETE FROM notifications WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}
