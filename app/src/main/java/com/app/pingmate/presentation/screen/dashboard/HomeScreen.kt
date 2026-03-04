package com.app.pingmate.presentation.screen.dashboard

/**
 * Main dashboard (home) screen: notification list, filters, reminders tab, and Voice AI FAB.
 * Uses app logo in the top bar and supports search, date filter, and notification actions.
 */

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pingmate.data.local.entity.GeneralReminderEntity
import com.app.pingmate.data.local.entity.NotificationEntity
import com.app.pingmate.service.PingMateNotificationService
import com.app.pingmate.ui.theme.*
import com.app.pingmate.utils.NotificationIntentCache
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.drawable.toBitmap
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.airbnb.lottie.compose.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNotificationClick: (Int) -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val pagingItems = viewModel.pagedNotifications.collectAsLazyPagingItems()
    val selectedPackageName by viewModel.selectedPackageName.collectAsState()
    val remindersList by viewModel.remindersList.collectAsState()
    val context = LocalContext.current

    var isSearchExpanded by remember { mutableStateOf(false) }
    var isFilterExpanded by remember { mutableStateOf(false) }
    var showAssistantScreen by remember { mutableStateOf(false) }
    var showReminderFor by remember { mutableStateOf<NotificationEntity?>(null) }
    var detailNotification by remember { mutableStateOf<NotificationEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val transcribedText by viewModel.transcription.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val lastAiPrompt by viewModel.lastAiPrompt.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val pendingAiReminder by viewModel.pendingAiReminder.collectAsState()

    val notificationPermissionLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                android.widget.Toast.makeText(
                    context,
                    "Notification permission is required to receive reminders.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

    val recordAudioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showAssistantScreen = true
        } else {
            android.widget.Toast.makeText(
                context,
                "Microphone permission is required for Voice AI.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val distinctPackageNames by viewModel.distinctPackageNames.collectAsState()
    val notificationCount by viewModel.notificationCount.collectAsState()

    val isAssistantProcessing = !isListening && transcribedText.isNotBlank() && aiResponse == null

    val anyAssistantVisible = showAssistantScreen
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFF09090F),
            topBar = {
                Column(
                    modifier = Modifier
                        .background(Color(0xFF09090F))
                        .statusBarsPadding()
                ) {
                    // ── Main title bar ──────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App logo (loaded via Context so adaptive icon is resolved; painterResource doesn't support adaptive-icon XML)
                        val appLogoDrawable = remember(context) {
                            ContextCompat.getDrawable(context, com.app.pingmate.R.mipmap.ic_launcher)
                        }
                        if (appLogoDrawable != null) {
                            Image(
                                painter = rememberDrawablePainter(drawable = appLogoDrawable),
                                contentDescription = "PingMate",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(11.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        if (isSearchExpanded) {
                            val focusRequester = remember { FocusRequester() }
                            LaunchedEffect(Unit) { focusRequester.requestFocus() }
                            val searchQuery by viewModel.searchQuery.collectAsState()
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                placeholder = {
                                    Text(
                                        "Search…",
                                        color = TextHint,
                                        fontSize = 15.sp
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = NotiBlue
                                ),
                                singleLine = true
                            )
                            IconButton(onClick = {
                                isSearchExpanded = false; viewModel.updateSearchQuery("")
                            }) {
                                Icon(Icons.Default.Close, null, tint = TextSecondary)
                            }
                        } else {
                            Text(
                                "PingMate",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color.White,
                                letterSpacing = (-0.5).sp,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TopBarIconBtn(icon = Icons.Outlined.Search) {
                                    isSearchExpanded = true
                                }
                                TopBarIconBtn(
                                    icon = if (isFilterExpanded) Icons.Outlined.Close else Icons.Outlined.FilterList
                                ) {
                                    isFilterExpanded = !isFilterExpanded
                                }
                                TopBarIconBtn(icon = Icons.Outlined.Settings) { onOpenSettings() }
                            }
                        }
                    }

                    // ── Inline filter chips: All, Favorites, Reminders, apps ─────────────────────
                    AnimatedVisibility(
                        visible = isFilterExpanded,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                    ) {
                        Column {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                        item {
                            FilterChipPill(
                                label = "All",
                                selected = selectedPackageName == null,
                                onClick = { viewModel.selectPackage(null) }
                            )
                        }
                        item {
                            FilterChipPill(
                                label = "Favorites",
                                selected = selectedPackageName == "FAVORITES",
                                onClick = { viewModel.selectPackage(if (selectedPackageName == "FAVORITES") null else "FAVORITES") }
                            )
                        }
                        item {
                            FilterChipPill(
                                label = "Reminders",
                                selected = selectedPackageName == "REMINDERS",
                                onClick = { viewModel.selectPackage(if (selectedPackageName == "REMINDERS") null else "REMINDERS") }
                            )
                        }
                        items(
                            distinctPackageNames,
                            key = { it }
                        ) { pkg ->
                            val appName = remember(pkg) {
                                try {
                                    context.packageManager.getApplicationLabel(
                                        context.packageManager.getApplicationInfo(
                                            pkg,
                                            0
                                        )
                                    ).toString()
                                } catch (e: Exception) {
                                    pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                                }
                            }
                            FilterChipPill(
                                label = appName,
                                selected = selectedPackageName == pkg,
                                onClick = { viewModel.selectPackage(if (selectedPackageName == pkg) null else pkg) }
                            )
                        }
                        }
                    }
                }

                // ── Date Chip Bar (hidden when Reminders tab is selected) ────────────────────
                if (selectedPackageName != "REMINDERS") {
                        val selectedDateStartMillis by viewModel.selectedDateStartMillis.collectAsState()
                        val selectedDate = remember(selectedDateStartMillis) {
                            if (selectedDateStartMillis == null) null else java.util.Date(
                                selectedDateStartMillis!!
                            )
                        }

                        var showDatePicker by remember { mutableStateOf(false) }
                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState()
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.selectDate(datePickerState.selectedDateMillis)
                                        showDatePicker = false
                                    }) { Text("Apply", color = NotiBlue) }
                                },
                                colors = DatePickerDefaults.colors(containerColor = Color(0xFF161622))
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(vertical = 14.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                val sel = selectedDate == null
                                FilterChipPill(
                                    label = "All",
                                    selected = sel
                                ) { viewModel.selectDate(null) }
                            }
                            val today = java.util.Calendar.getInstance().apply {
                                set(
                                    java.util.Calendar.HOUR_OF_DAY,
                                    0
                                ); set(java.util.Calendar.MINUTE, 0); set(
                                java.util.Calendar.SECOND,
                                0
                            ); set(java.util.Calendar.MILLISECOND, 0)
                            }.time
                            val dates = (0..6).map {
                                java.util.Calendar.getInstance().apply {
                                    time = today; add(
                                    java.util.Calendar.DAY_OF_YEAR,
                                    -it
                                )
                                }.time
                            }
                            items(dates) { date ->
                                val label = when {
                                    isSameDay(date, today) -> "Today"
                                    isSameDay(date, yesterday(today)) -> "Yesterday"
                                    else -> java.text.SimpleDateFormat(
                                        "dd MMM",
                                        java.util.Locale.getDefault()
                                    ).format(date)
                                }
                                val sel = selectedDate != null && isSameDay(date, selectedDate)
                                FilterChipPill(label = label, selected = sel) {
                                    viewModel.selectDate(if (sel) null else date.time)
                                }
                            }
                            item {
                                FilterChipPill(
                                    label = "Custom",
                                    selected = false,
                                    icon = Icons.Outlined.CalendarMonth,
                                    iconTint = TextSecondary
                                ) { showDatePicker = true }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF15151F), thickness = 1.dp)
                    }
                }
            },
            floatingActionButton = {
                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp, end = 8.dp)
                        .size(68.dp), // Slightly larger to accommodate the ring
                    contentAlignment = Alignment.Center
                ) {
                    // Circular Progress Ring around the button
                    val infiniteTransition = rememberInfiniteTransition(label = "fabRing")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )

                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotation),
                        color = NotiBlue.copy(alpha = 0.4f),
                        strokeWidth = 2.dp,
                        strokeCap = StrokeCap.Round,
                    )

                    // Pulsing glow for the ring
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glowAlpha"
                    )

                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(NotiBlue, VipPurple, NotiBlue),
                                center = center
                            ),
                            radius = size.minDimension / 2,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            ),
                            alpha = glowAlpha
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Brush.linearGradient(listOf(NotiBlue, VipPurple)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = {
                                val permissionCheckResult =
                                    androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.RECORD_AUDIO
                                    )
                                if (permissionCheckResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    showAssistantScreen = true
                                } else {
                                    recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxSize(),
                            elevation = FloatingActionButtonDefaults.elevation(
                                0.dp,
                                0.dp,
                                0.dp,
                                0.dp
                            )
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Assistant",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (selectedPackageName == "REMINDERS") {
                    RemindersListContent(
                        remindersList = remindersList,
                        onClearNotificationReminder = { viewModel.clearNotificationReminder(it) },
                        onDeleteGeneralReminder = { viewModel.deleteGeneralReminder(it) },
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 120.dp
                        ), // Space for FAB
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECENT ALERTS",
                                    style = PingMateTypography.titleSmall.copy(
                                        letterSpacing = 1.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = Color(0xFFE4E6EB)
                                )
                                if (notificationCount > 0) {
                                    Surface(
                                        color = Color(0xFF1E283F),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "$notificationCount ${if (notificationCount == 1) "Notification" else "Notifications"}",
                                            color = Color(0xFF6B9DFE),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 4.dp
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        when {
                            pagingItems.loadState.refresh is androidx.paging.LoadState.Loading && pagingItems.itemCount == 0 -> {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        repeat(5) {
                                            ShimmerNotificationCard()
                                        }
                                    }
                                }
                            }

                            pagingItems.itemCount == 0 && pagingItems.loadState.refresh !is androidx.paging.LoadState.Loading -> {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 32.dp)
                                            .padding(top = 80.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.NotificationsOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(80.dp),
                                            tint = TextMuted.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Text(
                                            text = "No Notifications Yet",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "You don't have any notifications right now.",
                                            color = TextMuted,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(top = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        TextButton(
                                            onClick = { pagingItems.retry() }
                                        ) {
                                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = NotiBlue)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Refresh", color = NotiBlue, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }

                            else -> {
                                items(
                                    count = pagingItems.itemCount,
                                    key = pagingItems.itemKey { it.id }
                                ) { index ->
                                    val notification = pagingItems[index]
                                    if (notification != null) {
                                        val dismissState = rememberSwipeToDismissBoxState(
                                            confirmValueChange = { value ->
                                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                                    viewModel.deleteNotification(notification)
                                                    true
                                                } else false
                                            }
                                        )
                                        SwipeToDismissBox(
                                            state = dismissState,
                                            enableDismissFromStartToEnd = false,
                                            enableDismissFromEndToStart = true,
                                            backgroundContent = {
                                                val isDismissing =
                                                    dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                                                val progress =
                                                    (dismissState.progress).coerceIn(0f, 1f)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(
                                                            if (isDismissing) Color(
                                                                0xFF2A1515
                                                            ) else Color.Transparent
                                                        )
                                                        .padding(end = 24.dp),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    if (isDismissing) {
                                                        val composition by rememberLottieComposition(
                                                            LottieCompositionSpec.Url("https://assets9.lottiefiles.com/packages/lf20_klid7z.json")
                                                        ) // Dust/Delete effect
                                                        LottieAnimation(
                                                            composition = composition,
                                                            iterations = 1,
                                                            modifier = Modifier.size(80.dp)
                                                        )
                                                    }
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = if (isDismissing) Color(0xFFA85A66) else Color.Transparent,
                                                        modifier = Modifier.scale(if (isDismissing) 1.2f else 1f)
                                                    )
                                                }
                                            }
                                        ) {
                                            NotificationCard(
                                                notification = notification,
                                                modifier = Modifier,
                                                compact = true,
                                                onClick = { detailNotification = notification },
                                                onFavoriteToggle = {
                                                    viewModel.toggleFavorite(
                                                        notification
                                                    )
                                                },
                                                onDelete = {
                                                    viewModel.deleteNotification(
                                                        notification
                                                    )
                                                },
                                                onRemind = {
                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                                                context,
                                                                android.Manifest.permission.POST_NOTIFICATIONS
                                                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                        ) {
                                                            showReminderFor = notification
                                                        } else {
                                                            notificationPermissionLauncher.launch(
                                                                android.Manifest.permission.POST_NOTIFICATIONS
                                                            )
                                                        }
                                                    } else {
                                                        showReminderFor = notification
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                // Paging footer: loading more, retry on error, or end marker
                                val appendState = pagingItems.loadState.append
                                item(key = "paging_footer") {
                                    when (appendState) {
                                        is androidx.paging.LoadState.Loading -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        color = NotiBlue,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Text(
                                                        text = "Loading more…",
                                                        color = TextMuted,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }

                                        is androidx.paging.LoadState.Error -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "Couldn't load more",
                                                    color = TextMuted,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                TextButton(onClick = { pagingItems.retry() }) {
                                                    Text(
                                                        "Retry",
                                                        color = NotiBlue,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        else -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 16.dp, bottom = 32.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "You're all caught up",
                                                    color = Color(0xFF5A5D66),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Detail bottom sheet: full content + quick actions when user taps a card
            detailNotification?.let { notification ->
                ModalBottomSheet(
                    onDismissRequest = { detailNotification = null },
                    sheetState = sheetState,
                    containerColor = Color(0xFF0E0E16),
                    dragHandle = { BottomSheetDefaults.DragHandle() },
                    scrimColor = Color.Black.copy(alpha = 0.72f)
                ) {
                    NotificationDetailSheetContent(
                        notification = notification,
                        onOpen = {
                            var contentIntent = NotificationIntentCache.get(notification.id)
                            if (contentIntent == null && !notification.notificationKey.isNullOrBlank()) {
                                PingMateNotificationService.resolveIntentFromActiveNotifications(
                                    notification.id,
                                    notification.notificationKey
                                )
                                contentIntent = NotificationIntentCache.get(notification.id)
                            }
                            if (contentIntent != null) {
                                try {
                                    contentIntent.send()
                                } catch (e: Exception) {
                                    context.packageManager.getLaunchIntentForPackage(notification.packageName)
                                        ?.let { context.startActivity(it) }
                                }
                            } else {
                                context.packageManager.getLaunchIntentForPackage(notification.packageName)
                                    ?.let { context.startActivity(it) }
                            }
                            detailNotification = null
                        },
                        onCopy = {
                            val text = buildString {
                                if (notification.title.isNotBlank()) append(notification.title).append(
                                    "\n\n"
                                )
                                append(notification.content)
                            }
                            val clipboard =
                                context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText(
                                    "Notification",
                                    text
                                )
                            )
                            android.widget.Toast.makeText(
                                context,
                                "Copied to clipboard",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        onSave = { viewModel.toggleFavorite(notification) },
                        onRemind = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    showReminderFor = notification
                                    detailNotification = null
                                } else {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                showReminderFor = notification
                                detailNotification = null
                            }
                        },
                        onDelete = {
                            viewModel.deleteNotification(notification); detailNotification = null
                        },
                        onDismiss = { detailNotification = null }
                    )
                }
            }


            showReminderFor?.let { notificationToRemind ->
                SetReminderDialog(
                    notification = notificationToRemind,
                    onDismiss = { showReminderFor = null },
                    onSave = { timeMillis, note ->
                        viewModel.setReminder(notificationToRemind, timeMillis, note, "")
                        showReminderFor = null
                        val dateTimeStr = java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault()).format(java.util.Date(timeMillis))
                        android.widget.Toast.makeText(context, "Reminder set for $dateTimeStr", android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }

            pendingAiReminder?.let { parsedReminder ->
                // Ensure the assistant screen is dismissed since we're now moving to the dialog
                if (showAssistantScreen) showAssistantScreen = false

                SetGeneralReminderDialog(
                    initialTimeMillis = parsedReminder.timeMillis,
                    initialNote = parsedReminder.note,
                    onDismiss = { viewModel.clearPendingReminder() },
                    onSave = { timeMillis, note ->
                        viewModel.setGeneralReminder(timeMillis, note)
                        viewModel.clearPendingReminder()
                        val dateTimeStr = java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault()).format(java.util.Date(timeMillis))
                        android.widget.Toast.makeText(context, "Reminder set for $dateTimeStr", android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        if (showAssistantScreen) {
            VoiceAssistantScreen(
                transcribedText = transcribedText,
                isListening = isListening,
                isProcessing = isAssistantProcessing,
                aiResponse = aiResponse,
                onStartListening = { viewModel.startVoiceAi() },
                onProcessPrompt = { viewModel.processAiPrompt(it) },
                onDismiss = {
                    showAssistantScreen = false
                    viewModel.clearAiState()
                }
            )
        }
    }
}

@Composable
fun RemindersListContent(
        remindersList: List<ReminderItem>,
        onClearNotificationReminder: (NotificationEntity) -> Unit,
        onDeleteGeneralReminder: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val timeFormat = remember { SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()) }
        val ctx = LocalContext.current
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "REMINDERS",
                    style = PingMateTypography.titleSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color(0xFFE4E6EB)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (remindersList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.AccessTime,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF3D3E48)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No reminders", color = Color(0xFF6B6F7A), fontSize = 15.sp)
                        Text(
                            "Set reminders via AI or from a notification card",
                            color = Color(0xFF4A4D56),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else {
                items(remindersList.size) { index ->
                    val item = remindersList[index]
                    when (item) {
                        is ReminderItem.NotificationReminder -> {
                            val n = item.notification
                            val timeStr = n.reminderTime?.let { timeFormat.format(Date(it)) } ?: ""
                            val isPassed = n.reminderTime?.let { it < System.currentTimeMillis() } == true
                            val appIcon = remember(n.packageName) {
                                try {
                                    ctx.packageManager.getApplicationIcon(n.packageName)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF0E0E12),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFF16161C)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (appIcon != null) {
                                        Image(
                                            bitmap = appIcon.toBitmap().asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))
                                        )
                                    } else {
                                        Icon(
                                            Icons.Outlined.AccessTime,
                                            null,
                                            tint = NotiBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val decoration = if (isPassed) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                                        Text(
                                            n.title.ifBlank { "Notification" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isPassed) Color(0xFF6B6F7C) else Color(0xFFD8D9DC),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textDecoration = decoration
                                        )
                                        Text(timeStr, fontSize = 12.sp, color = Color(0xFF6B6F7C), textDecoration = decoration)
                                        if (!n.reminderNote.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                n.reminderNote,
                                                fontSize = 12.sp,
                                                color = Color(0xFF8A8D98),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onClearNotificationReminder(n) }) {
                                        Icon(
                                            Icons.Outlined.DeleteOutline,
                                            "Remove",
                                            tint = Color(0xFFA85A66),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        is ReminderItem.GeneralReminder -> {
                            val e = item.entity
                            val timeStr = timeFormat.format(Date(e.reminderTimeMillis))
                            val isPassed = e.reminderTimeMillis < System.currentTimeMillis()
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF0E0E12),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFF16161C)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.AccessTime,
                                        null,
                                        tint = NotiBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val decoration = if (isPassed) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                                        Text(
                                            e.note.ifBlank { "Reminder" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isPassed) Color(0xFF6B6F7C) else Color(0xFFD8D9DC),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textDecoration = decoration
                                        )
                                        Text(timeStr, fontSize = 12.sp, color = Color(0xFF6B6F7C), textDecoration = decoration)
                                    }
                                    IconButton(onClick = { onDeleteGeneralReminder(e.id) }) {
                                        Icon(
                                            Icons.Outlined.DeleteOutline,
                                            "Remove",
                                            tint = Color(0xFFA85A66),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
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
        compact: Boolean = true,
        onClick: () -> Unit,
        onFavoriteToggle: () -> Unit,
        onDelete: () -> Unit,
        onRemind: () -> Unit
    ) {
        val timeFormat =
            remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
        val formattedTime = timeFormat.format(java.util.Date(notification.timestamp)).lowercase()
        val ctx = LocalContext.current

        // Decode contact/user photo (e.g. WhatsApp sender avatar stored from notification largeIcon)
        val largeIconBitmap = remember(notification.largeIconBase64) {
            notification.largeIconBase64?.let { b64 ->
                try {
                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }
        // Decode big picture (e.g. notification content image / BigPictureStyle)
        val bigPictureBitmap = remember(notification.bigPictureBase64) {
            notification.bigPictureBase64?.let { b64 ->
                try {
                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }

        val appIcon = remember(notification.packageName) {
            try {
                ctx.packageManager.getApplicationIcon(notification.packageName)
            } catch (e: Exception) {
                null
            }
        }
        val appLabel = remember(notification.packageName) {
            try {
                val ai = ctx.packageManager.getApplicationInfo(notification.packageName, 0)
                ctx.packageManager.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                notification.packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            }
        }

        val contentMaxLines = if (compact) 2 else 20
        // Only show content image when we don't have a profile – avoid showing profile again in message content
        val showBigPictureInCard = !compact && bigPictureBitmap != null && largeIconBitmap == null
        val showBigPictureThumbInCompact = compact && bigPictureBitmap != null && largeIconBitmap == null
        val isFav = notification.isFavorite
        // Same card style as Choose App screen: unified premium look
        val cardBg = if (isFav) Color(0xFF10182A) else Color(0xFF0E0E1A)
        val cardBorder = if (isFav) NotiBlue.copy(alpha = 0.35f) else Color(0xFF1F1F38)
        val scale by animateFloatAsState(
            targetValue = if (isFav) 1.01f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessLow), label = "cardScale"
        )
        val glowColor by animateColorAsState(
            targetValue = if (isFav) NotiBlue.copy(alpha = 0.08f) else Color.Transparent,
            animationSpec = tween(400), label = "cardGlow"
        )

        Surface(
            color = cardBg,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isFav) 1.5.dp else 1.dp,
                brush = if (isFav) Brush.horizontalGradient(
                    listOf(
                        NotiBlue,
                        VipPurple
                    )
                ) else Brush.horizontalGradient(listOf(cardBorder, cardBorder))
            ),
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .drawBehind {
                    if (isFav) {
                        drawRoundRect(
                            color = glowColor,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                            size = size
                        )
                    }
                },
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            onClick = onClick
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // User profile / large icon from system notification (e.g. WhatsApp contact photo)
                    Box(modifier = Modifier.size(48.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF121218))
                                .border(1.dp, Color(0xFF1E1E26), CircleShape)
                                .align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            if (largeIconBitmap != null) {
                                Image(
                                    bitmap = largeIconBitmap,
                                    contentDescription = "Sender",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else if (appIcon != null) {
                                androidx.compose.foundation.Image(
                                    painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(
                                        appIcon
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp).clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Notifications,
                                    null,
                                    tint = Color(0xFF5A5D68),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        if (largeIconBitmap != null && appIcon != null) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0E0E12))
                                    .border(1.dp, Color(0xFF1E1E26), CircleShape)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(
                                        appIcon
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = appLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B6F7C),
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                formattedTime,
                                fontSize = 10.sp,
                                color = Color(0xFF50535E),
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (notification.title.isNotBlank()) {
                            Text(
                                text = notification.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFFD8D9DC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                        }

                        Text(
                            text = notification.content,
                            fontSize = 12.sp,
                            color = Color(0xFF9A9DA8),
                            lineHeight = 18.sp,
                            maxLines = contentMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (showBigPictureThumbInCompact) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Image(
                                bitmap = bigPictureBitmap!!,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .heightIn(max = 72.dp)
                                    .width(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF121218))
                            )
                        }

                        Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = Color(0xFF16161C),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFF22222E)
                                ),
                                modifier = Modifier.clickable { onFavoriteToggle() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = null,
                                        tint = if (isFav) Color(0xFF9A8F6E) else Color(0xFF6B6F7C),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isFav) "Saved" else "Save",
                                        fontSize = 11.sp,
                                        color = if (isFav) Color(0xFF9A8F6E) else Color(0xFF6B6F7C),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Surface(
                                color = Color(0xFF16161C),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFF22222E)
                                ),
                                modifier = Modifier.clickable { onRemind() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.AccessTime,
                                        null,
                                        tint = Color(0xFF6B6F7C),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        "Remind",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B6F7C),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                if (showBigPictureInCard) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 14.dp)
                    ) {
                        Image(
                            bitmap = bigPictureBitmap,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF121218))
                        )
                    }
                }
            }
        }
    }

// ─────────────────────────────────────────────────────────────────────────────
// Notification detail bottom sheet: full content + quick actions
// ─────────────────────────────────────────────────────────────────────────────

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun NotificationDetailSheetContent(
        notification: NotificationEntity,
        onOpen: () -> Unit,
        onCopy: () -> Unit,
        onSave: () -> Unit,
        onRemind: () -> Unit,
        onDelete: () -> Unit,
        onDismiss: () -> Unit
    ) {
        val ctx = LocalContext.current
        val scrollState = rememberScrollState()
        val timeFormat =
            remember { java.text.SimpleDateFormat("h:mm a, MMM d", java.util.Locale.getDefault()) }
        val formattedTime = timeFormat.format(java.util.Date(notification.timestamp))

        val largeIconBitmap = remember(notification.largeIconBase64) {
            notification.largeIconBase64?.let { b64 ->
                try {
                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }
        val bigPictureBitmap = remember(notification.bigPictureBase64) {
            notification.bigPictureBase64?.let { b64 ->
                try {
                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }
        val appIcon = remember(notification.packageName) {
            try {
                ctx.packageManager.getApplicationIcon(notification.packageName)
            } catch (e: Exception) {
                null
            }
        }
        val appLabel = remember(notification.packageName) {
            try {
                val ai = ctx.packageManager.getApplicationInfo(notification.packageName, 0)
                ctx.packageManager.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                notification.packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState)
        ) {
            // Header: app icon & name + time, close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF121218))
                            .border(1.dp, Color(0xFF1E1E26), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (largeIconBitmap != null) {
                            Image(
                                bitmap = largeIconBitmap,
                                contentDescription = "Sender",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else if (appIcon != null) {
                            Image(
                                painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(
                                    appIcon
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Notifications,
                                null,
                                tint = Color(0xFF5A5D68),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            appLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B6F7C)
                        )
                        Text(formattedTime, fontSize = 11.sp, color = Color(0xFF50535E))
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = Color(0xFF6B6F7C),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (notification.title.isNotBlank()) {
                Text(
                    text = notification.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD8D9DC),
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            LinkifyText(
                text = notification.content,
                fontSize = 14.sp,
                color = Color(0xFF9A9DA8),
                linkColor = NotiBlue,
                lineHeight = 22.sp
            )

            if (bigPictureBitmap != null && largeIconBitmap == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    bitmap = bigPictureBitmap,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF121218))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF16161C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222E))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF9A9DA8)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Open",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD8D9DC)
                        )
                    }
                }
                Surface(
                    onClick = onCopy,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF16161C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222E))
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF6B6F7C)
                        )
                    }
                }
                Surface(
                    onClick = onSave,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF16161C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222E))
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            if (notification.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = if (notification.isFavorite) Color(0xFF9A8F6E) else Color(
                                0xFF6B6F7C
                            )
                        )
                    }
                }
                Surface(
                    onClick = onRemind,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF16161C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222E))
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.AccessTime,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF6B6F7C)
                        )
                    }
                }
                Surface(
                    onClick = onDelete,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF16161C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3035))
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFA85A66)
                        )
                    }
                }
            }
        }
    }


// ─────────────────────────────────────────────────────────────────────────────
// Helper composables
// ─────────────────────────────────────────────────────────────────────────────

    private val urlPattern = Regex("""(https?://[^\s]+)|(www\.[^\s]+)""")

    @Composable
    fun LinkifyText(
        text: String,
        modifier: Modifier = Modifier,
        fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
        color: Color = Color(0xFF9A9DA8),
        linkColor: Color = NotiBlue,
        lineHeight: androidx.compose.ui.unit.TextUnit = 22.sp,
        maxLines: Int = Int.MAX_VALUE
    ) {
        val context = LocalContext.current
        val annotated = remember(text) {
            buildAnnotatedString {
                if (text.isBlank()) {
                    append(text)
                    return@buildAnnotatedString
                }
                var lastEnd = 0
                urlPattern.findAll(text).forEach { match ->
                    append(text.substring(lastEnd, match.range.first))
                    val url = match.value.let { if (it.startsWith("www.")) "https://$it" else it }
                    pushStringAnnotation("URL", url)
                    withStyle(SpanStyle(color = linkColor)) { append(match.value) }
                    pop()
                    lastEnd = match.range.last + 1
                }
                append(text.substring(lastEnd))
            }
        }
        ClickableText(
            text = annotated,
            modifier = modifier,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = fontSize,
                color = color,
                lineHeight = lineHeight
            ),
            maxLines = maxLines,
            onClick = { offset ->
                annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { range ->
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(range.item)
                        )
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context,
                            "Cannot open link",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    @Composable
    fun TopBarIconBtn(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF13131E), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }

    @Composable
    fun FilterChipPill(
        label: String,
        selected: Boolean,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        iconTint: Color = TextMuted,
        drawable: android.graphics.drawable.Drawable? = null,
        onClick: () -> Unit
    ) {
        val bgColor by animateColorAsState(
            targetValue = if (selected) NotiBlue.copy(alpha = 0.16f) else Color(0xFF111120),
            animationSpec = tween(220), label = "chipBg"
        )
        val textColor by animateColorAsState(
            targetValue = if (selected) NotiBlue else TextMuted,
            animationSpec = tween(220), label = "chipTxt"
        )
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (selected) NotiBlue.copy(alpha = 0.45f) else Color(0xFF1C1C2C)
            ),
            modifier = Modifier
                .height(30.dp)
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (drawable != null) {
                    androidx.compose.foundation.Image(
                        painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(
                            drawable
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (icon != null) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(12.dp))
                }
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }

    private fun isSameDay(a: java.util.Date, b: java.util.Date): Boolean {
        val ca = java.util.Calendar.getInstance().apply { time = a }
        val cb = java.util.Calendar.getInstance().apply { time = b }
        return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR) &&
                ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun yesterday(today: java.util.Date): java.util.Date {
        val c = java.util.Calendar.getInstance()
        c.time = today
        c.add(java.util.Calendar.DAY_OF_YEAR, -1)
        return c.time
    }

    @Composable
    fun ShimmerNotificationCard() {
        Surface(
            color = Color(0xFF0E0E12),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16161C)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
    }

    fun Modifier.shimmerEffect(): Modifier = composed {
        var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
        val transition = rememberInfiniteTransition(label = "shimmer")
        val startOffsetX by transition.animateFloat(
            initialValue = -2 * size.width.toFloat(),
            targetValue = 2 * size.width.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1200)
            ),
            label = "shimmerOffset"
        )

        background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1A1A24),
                    Color(0xFF262636),
                    Color(0xFF1A1A24),
                ),
                start = androidx.compose.ui.geometry.Offset(startOffsetX, 0f),
                end = androidx.compose.ui.geometry.Offset(
                    startOffsetX + size.width.toFloat(),
                    size.height.toFloat()
                )
            )
        ).onGloballyPositioned {
            size = it.size
        }
    }

