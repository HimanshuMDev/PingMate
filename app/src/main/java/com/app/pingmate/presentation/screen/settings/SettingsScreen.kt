package com.app.pingmate.presentation.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.util.Log
import com.app.pingmate.utils.OfflineSummarizationEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(OfflineSummarizationEngine.PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString(OfflineSummarizationEngine.KEY_GEMINI_API, "") ?: "") }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141518), titleContentColor = Color.White)
            )
        },
        containerColor = Color(0xFF141518)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                "Gemini API key (optional)",
                color = Color(0xFFE4E6EB),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add your free API key from Google AI Studio to get intelligent voice summaries that understand any way you ask.",
                color = Color(0xFFB0B3B8),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste API key", color = Color(0xFF5A5D66)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF4A84F6),
                    focusedBorderColor = Color(0xFF4A84F6),
                    unfocusedBorderColor = Color(0xFF2C2D31)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val trimmed = apiKey.trim()
                    val ok = prefs.edit().putString(OfflineSummarizationEngine.KEY_GEMINI_API, trimmed).commit()
                    Log.d("PingMateAI", "Settings: API key saved with commit()=$ok, keyLength=${trimmed.length}")
                    saved = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A84F6))
            ) {
                Text("Save")
            }
            if (saved) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Saved. Key length: ${apiKey.trim().length}. Voice AI will use it now.", color = Color(0xFF6B9DFE), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
