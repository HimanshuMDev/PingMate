package com.app.pingmate.presentation.screen.onboarding

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.app.pingmate.service.PingMateNotificationService
import com.app.pingmate.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PermissionScreen(
    onNavigateNext: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }

    // Periodically check if the user granted permission while they were in the settings screen
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            while (true) {
                delay(1000)
                if (isNotificationServiceEnabled(context)) {
                    hasPermission = true
                    onNavigateNext()
                    break
                }
            }
        } else {
            onNavigateNext()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = isNotificationServiceEnabled(context)
        if (hasPermission) {
            onNavigateNext()
        }
    }

    Scaffold(
        containerColor = CharcoalBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        launcher.launch(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NotiBlue,
                        contentColor = TextPrimary
                    )
                ) {
                    Text(
                        text = "Grant Access",
                        style = PingMateTypography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Text(
                text = "🔔",
                fontSize = 80.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Notification Access",
                style = PingMateTypography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "PingMate needs permission to read your incoming notifications so it can intercept them and funnel them into your smart feed.",
                style = PingMateTypography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "How to enable:",
                        style = PingMateTypography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Tap 'Grant Access' below.\n2. Find PingMate in the list.\n3. Toggle the switch to ON.\n4. Tap 'Allow' to confirm.",
                        style = PingMateTypography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!flat.isNullOrEmpty()) {
        val names = flat.split(":")
        for (name in names) {
            val componentName = ComponentName.unflattenFromString(name)
            if (componentName != null && componentName.packageName == pkgName) {
                return true
            }
        }
    }
    return false
}
