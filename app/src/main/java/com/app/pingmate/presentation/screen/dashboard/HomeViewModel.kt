package com.app.pingmate.presentation.screen.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.pingmate.data.local.PingMateDatabase
import com.app.pingmate.data.local.entity.GeneralReminderEntity
import com.app.pingmate.data.local.entity.NotificationEntity
import com.app.pingmate.utils.NotificationIntentCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import android.util.Log
import java.util.Calendar
import java.util.Locale

/** Represents one entry in the date strip: label (e.g. "Today", "Yesterday", "Feb 26") and start-of-day millis. */
data class DateStripItem(val label: String, val startOfDayMillis: Long)

/** One item in the Reminders list: either a notification with a reminder or a standalone general reminder. */
sealed class ReminderItem {
    data class NotificationReminder(val notification: NotificationEntity) : ReminderItem()
    data class GeneralReminder(val entity: GeneralReminderEntity) : ReminderItem()
}

data class ParsedReminder(val timeMillis: Long, val note: String)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PingMateDatabase.getInstance(application)
    private val notificationDao = db.notificationDao
    private val generalReminderDao = db.generalReminderDao

    val speechRecognizerManager = com.app.pingmate.utils.SpeechRecognizerManager(application)
    val transcription: StateFlow<String> = speechRecognizerManager.transcription
    val isListening: StateFlow<Boolean> = speechRecognizerManager.isListening
    val speechError: StateFlow<String?> = speechRecognizerManager.error
    
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    /** Prompt that was sent for the current/last AI result (e.g. after user edited and tapped Summarize). */
    private val _lastAiPrompt = MutableStateFlow<String?>(null)
    val lastAiPrompt: StateFlow<String?> = _lastAiPrompt.asStateFlow()

    /** True while summarization/reminder is in progress; overlay shows "Preparing…" instead of harsh "Analyzing…". */
    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _pendingAiReminder = MutableStateFlow<ParsedReminder?>(null)
    val pendingAiReminder: StateFlow<ParsedReminder?> = _pendingAiReminder.asStateFlow()

    /** Date strip: Today, Yesterday, then past days. Built once and exposed. */
    val dateStripItems: List<DateStripItem> = buildDateStrip()

    /** Currently selected date (start of day in millis). Null means 'All' dates. Defaulting to Today. */
    private val _selectedDateStartMillis = MutableStateFlow<Long?>(getStartOfTodayMillis())
    val selectedDateStartMillis: StateFlow<Long?> = _selectedDateStartMillis.asStateFlow()

    private val _selectedPackageName = MutableStateFlow<String?>(null)
    val selectedPackageName: StateFlow<String?> = _selectedPackageName.asStateFlow()

    val distinctPackageNames: StateFlow<List<String>> = notificationDao.getDistinctPackageNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Combined list: notification-based reminders + general (AI) reminders, sorted by time. */
    val remindersList: StateFlow<List<ReminderItem>> = combine(
        notificationDao.getNotificationsWithReminder(),
        generalReminderDao.getAllFlow()
    ) { notifList, generalList ->
        val notifItems = notifList.map { ReminderItem.NotificationReminder(it) }
        val generalItems = generalList.map { ReminderItem.GeneralReminder(it) }
        (notifItems + generalItems).sortedBy {
            when (it) {
                is ReminderItem.NotificationReminder -> it.notification.reminderTime ?: 0L
                is ReminderItem.GeneralReminder -> it.entity.reminderTimeMillis
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    companion object {
        private const val DATE_STRIP_DAYS = 16 // Today + Yesterday + 14 more
    }
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pagedNotifications = combine(
        _selectedDateStartMillis,
        _searchQuery,
        _selectedPackageName
    ) { dateMillis, query, pkg ->
        Triple(dateMillis, query, pkg)
    }.flatMapLatest { triple: Triple<Long?, String, String?> ->
        val dateMillis = triple.first
        val query = triple.second
        val pkg = triple.third
        
        val isFavoriteFilter = if (pkg == "FAVORITES") true else null
        val actualPkgFilter = if (pkg == "FAVORITES" || pkg == "All" || pkg == "REMINDERS") null else pkg
        
        Pager(
            config = PagingConfig(
                pageSize = 20,
                initialLoadSize = 20,
                prefetchDistance = 8,
                enablePlaceholders = false
            )
        ) {
            if (query.isNotBlank()) {
                notificationDao.searchNotificationsPaged(query, actualPkgFilter, isFavoriteFilter)
            } else if (dateMillis != null) {
                val endOfDay = getEndOfDayMillis(dateMillis)
                notificationDao.getNotificationsForDatePaged(dateMillis, endOfDay, actualPkgFilter, isFavoriteFilter)
            } else {
                notificationDao.getAllNotificationsPaged(actualPkgFilter, isFavoriteFilter)
            }
        }.flow
    }.cachedIn(viewModelScope)

    /** Total notification count for current filter (date, app, search). Updates when filters change. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val notificationCount: StateFlow<Int> = combine(
        _selectedDateStartMillis,
        _searchQuery,
        _selectedPackageName
    ) { dateMillis, query, pkg ->
        Triple(dateMillis, query, pkg)
    }.flatMapLatest { triple ->
        flow {
            val (dateMillis, query, pkg) = triple
            val isFav = if (pkg == "FAVORITES") true else null
            val actualPkg = if (pkg == "FAVORITES" || pkg == "All" || pkg == "REMINDERS") null else pkg
            val dateStart = dateMillis
            val dateEnd = dateMillis?.let { getEndOfDayMillis(it) }
            val search = query.ifBlank { null }
            val count = withContext(Dispatchers.IO) {
                notificationDao.getNotificationCount(actualPkg, isFav, dateStart, dateEnd, search)
            }
            emit(count)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun buildDateStrip(): List<DateStripItem> {
        val cal = Calendar.getInstance(Locale.getDefault())
        val dateFormat = java.text.SimpleDateFormat("MMM d", Locale.getDefault())
        val list = mutableListOf<DateStripItem>()
        for (i in 0 until DATE_STRIP_DAYS) {
            val startOfDay = getStartOfDayForOffset(cal, i)
            val label = when (i) {
                0 -> "Today"
                1 -> "Yesterday"
                else -> dateFormat.format(java.util.Date(startOfDay))
            }
            list.add(DateStripItem(label, startOfDay))
        }
        return list
    }

    private fun getStartOfTodayMillis(): Long = getStartOfDayForOffset(Calendar.getInstance(Locale.getDefault()), 0)

    private fun getStartOfDayForOffset(cal: Calendar, daysBack: Int): Long {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_MONTH, -daysBack)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getEndOfDayMillis(startOfDayMillis: Long): Long {
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.timeInMillis = startOfDayMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    fun selectDate(startOfDayMillis: Long?) {
        if (_selectedDateStartMillis.value == startOfDayMillis) return
        _selectedDateStartMillis.value = startOfDayMillis
    }

    fun selectPackage(pkg: String?) {
        if (_selectedPackageName.value == pkg) return
        _selectedPackageName.value = pkg
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(notification: NotificationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            notificationDao.updateNotification(notification.copy(isFavorite = !notification.isFavorite))
        }
    }

    fun deleteNotification(notification: NotificationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            notificationDao.deleteNotification(notification.id)
            NotificationIntentCache.remove(notification.id)
        }
    }

    fun setReminder(notification: NotificationEntity, timeMillis: Long, note: String, tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Update local database entity
            val updated = notification.copy(
                reminderTime = timeMillis,
                reminderNote = note.ifBlank { null },
                reminderTag = tag.ifBlank { null }
            )
            notificationDao.updateNotification(updated)

            // Schedule the alarm via Android AlarmManager
            val alarmManager = getApplication<Application>().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = android.content.Intent(getApplication(), com.app.pingmate.receiver.ReminderReceiver::class.java).apply {
                var messageDisplay = notification.content
                if (note.isNotBlank()) {
                    messageDisplay = if (tag.isNotBlank()) "[$tag] $note\n\n$messageDisplay" else "$note\n\n$messageDisplay"
                }
                
                putExtra("EXTRA_TITLE", notification.title)
                putExtra("EXTRA_MESSAGE", messageDisplay)
                putExtra("EXTRA_ID", notification.id)
            }
            
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                getApplication(),
                notification.id,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // Use Exact Alarm if permission is granted, otherwise fallback
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            }
            
            android.util.Log.d("PingMateReminder", "Scheduled Alarm for ${notification.title} at $timeMillis")
        }
    }

    fun setGeneralReminder(timeMillis: Long, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = GeneralReminderEntity(reminderTimeMillis = timeMillis, note = note)
            val id = generalReminderDao.insert(entity).toInt()
            val alarmManager = getApplication<Application>().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = android.content.Intent(getApplication(), com.app.pingmate.receiver.ReminderReceiver::class.java).apply {
                putExtra("EXTRA_TITLE", "PingMate Reminder")
                putExtra("EXTRA_MESSAGE", note.ifBlank { "You have a scheduled reminder" })
                putExtra("EXTRA_GENERAL_REMINDER_ID", id)
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                getApplication(),
                com.app.pingmate.receiver.ReminderReceiver.REQUEST_CODE_GENERAL_BASE + id,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            }
            Log.d("PingMateReminder", "Scheduled general reminder id=$id at $timeMillis")
        }
    }

    fun clearNotificationReminder(notification: NotificationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            notificationDao.updateNotification(notification.copy(reminderTime = null, reminderNote = null, reminderTag = null))
            val alarmManager = getApplication<Application>().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = android.content.Intent(getApplication(), com.app.pingmate.receiver.ReminderReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                getApplication(), notification.id, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun deleteGeneralReminder(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            generalReminderDao.delete(id)
            val alarmManager = getApplication<Application>().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = android.content.Intent(getApplication(), com.app.pingmate.receiver.ReminderReceiver::class.java).apply {
                putExtra("EXTRA_GENERAL_REMINDER_ID", id)
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                getApplication(),
                com.app.pingmate.receiver.ReminderReceiver.REQUEST_CODE_GENERAL_BASE + id,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private val summarizationEngine = com.app.pingmate.utils.OfflineSummarizationEngine(application, notificationDao)
    private var aiPromptJob: Job? = null

    fun startVoiceAi() {
        _aiResponse.value = null
        speechRecognizerManager.clearTranscription()
        com.app.pingmate.utils.VoiceAiSoundHelper.playListeningStarted(getApplication())
        speechRecognizerManager.startListening()
    }

    fun clearAiState() {
        aiPromptJob?.cancel()
        aiPromptJob = null
        _isAiProcessing.value = false
        speechRecognizerManager.clearTranscription()
        speechRecognizerManager.stopListening()
        _aiResponse.value = null
        _lastAiPrompt.value = null
        _pendingAiReminder.value = null
    }

    fun clearPendingReminder() {
        _pendingAiReminder.value = null
    }

    fun processAiPrompt(prompt: String) {
        if (prompt.isBlank()) return
        _lastAiPrompt.value = prompt.trim()
        _isAiProcessing.value = true
        com.app.pingmate.utils.VoiceAiSoundHelper.playProcessingStarted(getApplication())
        aiPromptJob?.cancel()
        aiPromptJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("PingMateAI", "ViewModel: processAiPrompt started for \"${prompt.take(50)}...\"")
                val summary = summarizationEngine.summarize(prompt)
                Log.d("PingMateAI", "ViewModel: got result length=${summary.length}")
                _aiResponse.value = summary
            } catch (e: Exception) {
                Log.e("PingMateAI", "ViewModel: summarize failed", e)
                _aiResponse.value = "Something went wrong: ${e.message}. Check Logcat (filter: PingMateAI)."
            }
            _isAiProcessing.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        aiPromptJob?.cancel()
        aiPromptJob = null
        speechRecognizerManager.destroy()
    }
}
