package com.app.pingmate.presentation.screen.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withAnnotation
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var isVoiceAiOverlayVisible by remember { mutableStateOf(false) }
    var isVoiceAiDialogVisible by remember { mutableStateOf(false) }
    var showReminderFor by remember { mutableStateOf<NotificationEntity?>(null) }
    var showReminderConfirmation by remember { mutableStateOf(false) }
    var detailNotification by remember { mutableStateOf<NotificationEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val transcribedText by viewModel.transcription.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isListening by viewModel.isListening.collectAsState()

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(context, "Notification permission is required to receive reminders.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val recordAudioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isVoiceAiOverlayVisible = true
            viewModel.startVoiceAi()
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required for Voice AI.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Logic Effects
    LaunchedEffect(isListening) {
        if (!isListening && transcribedText.isNotBlank() && aiResponse == null && isVoiceAiOverlayVisible) {
            viewModel.processAiPrompt(transcribedText)
        }
    }

    LaunchedEffect(aiResponse) {
        if (aiResponse != null && isVoiceAiOverlayVisible) {
            isVoiceAiOverlayVisible = false
            isVoiceAiDialogVisible = true
        }
    }

    val distinctPackageNames by viewModel.distinctPackageNames.collectAsState()

    // Main UI: header with inline filters (no drawer)
    Scaffold(
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
                    // Logo
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(NotiBlue, VipPurple)),
                                RoundedCornerShape(11.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(20.dp))
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
                            placeholder = { Text("Search…", color = TextHint, fontSize = 15.sp) },
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
                        IconButton(onClick = { isSearchExpanded = false; viewModel.updateSearchQuery("") }) {
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
                            TopBarIconBtn(icon = Icons.Outlined.Search) { isSearchExpanded = true }
                            TopBarIconBtn(icon = Icons.Outlined.Settings) { onOpenSettings() }
                        }
                    }
                }

                // ── Inline filter chips: All, Favorites, Reminders, apps ─────────────────────
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
                                context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
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

                // ── Date Chip Bar (hidden when Reminders tab is selected) ────────────────────
                if (selectedPackageName != "REMINDERS") {
                    val selectedDateStartMillis by viewModel.selectedDateStartMillis.collectAsState()
                    val selectedDate = remember(selectedDateStartMillis) {
                        if (selectedDateStartMillis == null) null else java.util.Date(selectedDateStartMillis!!)
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
                            FilterChipPill(label = "All", selected = sel) { viewModel.selectDate(null) }
                        }
                        val today = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.time
                        val dates = (0..6).map {
                            java.util.Calendar.getInstance().apply { time = today; add(java.util.Calendar.DAY_OF_YEAR, -it) }.time
                        }
                        items(dates) { date ->
                            val label = when {
                                isSameDay(date, today) -> "Today"
                                isSameDay(date, yesterday(today)) -> "Yesterday"
                                else -> java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(date)
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
                            val permissionCheckResult = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                            if (permissionCheckResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                isVoiceAiOverlayVisible = true
                                viewModel.startVoiceAi()
                            } else {
                                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxSize(),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", modifier = Modifier.size(26.dp))
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
                .then(
                    if (isVoiceAiOverlayVisible && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            renderEffect = BlurEffect(100f, 100f, TileMode.Clamp)
                        }
                    } else Modifier
                )
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
                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp), // Space for FAB
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                                    .padding(horizontal = 24.dp)
                                    .padding(top = 100.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://assets10.lottiefiles.com/packages/lf20_m6cu96.json")) // Relaxing/Empty state
                                LottieAnimation(
                                    composition = composition,
                                    iterations = LottieConstants.IterateForever,
                                    modifier = Modifier.size(200.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "All caught up!",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "No new alerts to show right now.",
                                    color = TextMuted,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
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
                                    val isDismissing = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                                    val progress = (dismissState.progress).coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isDismissing) Color(0xFF2A1515) else Color.Transparent)
                                            .padding(end = 24.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (isDismissing) {
                                            val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://assets9.lottiefiles.com/packages/lf20_klid7z.json")) // Dust/Delete effect
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
                                        Text("Retry", color = NotiBlue, fontWeight = FontWeight.SemiBold)
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
                        PingMateNotificationService.resolveIntentFromActiveNotifications(notification.id, notification.notificationKey)
                        contentIntent = NotificationIntentCache.get(notification.id)
                    }
                    if (contentIntent != null) {
                        try { contentIntent.send() } catch (e: Exception) {
                            context.packageManager.getLaunchIntentForPackage(notification.packageName)?.let { context.startActivity(it) }
                        }
                    } else {
                        context.packageManager.getLaunchIntentForPackage(notification.packageName)?.let { context.startActivity(it) }
                    }
                    detailNotification = null
                },
                onCopy = {
                    val text = buildString {
                        if (notification.title.isNotBlank()) append(notification.title).append("\n\n")
                        append(notification.content)
                    }
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Notification", text))
                    android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                },
                onSave = { viewModel.toggleFavorite(notification) },
                onRemind = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
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
                onDelete = { viewModel.deleteNotification(notification); detailNotification = null },
                onDismiss = { detailNotification = null }
            )
        }
    }

    // Dialogs & Overlays (Declared at bottom for correct Z-index)
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

    val isAiProcessing by viewModel.isAiProcessing.collectAsState()
    if (isVoiceAiOverlayVisible) {
        VoiceAiFullscreenOverlay(
            transcribedText = transcribedText,
            isThinking = isAiProcessing,
            onDismiss = {
                isVoiceAiOverlayVisible = false
                viewModel.clearAiState()
            }
        )
    }

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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AccessTime, null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Reminder set", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            text = { Text("Your reminder has been successfully scheduled. You'll get a notification at the chosen time.", color = Color(0xFFB0B3B8), fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showReminderConfirmation = false }) {
                    Text("OK", color = NotiBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color(0xFF1E1F24),
            shape = RoundedCornerShape(16.dp)
        )
    }
    } // Scaffold content
}

@Composable
fun RemindersListContent(
    remindersList: List<ReminderItem>,
    onClearNotificationReminder: (NotificationEntity) -> Unit,
    onDeleteGeneralReminder: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "REMINDERS",
                style = PingMateTypography.titleSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.ExtraBold),
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
                    Icon(Icons.Outlined.AccessTime, null, modifier = Modifier.size(48.dp), tint = Color(0xFF3D3E48))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No reminders", color = Color(0xFF6B6F7A), fontSize = 15.sp)
                    Text("Set reminders via AI or from a notification card", color = Color(0xFF4A4D56), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        } else {
            items(remindersList.size) { index ->
                val item = remindersList[index]
                when (item) {
                    is ReminderItem.NotificationReminder -> {
                        val n = item.notification
                        val timeStr = n.reminderTime?.let { timeFormat.format(Date(it)) } ?: ""
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF0E0E12),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16161C))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.AccessTime, null, tint = NotiBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(n.title.ifBlank { "Notification" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD8D9DC), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(timeStr, fontSize = 12.sp, color = Color(0xFF6B6F7C))
                                }
                                IconButton(onClick = { onClearNotificationReminder(n) }) {
                                    Icon(Icons.Outlined.DeleteOutline, "Remove", tint = Color(0xFFA85A66), modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                    is ReminderItem.GeneralReminder -> {
                        val e = item.entity
                        val timeStr = timeFormat.format(Date(e.reminderTimeMillis))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF0E0E12),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16161C))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.AccessTime, null, tint = NotiBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(e.note.ifBlank { "Reminder" }, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFD8D9DC), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(timeStr, fontSize = 12.sp, color = Color(0xFF6B6F7C))
                                }
                                IconButton(onClick = { onDeleteGeneralReminder(e.id) }) {
                                    Icon(Icons.Outlined.DeleteOutline, "Remove", tint = Color(0xFFA85A66), modifier = Modifier.size(22.dp))
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
    val timeFormat = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
    val formattedTime = timeFormat.format(java.util.Date(notification.timestamp)).lowercase()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // Decode contact/user photo (e.g. WhatsApp sender avatar stored from notification largeIcon)
    val largeIconBitmap = remember(notification.largeIconBase64) {
        notification.largeIconBase64?.let { b64 ->
            try {
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?.asImageBitmap()
            } catch (e: Exception) { null }
        }
    }
    // Decode big picture (e.g. notification content image / BigPictureStyle)
    val bigPictureBitmap = remember(notification.bigPictureBase64) {
        notification.bigPictureBase64?.let { b64 ->
            try {
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?.asImageBitmap()
            } catch (e: Exception) { null }
        }
    }

    val appIcon = remember(notification.packageName) {
        try { ctx.packageManager.getApplicationIcon(notification.packageName) } catch (e: Exception) { null }
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
    val showBigPictureInCard = !compact && bigPictureBitmap != null
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
            brush = if (isFav) Brush.horizontalGradient(listOf(NotiBlue, VipPurple)) else Brush.horizontalGradient(listOf(cardBorder, cardBorder))
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
                            painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(appIcon),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Default.Notifications, null, tint = Color(0xFF5A5D68), modifier = Modifier.size(22.dp))
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
                            painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(appIcon),
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
                    Text(formattedTime, fontSize = 10.sp, color = Color(0xFF50535E), fontWeight = FontWeight.Normal)
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

                Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = Color(0xFF16161C),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222E)),
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222E)),
                        modifier = Modifier.clickable { onRemind() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.AccessTime, null, tint = Color(0xFF6B6F7C), modifier = Modifier.size(12.dp))
                            Text("Remind", fontSize = 11.sp, color = Color(0xFF6B6F7C), fontWeight = FontWeight.Medium)
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
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    val timeFormat = remember { java.text.SimpleDateFormat("h:mm a, MMM d", java.util.Locale.getDefault()) }
    val formattedTime = timeFormat.format(java.util.Date(notification.timestamp))

    val largeIconBitmap = remember(notification.largeIconBase64) {
        notification.largeIconBase64?.let { b64 ->
            try {
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) { null }
        }
    }
    val bigPictureBitmap = remember(notification.bigPictureBase64) {
        notification.bigPictureBase64?.let { b64 ->
            try {
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) { null }
        }
    }
    val appIcon = remember(notification.packageName) {
        try { ctx.packageManager.getApplicationIcon(notification.packageName) } catch (e: Exception) { null }
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
                            painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(appIcon),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Icon(Icons.Default.Notifications, null, tint = Color(0xFF5A5D68), modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(appLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B6F7C))
                    Text(formattedTime, fontSize = 11.sp, color = Color(0xFF50535E))
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color(0xFF6B6F7C), modifier = Modifier.size(22.dp))
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

        if (bigPictureBitmap != null) {
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
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp), tint = Color(0xFF9A9DA8))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFD8D9DC))
                }
            }
            Surface(
                onClick = onCopy,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF16161C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22222E))
            ) {
                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(20.dp), tint = Color(0xFF6B6F7C))
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
                        tint = if (notification.isFavorite) Color(0xFF9A8F6E) else Color(0xFF6B6F7C)
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
                    Icon(Icons.Outlined.AccessTime, null, modifier = Modifier.size(20.dp), tint = Color(0xFF6B6F7C))
                }
            }
            Surface(
                onClick = onDelete,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF16161C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3035))
            ) {
                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(20.dp), tint = Color(0xFFA85A66))
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
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(range.item))
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Cannot open link", android.widget.Toast.LENGTH_SHORT).show()
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
                    painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(drawable),
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

@Composable
fun InlineDateStrip(
    selectedDate: java.util.Date?,
    onDateSelected: (java.util.Date?) -> Unit
) {
    val today = remember {
        val c = java.util.Calendar.getInstance(); c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0); c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0); c.time
    }

    // Build last 14 days
    val dates = remember {
        (0..13).map { daysAgo ->
            val c = java.util.Calendar.getInstance()
            c.time = today
            c.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
            c.time
        }
    }

    val dayFormat = remember { java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()) }
    val numFormat = remember { java.text.SimpleDateFormat("d", java.util.Locale.getDefault()) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        item {
            val sel = selectedDate == null
            DateChip(
                dayLabel = "ALL",
                numLabel = "•",
                selected = sel,
                onClick = { onDateSelected(null) }
            )
        }
        items(dates) { date ->
            val dayLabel = when {
                isSameDay(date, today) -> "TODAY"
                isSameDay(date, yesterday(today)) -> "YEST."
                else -> dayFormat.format(date).uppercase()
            }
            val numLabel = numFormat.format(date)
            val sel = selectedDate != null && isSameDay(date, selectedDate)
            DateChip(dayLabel = dayLabel, numLabel = numLabel, selected = sel) {
                onDateSelected(if (sel) null else date)
            }
        }
    }
}

@Composable
private fun DateChip(dayLabel: String, numLabel: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) NotiBlue else Color(0xFF111120),
        animationSpec = tween(200), label = "dateBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy), label = "dateScale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (selected) NotiBlue else Color(0xFF1C1C2C),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else TextMuted,
                letterSpacing = 0.5.sp
            )
            Text(
                text = numLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) Color.White else TextSecondary
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
            end = androidx.compose.ui.geometry.Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Immersive Voice AI Fullscreen Overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VoiceAiFullscreenOverlay(
    transcribedText: String,
    isThinking: Boolean,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aiPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Full-screen blur: dark overlay so background is clearly blurred
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onDismiss() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Smaller Lottie for a cleaner look
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.9f)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF6B7FE8).copy(alpha = glowAlpha * 0.5f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
                val composition by com.airbnb.lottie.compose.rememberLottieComposition(
                    com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(com.app.pingmate.R.raw.ai_animation)
                )
                val progress by com.airbnb.lottie.compose.animateLottieCompositionAsState(
                    composition = composition,
                    iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever
                )
                com.airbnb.lottie.compose.LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Assistant",
                color = Color(0xFFE8EAEF),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Spoken text / status card — clearer typography and "Preparing…" instead of "Analyzing…"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1B22).copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2B35))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = if (isThinking) "preparing" else transcribedText,
                        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
                        label = "overlayTranscription"
                    ) { text ->
                        when {
                            text == "preparing" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color(0xFF7A8AE8),
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Preparing…",
                                        color = Color(0xFFB8BCC8),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            else -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Outlined.Mic,
                                        contentDescription = null,
                                        tint = Color(0xFF7A7E8E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (text.isBlank()) "I'm listening…" else text,
                                        color = if (text.isBlank()) Color(0xFF9A9EAC) else Color(0xFFE8EAEF),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .size(40.dp)
                .background(Color(0xFF1E1F28).copy(alpha = 0.9f), CircleShape)
        ) {
            Icon(Icons.Default.Close, null, tint = Color(0xFF8A8E9A), modifier = Modifier.size(20.dp))
        }
    }
}
