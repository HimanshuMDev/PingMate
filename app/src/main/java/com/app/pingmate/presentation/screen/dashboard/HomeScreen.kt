package com.app.pingmate.presentation.screen.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pingmate.data.local.entity.NotificationEntity
import com.app.pingmate.service.PingMateNotificationService
import com.app.pingmate.ui.theme.*
import com.app.pingmate.utils.NotificationIntentCache
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNotificationClick: (Int) -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val pagingItems = viewModel.pagedNotifications.collectAsLazyPagingItems()
    var selectedTab by remember { mutableStateOf("All") }
    val tabs = listOf("All", "VIP", "Tasks", "Pinned")

    var isSearchExpanded by remember { mutableStateOf(false) }
    var isCalendarVisible by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(context, "Notification permission is required to receive reminders.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    var isVoiceAiDialogVisible by remember { mutableStateOf(false) }
    var showReminderFor by remember { mutableStateOf<NotificationEntity?>(null) }
    var showReminderConfirmation by remember { mutableStateOf(false) }

    val transcribedText by viewModel.transcription.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isListening by viewModel.isListening.collectAsState()

    // When listening stops, process the prompt if we got one.
    LaunchedEffect(isListening) {
        if (!isListening && transcribedText.isNotBlank() && aiResponse == null && isVoiceAiDialogVisible) {
            viewModel.processAiPrompt(transcribedText)
        }
    }

    val recordAudioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isVoiceAiDialogVisible = true
            viewModel.startVoiceAi()
        } else {
            // Handle permission denial gracefully
            android.widget.Toast.makeText(context, "Microphone permission is required for Voice AI.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    if (isVoiceAiDialogVisible) {
        VoiceAiDialog(
            onDismissRequest = { 
                isVoiceAiDialogVisible = false
                viewModel.clearAiState()
            },
            transcribedText = transcribedText,
            aiResponse = aiResponse
        )
    }

    // Reminder Dialog Rendering
    showReminderFor?.let { notificationToRemind ->
        SetReminderDialog(
            notification = notificationToRemind,
            onDismiss = { showReminderFor = null },
            onSave = { timeMillis, note ->
                viewModel.setReminder(notificationToRemind, timeMillis, note, "")
                showReminderFor = null
                showReminderConfirmation = true
            }
        )
    }

    if (showReminderConfirmation) {
        AlertDialog(
            onDismissRequest = { showReminderConfirmation = false },
            title = { Text("Reminder Set", color = Color.White) },
            text = { Text("Your reminder has been successfully scheduled!", color = Color(0xFFB0B3B8)) },
            confirmButton = {
                TextButton(onClick = { showReminderConfirmation = false }) {
                    Text("OK", color = Color(0xFF6B9DFE))
                }
            },
            containerColor = Color(0xFF1E1F24),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = Color(0xFF141518), // Dark background matching the image
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF141518))) {
                TopAppBar(
                    title = {
                        if (isSearchExpanded) {
                            val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                            
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                            
                            val searchQuery by viewModel.searchQuery.collectAsState()
                            
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp)
                                    .focusRequester(focusRequester),
                                placeholder = { Text("Search notifications...", color = Color(0xFF5A5D66), fontSize = 16.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFF4A84F6)
                                ),
                                singleLine = true
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Logo",
                                        tint = Color.Black,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "NotiFlow AI",
                                    style = PingMateTypography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                                    color = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF141518),
                        titleContentColor = Color.White,
                        actionIconContentColor = Color(0xFFB0B3B8)
                    ),
                    actions = {
                        if (isSearchExpanded) {
                            IconButton(onClick = { 
                                isSearchExpanded = false
                                viewModel.updateSearchQuery("")
                            }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        } else {
                            IconButton(onClick = { isCalendarVisible = !isCalendarVisible }) {
                                Icon(
                                    imageVector = if (isCalendarVisible) Icons.Default.Close else Icons.Outlined.CalendarMonth,
                                    contentDescription = "Toggle Calendar",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { onOpenSettings() }) {
                                Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                            IconButton(onClick = { isSearchExpanded = true }) {
                                Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search", tint = Color.White)
                            }
                        }
                    }
                )
                HorizontalDivider(color = Color(0xFF2C2D31), thickness = 1.dp)

                // Calendar Strip (Horizontal Scrollable Date Selector)
                val selectedDateStartMillis by viewModel.selectedDateStartMillis.collectAsState()
                val selectedDate = remember(selectedDateStartMillis) {
                    if (selectedDateStartMillis == null) null else java.util.Date(selectedDateStartMillis!!)
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = isCalendarVisible,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    CalendarStrip(
                        selectedDate = selectedDate,
                        onDateSelected = { date ->
                            viewModel.selectDate(date?.time)
                        }
                    )
                }

                // App Filter Strip
                val distinctPackageNames by viewModel.distinctPackageNames.collectAsState()
                val selectedPackageName by viewModel.selectedPackageName.collectAsState()

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val isAllSelected = selectedPackageName == null
                        Surface(
                            color = if (isAllSelected) Color(0xFF2C3246) else Color(0xFF1E1F24),
                            shape = RoundedCornerShape(20.dp),
                            border = if (!isAllSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2D31)) else null,
                            modifier = Modifier
                                .height(32.dp)
                                .clickable { viewModel.selectPackage(null) }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    text = "All",
                                    color = if (isAllSelected) Color.White else Color(0xFFB0B3B8),
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    item {
                        val isFavSelected = selectedPackageName == "FAVORITES"
                        Surface(
                            color = if (isFavSelected) Color(0xFF2C3246) else Color(0xFF1E1F24),
                            shape = RoundedCornerShape(20.dp),
                            border = if (!isFavSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2D31)) else null,
                            modifier = Modifier
                                .height(32.dp)
                                .clickable { viewModel.selectPackage(if (isFavSelected) null else "FAVORITES") }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Filled.Star, contentDescription = "Favorites", tint = if (isFavSelected) Color(0xFFFACC15) else Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Favorites",
                                    color = if (isFavSelected) Color.White else Color(0xFFB0B3B8),
                                    fontWeight = if (isFavSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    items(distinctPackageNames) { pkg ->
                        val isSelected = selectedPackageName == pkg
                        // Extract a readable app name from the package name, or just use the last segment.
                        // For a real app, you'd use PackageManager to get the actual app label.
                        val appName = remember(pkg) {
                            try {
                                val pm = context.packageManager
                                val appInfo = pm.getApplicationInfo(pkg, 0)
                                pm.getApplicationLabel(appInfo).toString()
                            } catch (e: Exception) {
                                pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                            }
                        }

                        Surface(
                            color = if (isSelected) Color(0xFF2C3246) else Color(0xFF1E1F24),
                            shape = RoundedCornerShape(20.dp),
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2D31)) else null,
                            modifier = Modifier
                                .height(32.dp)
                                .clickable { viewModel.selectPackage(pkg) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // App Icon
                                val appIcon = remember(pkg) {
                                    try {
                                        context.packageManager.getApplicationIcon(pkg)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (appIcon != null) {
                                    Image(
                                        painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(appIcon),
                                        contentDescription = appName,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = appName,
                                    color = if (isSelected) Color.White else Color(0xFFB0B3B8),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    val permissionCheckResult = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                    if (permissionCheckResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        isVoiceAiDialogVisible = true
                        viewModel.startVoiceAi()
                    } else {
                        recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                },
                containerColor = Color(0xFF6B9DFE),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", modifier = Modifier.size(28.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val context = androidx.compose.ui.platform.LocalContext.current
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp), // Space for FAB
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filter removed from scrollable layout

                // Header
                item {
                    val filteredCount = pagingItems.itemCount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT ALERTS",
                            style = PingMateTypography.titleSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFFE4E6EB)
                        )
                        Surface(
                            color = Color(0xFF1E283F),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$filteredCount New",
                                color = Color(0xFF6B9DFE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (pagingItems.itemCount == 0 && pagingItems.loadState.refresh !is androidx.paging.LoadState.Loading) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HorizontalDivider(color = Color(0xFF2C2D31), thickness = 2.dp, modifier = Modifier.width(40.dp).clip(RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ALL CAUGHT UP FOR NOW",
                                color = Color(0xFF5A5D66),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                } else {
                    items(
                        count = pagingItems.itemCount,
                        key = pagingItems.itemKey { it.id }
                    ) { index ->
                        val notification = pagingItems[index]
                        if (notification != null) {
                            NotificationCard(
                                notification = notification,
                                modifier = Modifier,
                                onClick = {
                                    var contentIntent = NotificationIntentCache.get(notification.id)
                                    android.util.Log.d("PingMateClick", "Clicked ${notification.packageName} id=${notification.id}. Cache hit: ${contentIntent != null}")
                                    
                                    if (contentIntent == null && !notification.notificationKey.isNullOrBlank()) {
                                        android.util.Log.d("PingMateClick", "Attempting resolution from active notifications for key=${notification.notificationKey}")
                                        PingMateNotificationService.resolveIntentFromActiveNotifications(notification.id, notification.notificationKey)
                                        contentIntent = NotificationIntentCache.get(notification.id)
                                        android.util.Log.d("PingMateClick", "Resolved from active: ${contentIntent != null}")
                                    }
                                    
                                    if (contentIntent != null) {
                                        try {
                                            android.util.Log.d("PingMateClick", "Sending original contentIntent...")
                                            contentIntent.send() // Simple overload preserves all original intent extras
                                            android.util.Log.d("PingMateClick", "Pending intent sent successfully!")
                                        } catch (e: android.app.PendingIntent.CanceledException) {
                                            android.util.Log.e("PingMateClick", "PendingIntent was canceled by the OS or source app: ${e.message}")
                                            context.packageManager.getLaunchIntentForPackage(notification.packageName)?.let { context.startActivity(it) }
                                        } catch (e: Exception) {
                                            android.util.Log.e("PingMateClick", "Unknown failure calling contentIntent.send()", e)
                                            context.packageManager.getLaunchIntentForPackage(notification.packageName)?.let { context.startActivity(it) }
                                        }
                                    } else {
                                        android.util.Log.w("PingMateClick", "Could not resolve contentIntent! Falling back to app launch.")
                                        context.packageManager.getLaunchIntentForPackage(notification.packageName)?.let { context.startActivity(it) }
                                    }
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(notification) },
                                onDelete = { viewModel.deleteNotification(notification) },
                                onRemind = { 
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            showReminderFor = notification
                                        } else {
                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        showReminderFor = notification
                                    }
                                }
                            )
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HorizontalDivider(color = Color(0xFF2C2D31), thickness = 3.dp, modifier = Modifier.width(40.dp).clip(RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ALL CAUGHT UP FOR NOW",
                                color = Color(0xFF5A5D66),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCard(
    notification: NotificationEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onRemind: () -> Unit
) {
    val timeFormat = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
    val formattedTime = timeFormat.format(java.util.Date(notification.timestamp)).lowercase()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Surface(
        color = Color(0xFF1E1F24), // Matches the dark background card from the image
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2D31)),
        modifier = modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // TOP HEADER: Avatar, App Name, Time
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                // Avatar Container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    val appIcon = remember(notification.packageName) {
                        try {
                            ctx.packageManager.getApplicationIcon(notification.packageName)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (appIcon != null) {
                        Image(
                            painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(appIcon),
                            contentDescription = notification.packageName,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Apps, 
                            contentDescription = "App Icon", 
                            tint = Color.Black, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                val pkgNameDisplay = notification.packageName.substringAfterLast(".").uppercase()
                Text(
                    text = pkgNameDisplay,
                    style = PingMateTypography.labelMedium.copy(letterSpacing = 1.sp, fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = formattedTime,
                    style = PingMateTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFB0B3B8)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // TITLE
            Text(
                text = notification.title.ifBlank { "System" },
                style = PingMateTypography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Message
            Text(
                text = notification.content,
                style = PingMateTypography.bodyMedium.copy(lineHeight = 20.sp),
                color = Color(0xFFB0B3B8),
                maxLines = 3, 
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = Color(0xFF2C2D31), thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bottom Action Row matching image exactly
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reply Button (Left arrow icon + Text)
                    Row(
                        modifier = Modifier.clickable { /* Handle Reply */ },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = "Reply",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reply",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(32.dp))
                    
                    // Favorite Star (Outline by default)
                    Icon(
                        imageVector = if (notification.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (notification.isFavorite) Color(0xFFFACC15) else Color.White,
                        modifier = Modifier.size(20.dp).clickable { onFavoriteToggle() }
                    )
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    // Reminder Clock
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = "Set Reminder",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp).clickable { onRemind() }
                    )
                }
                
                // Right Side Delete (Red Trash Can)
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFE01E5A),
                    modifier = Modifier.size(20.dp).clickable { onDelete() }
                )
            }
        }
    }
}
