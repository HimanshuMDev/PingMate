package com.app.pingmate.presentation.screen.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.pingmate.data.local.entity.NotificationEntity
import com.app.pingmate.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetReminderDialog(
    notification: NotificationEntity,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Date: default today
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var selectedDateMillis by remember { mutableStateOf(todayStart) }

    // Time Selection State
    val calendar = remember { Calendar.getInstance().apply { add(Calendar.MINUTE, 15) } }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    var showDateTimePicker by remember { mutableStateOf(false) }
    var isSelectingTime by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF0F0F16),
            border = BorderStroke(
                1.dp, 
                Brush.linearGradient(listOf(NotiBlue.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f)))
            ),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
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
                            Icon(Icons.Outlined.AccessTime, null, tint = NotiBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Schedule Alert",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Notification Context Bubble
                Surface(
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).background(Color(0xFF1A1A22), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ChatBubbleOutline, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = notification.title.ifBlank { "System Notification" },
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = notification.content,
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Combined Date & Time (single row opens one picker dialog)
                Text(
                    "DATE & TIME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NotiBlue,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                val hour = if (timePickerState.hour == 0 || timePickerState.hour == 12) 12 else timePickerState.hour % 12
                val minute = timePickerState.minute.toString().padStart(2, '0')
                val amPm = if (timePickerState.hour < 12) "AM" else "PM"
                val dateStr = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
                val dateTimeLine = "$dateStr   $hour:$minute $amPm"
                Surface(
                    onClick = { 
                        isSelectingTime = false
                        showDateTimePicker = true 
                    },
                    color = Color(0xFF1E1E2C),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C2C3E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = NotiBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = dateTimeLine,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Description Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Add a quick note...", color = TextMuted, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NotiBlue.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Dismiss", color = TextMuted, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            val resultCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                            resultCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            resultCal.set(Calendar.MINUTE, timePickerState.minute)
                            resultCal.set(Calendar.SECOND, 0)
                            resultCal.set(Calendar.MILLISECOND, 0)
                            if (resultCal.timeInMillis < System.currentTimeMillis()) {
                                resultCal.timeInMillis = System.currentTimeMillis()
                                resultCal.add(Calendar.MINUTE, 1)
                            }
                            onSave(resultCal.timeInMillis, note)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NotiBlue),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.2f).height(50.dp)
                    ) {
                        Text("Schedule", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDateTimePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        val scrollState = rememberScrollState()
        Dialog(
            onDismissRequest = { showDateTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF161622),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight() // Wrap height to content
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp), // Removed vertical scroll since it fits now
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isSelectingTime) {
                        // STEP 1: DATE PICKER
                        Text("Select Date", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        DatePicker(state = datePickerState)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showDateTimePicker = false }) { Text("Cancel", color = TextMuted) }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { 
                                datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                                isSelectingTime = true // Switch to time picker
                            }) { Text("Next", color = NotiBlue, fontWeight = FontWeight.Bold) }
                        }
                    } else {
                        // STEP 2: TIME PICKER
                        Text("Select Time", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = Color(0xFF1E1E2C),
                                clockDialSelectedContentColor = Color.White,
                                clockDialUnselectedContentColor = Color.White.copy(alpha = 0.5f),
                                selectorColor = NotiBlue,
                                periodSelectorSelectedContainerColor = NotiBlue,
                                periodSelectorUnselectedContainerColor = Color(0xFF1E1E2C),
                                periodSelectorSelectedContentColor = Color.White,
                                periodSelectorUnselectedContentColor = Color.White.copy(alpha = 0.5f),
                                timeSelectorSelectedContainerColor = NotiBlue.copy(alpha = 0.2f),
                                timeSelectorUnselectedContainerColor = Color(0xFF1E1E2C),
                                timeSelectorSelectedContentColor = NotiBlue,
                                timeSelectorUnselectedContentColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { isSelectingTime = false }) { Text("Back", color = TextMuted) }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                showDateTimePicker = false
                            }) { Text("OK", color = NotiBlue, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    containerColor: Color = Color(0xFF0F0F16),
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = containerColor,
            modifier = Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min).padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetGeneralReminderDialog(
    initialTimeMillis: Long,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit
) {
    var note by remember { mutableStateOf(initialNote) }
    val context = LocalContext.current

    var selectedDateMillis by remember { mutableStateOf(initialTimeMillis) }

    val calendar = remember(initialTimeMillis) { Calendar.getInstance().apply { timeInMillis = initialTimeMillis } }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    var showDateTimePicker by remember { mutableStateOf(false) }
    var isSelectingTime by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF0F0F16),
            border = BorderStroke(
                1.dp, 
                Brush.linearGradient(listOf(NotiBlue.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f)))
            ),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
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
                            Icon(Icons.Outlined.AccessTime, null, tint = NotiBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Schedule Reminder",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Combined Date & Time
                Text(
                    "DATE & TIME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NotiBlue,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                val hour = if (timePickerState.hour == 0 || timePickerState.hour == 12) 12 else timePickerState.hour % 12
                val minute = timePickerState.minute.toString().padStart(2, '0')
                val amPm = if (timePickerState.hour < 12) "AM" else "PM"
                val dateStr = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
                val dateTimeLine = "$dateStr   $hour:$minute $amPm"
                Surface(
                    onClick = { 
                        isSelectingTime = false
                        showDateTimePicker = true 
                    },
                    color = Color(0xFF1E1E2C),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C2C3E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = NotiBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = dateTimeLine,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Description Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("What to remind you about...", color = TextMuted, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NotiBlue.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Dismiss", color = TextMuted, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            val resultCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                            resultCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            resultCal.set(Calendar.MINUTE, timePickerState.minute)
                            resultCal.set(Calendar.SECOND, 0)
                            resultCal.set(Calendar.MILLISECOND, 0)
                            if (resultCal.timeInMillis < System.currentTimeMillis()) {
                                resultCal.timeInMillis = System.currentTimeMillis()
                                resultCal.add(Calendar.MINUTE, 1)
                            }
                            onSave(resultCal.timeInMillis, note)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NotiBlue),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.2f).height(50.dp)
                    ) {
                        Text("Schedule", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDateTimePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        val scrollState = rememberScrollState()
        Dialog(
            onDismissRequest = { showDateTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF161622),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isSelectingTime) {
                        // STEP 1: DATE PICKER
                        Text("Select Date", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        DatePicker(state = datePickerState)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showDateTimePicker = false }) { Text("Cancel", color = TextMuted) }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { 
                                datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                                isSelectingTime = true // Switch to time picker
                            }) { Text("Next", color = NotiBlue, fontWeight = FontWeight.Bold) }
                        }
                    } else {
                        // STEP 2: TIME PICKER
                        Text("Select Time", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = Color(0xFF1E1E2C),
                                clockDialSelectedContentColor = Color.White,
                                clockDialUnselectedContentColor = Color.White.copy(alpha = 0.5f),
                                selectorColor = NotiBlue,
                                periodSelectorSelectedContainerColor = NotiBlue,
                                periodSelectorUnselectedContainerColor = Color(0xFF1E1E2C),
                                periodSelectorSelectedContentColor = Color.White,
                                periodSelectorUnselectedContentColor = Color.White.copy(alpha = 0.5f),
                                timeSelectorSelectedContainerColor = NotiBlue.copy(alpha = 0.2f),
                                timeSelectorUnselectedContainerColor = Color(0xFF1E1E2C),
                                timeSelectorSelectedContentColor = NotiBlue,
                                timeSelectorUnselectedContentColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { isSelectingTime = false }) { Text("Back", color = TextMuted) }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                showDateTimePicker = false
                            }) { Text("OK", color = NotiBlue, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}
