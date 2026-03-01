package com.app.pingmate.presentation.screen.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pingmate.domain.model.AppSelectionInfo
import com.app.pingmate.ui.theme.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle

@Composable
fun ChooseAppsScreen(
    onNavigateNext: () -> Unit,
    viewModel: ChooseAppsViewModel = viewModel()
) {
    val appsList by viewModel.appsList.collectAsState()
    val isSaveEnabled by viewModel.isSaveEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = CharcoalBackground,
        bottomBar = {
            ChooseAppsBottomBar(
                isSaveEnabled = isSaveEnabled,
                onSaveClicked = {
                    viewModel.saveAndContinue()
                    onNavigateNext()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            // Header Text
            Text(
                text = "Choose Your Apps",
                style = PingMateTypography.headlineLarge.copy(fontSize = 32.sp),
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Select the apps PingMate will intelligently track and summarize using offline AI.",
                style = PingMateTypography.bodyLarge,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NotiBlue)
                }
            } else {
                // List of Apps
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(appsList, key = { it.packageName }) { appInfo ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                                animationSpec = tween(500),
                                initialOffsetY = { it / 2 }
                            )
                        ) {
                            AppSelectionCard(
                                appInfo = appInfo,
                                onToggle = { viewModel.toggleAppSelection(appInfo.packageName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppSelectionCard(
    appInfo: AppSelectionInfo,
    onToggle: () -> Unit
) {
    Surface(
        color = if (appInfo.isEnabled) SurfaceVariant else SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (appInfo.isEnabled) 2.dp else 1.dp,
                color = if (appInfo.isEnabled) NotiBlue else BorderSubtle,
                shape = RoundedCornerShape(20.dp)
            ),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                if (appInfo.iconDrawable != null) {
                    androidx.compose.foundation.Image(
                        painter = rememberDrawablePainter(appInfo.iconDrawable),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                } else {
                    // Fallback Glow effect
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background((appInfo.iconColor ?: NotiBlue).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(appInfo.iconColor ?: NotiBlue))
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Texts
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.appName,
                    style = PingMateTypography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = appInfo.description,
                    style = PingMateTypography.bodyMedium,
                    color = TextMuted
                )
            }
            
            // Selection Indicator
            Icon(
                imageVector = if (appInfo.isEnabled) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (appInfo.isEnabled) "Selected" else "Unselected",
                tint = if (appInfo.isEnabled) NotiBlue else TextMuted,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun ChooseAppsBottomBar(
    isSaveEnabled: Boolean,
    onSaveClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "•  PRIVACY FIRST • OFFLINE PROCESSING  •",
            style = PingMateTypography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSaveClicked,
            enabled = isSaveEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NotiBlue,
                contentColor = TextPrimary,
                disabledContainerColor = SurfaceVariant,
                disabledContentColor = TextSecondary
            )
        ) {
            Text(
                text = "Save & Continue →",
                style = PingMateTypography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
