package com.app.pingmate.presentation.screen.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.pingmate.data.local.PingMateDatabase
import com.app.pingmate.data.local.dao.NotificationDao
import com.app.pingmate.ui.theme.*
import com.app.pingmate.utils.OfflineSummarizationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToChooseApps: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember {
        context.applicationContext.getSharedPreferences("PingMatePrefs", Context.MODE_PRIVATE)
    }

    var trackedAppsForAi by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var aiExcludedSet by remember { mutableStateOf<Set<String>>(OfflineSummarizationEngine.getAiExcludedPackages(context)) }
    var isLoadingTrackedApps by remember { mutableStateOf(true) }
    var geminiApiKey by remember { mutableStateOf(prefs.getString(OfflineSummarizationEngine.KEY_GEMINI_API_KEY, "") ?: "") }
    var geminiKeyVisible by remember { mutableStateOf(false) }
    var showClearMessagesDialog by remember { mutableStateOf(false) }
    var clearDialogAppList by remember { mutableStateOf<List<Triple<String, String, Int>>>(emptyList()) }
    var isLoadingClearList by remember { mutableStateOf(false) }
    var confirmClearPackage by remember { mutableStateOf<String?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val notificationDao = remember { PingMateDatabase.getInstance(context.applicationContext).notificationDao }

    LaunchedEffect(showClearMessagesDialog) {
        if (showClearMessagesDialog) {
            isLoadingClearList = true
            clearDialogAppList = withContext(Dispatchers.IO) {
                val counts = notificationDao.getPackageNamesWithCounts()
                val pm = context.packageManager
                counts.mapNotNull { (pkg, count) ->
                    try {
                        val info = pm.getApplicationInfo(pkg, 0)
                        Triple(pkg, pm.getApplicationLabel(info).toString(), count)
                    } catch (e: Exception) {
                        Triple(pkg, pkg, count)
                    }
                }
            }
            isLoadingClearList = false
        }
    }

    LaunchedEffect(Unit) {
        val (list, excluded) = withContext(Dispatchers.IO) {
            val tracked = prefs.getStringSet("tracked_apps", emptySet()) ?: emptySet()
            val pm = context.packageManager
            val appList = tracked.mapNotNull { pkg ->
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    pkg to pm.getApplicationLabel(info).toString()
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.second }
            val excludedPkgs = OfflineSummarizationEngine.getAiExcludedPackages(context)
            Pair(appList, excludedPkgs)
        }
        trackedAppsForAi = list
        aiExcludedSet = excluded
        isLoadingTrackedApps = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0A10), Color(0xFF0E0E18))))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = TextPrimary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Gemini API",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Surface(
                    color = Color(0xFF0E0E1A),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F1F38)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(SurfaceVariant.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Key, null, tint = NotiBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Gemini API Key",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    "Paste your key from aistudio.google.com/apikey. Used for AI summaries.",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = geminiApiKey,
                            onValueChange = { geminiApiKey = it; prefs.edit().putString(OfflineSummarizationEngine.KEY_GEMINI_API_KEY, it).apply() },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Paste your Gemini API key", color = TextMuted, fontSize = 14.sp) },
                            visualTransformation = if (geminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NotiBlue.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color(0xFF16161E),
                                unfocusedContainerColor = Color(0xFF16161E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = NotiBlue
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                            trailingIcon = {
                                IconButton(onClick = { geminiKeyVisible = !geminiKeyVisible }) {
                                    Icon(
                                        if (geminiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (geminiKeyVisible) "Hide key" else "Show key",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Apps & privacy",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Surface(
                    color = Color(0xFF0E0E1A),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F1F38)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToChooseApps() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SurfaceVariant.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Apps, null, tint = NotiBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Choose applications",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Which apps to track for notifications",
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 14.sp
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                }

                Text(
                    text = "Exclude from AI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Surface(
                    color = Color(0xFF0E0E1A),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F1F38)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Switch on to exclude an app's notifications from AI. They still appear in the list.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        when {
                            isLoadingTrackedApps -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = NotiBlue
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Loading apps…", fontSize = 12.sp, color = TextMuted)
                                }
                            }
                            trackedAppsForAi.isEmpty() -> {
                                Text(
                                    "No tracked apps. Tap \"Choose applications\" above to add some.",
                                    fontSize = 12.sp,
                                    color = TextHint,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            else -> {
                                trackedAppsForAi.forEach { (pkg, appName) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            appName,
                                            modifier = Modifier.weight(1f),
                                            fontSize = 13.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Switch(
                                            checked = pkg in aiExcludedSet,
                                            onCheckedChange = { checked ->
                                                aiExcludedSet = if (checked) aiExcludedSet + pkg else aiExcludedSet - pkg
                                                OfflineSummarizationEngine.setAiExcludedPackages(context, aiExcludedSet)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = TextPrimary,
                                                checkedTrackColor = NotiBlue,
                                                uncheckedThumbColor = TextSecondary,
                                                uncheckedTrackColor = BorderSubtle
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Clear messages",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Surface(
                    color = Color(0xFF0E0E1A),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F1F38)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearMessagesDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SurfaceVariant.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DeleteSweep, null, tint = NotiBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Clear messages",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Clear by app or clear all notifications",
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 14.sp
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (showClearMessagesDialog) {
            Dialog(
                onDismissRequest = { showClearMessagesDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight()
                        .heightIn(max = 560.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF0F0F16),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(NotiBlue.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f)))
                    ),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(NotiBlue.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.DeleteSweep, null, tint = NotiBlue, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Clear messages",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { showClearMessagesDialog = false }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (isLoadingClearList) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = NotiBlue)
                            }
                        } else if (clearDialogAppList.isEmpty()) {
                            Text(
                                "No messages to clear.",
                                color = TextMuted,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showClearMessagesDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NotiBlue),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Text("Done", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Text(
                                "Tap an app to clear its messages, or clear all below.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(clearDialogAppList) { (pkg, appName, count) ->
                                    Surface(
                                        color = Color(0xFF1A1A24),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, BorderSubtle),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { confirmClearPackage = pkg }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AndroidView(
                                                factory = { ctx ->
                                                    android.widget.ImageView(ctx).apply {
                                                        try {
                                                            setImageDrawable(ctx.packageManager.getApplicationIcon(pkg))
                                                        } catch (_: Exception) {
                                                            setImageDrawable(ctx.getDrawable(android.R.drawable.sym_def_app_icon))
                                                        }
                                                        clipToOutline = true
                                                    }
                                                },
                                                modifier = Modifier.size(44.dp).clip(CircleShape),
                                                update = { iv ->
                                                    try {
                                                        iv.setImageDrawable(iv.context.packageManager.getApplicationIcon(pkg))
                                                    } catch (_: Exception) {
                                                        iv.setImageDrawable(iv.context.getDrawable(android.R.drawable.sym_def_app_icon))
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(appName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                                Text("$count message${if (count == 1) "" else "s"}", fontSize = 12.sp, color = TextMuted)
                                            }
                                            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                onClick = { confirmClearAll = true },
                                color = UrgentRedDim.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, UrgentRed.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = UrgentRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Clear all messages", color = UrgentRed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showClearMessagesDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NotiBlue),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Text("Done", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        confirmClearPackage?.let { pkg ->
            val item = clearDialogAppList.find { it.first == pkg }
            val appName = item?.second ?: pkg
            val count = item?.third ?: 0
            Dialog(
                onDismissRequest = { confirmClearPackage = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF0F0F16),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(UrgentRed.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f)))
                    ),
                    shadowElevation = 16.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Clear messages?",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Clear $count message${if (count == 1) "" else "s"} from $appName? This cannot be undone.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { confirmClearPackage = null },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Medium)
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { notificationDao.deleteByPackageName(pkg) }
                                        clearDialogAppList = clearDialogAppList.filter { it.first != pkg }
                                        confirmClearPackage = null
                                        if (clearDialogAppList.isEmpty()) showClearMessagesDialog = false
                                        Toast.makeText(context, "Cleared messages from $appName", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = UrgentRed),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Text("Clear", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        if (confirmClearAll) {
            Dialog(
                onDismissRequest = { confirmClearAll = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF0F0F16),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(UrgentRed.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f)))
                    ),
                    shadowElevation = 16.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Clear all messages?",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "This will delete all notifications. This cannot be undone.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { confirmClearAll = false },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Medium)
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { notificationDao.clearAll() }
                                        confirmClearAll = false
                                        showClearMessagesDialog = false
                                        clearDialogAppList = emptyList()
                                        Toast.makeText(context, "All messages cleared", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = UrgentRed),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Text("Clear all", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
