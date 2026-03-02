package com.app.pingmate.presentation.screen.settings

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Apps
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
import com.app.pingmate.ui.theme.*
import com.app.pingmate.utils.OfflineSummarizationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToChooseApps: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(OfflineSummarizationEngine.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var apiKey by remember { mutableStateOf(prefs.getString(OfflineSummarizationEngine.KEY_GEMINI_API, "") ?: "") }
    var saved by remember { mutableStateOf(false) }
    var keyVisible by remember { mutableStateOf(false) }

    var trackedAppsForAi by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var aiExcludedSet by remember { mutableStateOf<Set<String>>(OfflineSummarizationEngine.getAiExcludedPackages(context)) }
    var isLoadingTrackedApps by remember { mutableStateOf(true) }
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
                    text = "AI Configuration",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NotiBlue,
                    letterSpacing = 1.2.sp
                )
                Surface(
                    color = Color(0xFF0E0E1A),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F1F38)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(NotiBlue.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = NotiBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Gemini API Key",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Add your API key from Google AI Studio for voice summaries.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = NotiBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "aistudio.google.com",
                                    fontSize = 10.sp,
                                    color = NotiBlue,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "API key",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it; saved = false },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Paste your Gemini API key", color = TextHint, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Key, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { keyVisible = !keyVisible }) {
                                    Icon(
                                        if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null, tint = TextMuted, modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NotiBlue,
                                focusedBorderColor = NotiBlue,
                                unfocusedBorderColor = BorderSubtle,
                                focusedContainerColor = CharcoalBackground,
                                unfocusedContainerColor = CharcoalBackground
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val trimmed = apiKey.trim()
                                prefs.edit().putString(OfflineSummarizationEngine.KEY_GEMINI_API, trimmed).commit()
                                Log.d("PingMateAI", "API key saved, len=${trimmed.length}")
                                saved = true
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NotiBlue)
                        ) {
                            AnimatedContent(
                                targetState = saved,
                                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                                label = "saveState"
                            ) { isSaved ->
                                if (isSaved) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Saved", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                } else {
                                    Text("Save API key", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }
                        }
                        AnimatedContent(
                            targetState = saved,
                            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                            label = "savedMsg"
                        ) { isSaved ->
                            if (isSaved) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Check, null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Saved · ${apiKey.trim().length} characters",
                                        fontSize = 11.sp,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(0.dp))
                            }
                        }
                    }
                }

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
                    text = "API key is stored only on this device.",
                    fontSize = 10.sp,
                    color = TextHint,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}
