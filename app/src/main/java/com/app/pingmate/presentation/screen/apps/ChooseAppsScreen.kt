package com.app.pingmate.presentation.screen.apps

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pingmate.domain.model.AppSelectionInfo
import com.app.pingmate.presentation.screen.onboarding.GradientButton
import com.app.pingmate.ui.theme.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.delay

@Composable
fun ChooseAppsScreen(
    onNavigateNext: () -> Unit,
    viewModel: ChooseAppsViewModel = viewModel()
) {
    val appsList by viewModel.appsList.collectAsState()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Header fade/slide entrance
    var headerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        headerVisible = true
    }

    val filteredApps = remember(appsList, searchQuery) {
        if (searchQuery.isBlank()) appsList
        else appsList.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }
    val selectedCount = appsList.count { it.isEnabled }

    // Use Scaffold so bottom bar is ALWAYS FIXED above keyboard
    Scaffold(
        containerColor = Color(0xFF0A0A10),
        bottomBar = {
            // Fixed bottom bar — always visible regardless of scroll position
            Surface(
                color = Color(0xFF0A0A10),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Animated save button label
                    GradientButton(
                        text = when {
                            selectedCount == 0 -> "Select at least one app"
                            selectedCount == 1 -> "Continue with 1 app →"
                            else -> "Continue with $selectedCount apps →"
                        },
                        onClick = {
                            viewModel.saveAndContinue()
                            onNavigateNext()
                        },
                        enabled = isSaveEnabled
                    )
                    Text(
                        text = "•  PRIVATE  •  OFFLINE AI  •  ZERO CLOUD  •",
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextHint,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A0A10), Color(0xFF0E0E18))
                    )
                )
        ) {
            // Ambient glow top
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-50).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(NotiBlue.copy(alpha = 0.08f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp, top = 0.dp, bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ---- Sticky Header ----
                item {
                    AnimatedVisibility(
                        visible = headerVisible,
                        enter = slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(400))
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                start = 4.dp, end = 4.dp, top = 56.dp, bottom = 8.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Choose Your Apps",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Text(
                                        text = "Select apps to track & summarize with AI",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        lineHeight = 19.sp
                                    )
                                }
                                // Live selected count badge
                                AnimatedContent(
                                    targetState = selectedCount,
                                    transitionSpec = {
                                        (slideInVertically { it } + fadeIn()) togetherWith
                                        (slideOutVertically { -it } + fadeOut())
                                    },
                                    label = "badge"
                                ) { count ->
                                    if (count > 0) {
                                        Surface(
                                            color = NotiBlue.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text(
                                                text = "$count ✓",
                                                color = NotiBlue,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(
                                                    horizontal = 12.dp, vertical = 6.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Search Field
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search apps...",
                                        color = TextHint,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF12121E),
                                    unfocusedContainerColor = Color(0xFF12121E),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = NotiBlue
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .border(1.dp, Color(0xFF1F1F38), RoundedCornerShape(16.dp))
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // ---- Loading shimmer state ----
                if (isLoading) {
                    items(8) { index ->
                        ShimmerAppCard(index = index)
                    }
                } else if (filteredApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🔍", fontSize = 36.sp)
                                Text(
                                    "No apps found for \"$searchQuery\"",
                                    color = TextMuted,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        filteredApps,
                        key = { _, app -> app.packageName }
                    ) { index, appInfo ->
                        AppSelectionCard(
                            appInfo = appInfo,
                            index = index,
                            onToggle = { viewModel.toggleAppSelection(appInfo.packageName) }
                        )
                    }
                }
            }
        }
    }
}

// ---- Shimmer loading card ----
@Composable
private fun ShimmerAppCard(index: Int) {
    val transition = rememberInfiniteTransition(label = "shimmer$index")
    val shimmerTranslate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing, delayMillis = index * 60),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX$index"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A1A28),
            Color(0xFF252535),
            Color(0xFF1A1A28)
        ),
        start = Offset(shimmerTranslate - 300f, 0f),
        end = Offset(shimmerTranslate, 0f)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(shimmerBrush)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(0xFF252535))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF252535))
            )
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF252535))
            )
        }
    }
}

// ---- App selection card ----
@Composable
fun AppSelectionCard(
    appInfo: AppSelectionInfo,
    index: Int = 0,
    onToggle: () -> Unit
) {
    val isSelected = appInfo.isEnabled

    // Smooth scale on select/deselect
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardScale"
    )

    val cardBgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF10182A) else Color(0xFF0E0E1A),
        animationSpec = tween(250),
        label = "cardBg"
    )

    Surface(
        color = cardBgColor,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                brush = if (isSelected)
                    Brush.horizontalGradient(listOf(NotiBlue, VipPurple))
                else
                    Brush.horizontalGradient(listOf(Color(0xFF1F1F38), Color(0xFF1F1F38))),
                shape = RoundedCornerShape(18.dp)
            ),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFF12121E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (appInfo.iconDrawable != null) {
                    androidx.compose.foundation.Image(
                        painter = rememberDrawablePainter(appInfo.iconDrawable),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                (appInfo.iconColor ?: NotiBlue).copy(alpha = 0.25f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(appInfo.iconColor ?: NotiBlue, CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else TextSecondary,
                    animationSpec = tween(200),
                    label = "textColor"
                )
                Text(
                    text = appInfo.appName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    text = appInfo.description,
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Animated check icon
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                    (scaleOut(tween(150)) + fadeOut(tween(150)))
                },
                label = "checkIcon"
            ) { selected ->
                Icon(
                    imageVector = if (selected) Icons.Filled.CheckCircle
                                  else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) NotiBlue else Color(0xFF2A2D45),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
