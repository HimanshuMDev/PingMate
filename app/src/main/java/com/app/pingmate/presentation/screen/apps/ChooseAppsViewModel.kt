package com.app.pingmate.presentation.screen.apps

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.pingmate.domain.model.AppSelectionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class ChooseAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val _appsList = MutableStateFlow<List<AppSelectionInfo>>(emptyList())
    val appsList: StateFlow<List<AppSelectionInfo>> = _appsList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val isSaveEnabled: StateFlow<Boolean> = appsList
        .map { list -> list.any { it.isEnabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            
            // 1. Create an intent to find apps that have a launcher icon
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            
            // 2. Query the package manager for activities that match this intent
            val resolveInfoList = pm.queryIntentActivities(intent, 0)
            
            // 3. Map to our domain model and filter out PingMate itself
            val validApps = resolveInfoList.mapNotNull { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                
                // Do not show our own app in the list
                if (appInfo.packageName == "com.app.pingmate") {
                    return@mapNotNull null
                }

                val appName = resolveInfo.loadLabel(pm).toString()
                val icon = try {
                    resolveInfo.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                
                AppSelectionInfo(
                    packageName = appInfo.packageName,
                    appName = appName,
                    description = "Installed App",
                    isEnabled = false,
                    iconColor = null,
                    iconDrawable = icon
                )
            }.distinctBy { it.packageName }.sortedBy { it.appName }

            val prefs = getApplication<Application>().getSharedPreferences("PingMatePrefs", android.content.Context.MODE_PRIVATE)
            val tracked = prefs.getStringSet("tracked_apps", emptySet()) ?: emptySet()
            _appsList.value = validApps.map { it.copy(isEnabled = it.packageName in tracked) }
            _isLoading.value = false
        }
    }

    fun toggleAppSelection(packageName: String) {
        _appsList.update { currentList ->
            currentList.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isEnabled = !app.isEnabled)
                } else {
                    app
                }
            }
        }
    }

    fun saveAndContinue() {
        val selectedPackages = _appsList.value.filter { it.isEnabled }.map { it.packageName }.toSet()
        
        val prefs = getApplication<Application>().getSharedPreferences("PingMatePrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("tracked_apps", selectedPackages).apply()
        
        // Also save a global boolean showing onboarding is complete
        prefs.edit().putBoolean("onboarding_complete", true).apply()
    }
}

