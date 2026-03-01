package com.app.pingmate.presentation.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.*

import com.app.pingmate.data.local.entity.NotificationEntity
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetReminderDialog(
    notification: NotificationEntity,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    // Initial time logic
    val cal = Calendar.getInstance()
    // Add 15 mins default
    cal.add(Calendar.MINUTE, 15)
    
    var selectedHour by remember { mutableStateOf(if (cal.get(Calendar.HOUR) == 0) 12 else cal.get(Calendar.HOUR)) }
    var selectedMinute by remember { mutableStateOf(cal.get(Calendar.MINUTE)) }
    var isAm by remember { mutableStateOf(cal.get(Calendar.AM_PM) == Calendar.AM) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF141518) // Dark background
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF1E1F24), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = "Clock",
                        tint = Color(0xFF6B9DFE),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Set Reminder",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "When should PingMate alert you?",
                    color = Color(0xFFB0B3B8),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Contextual Notification Preview
                Surface(
                    color = Color(0xFF1E1F24),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2D31)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = notification.title.ifBlank { "System" },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notification.content,
                            color = Color(0xFFB0B3B8),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Time Picker Wheel Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF1E1F24), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Highlight bar in the middle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color(0xFF2C3246).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour Picker (1..12)
                        WheelPicker(
                            items = (1..12).map { it.toString().padStart(2, '0') },
                            initialIndex = selectedHour - 1,
                            onItemSelected = { selectedHour = it + 1 }
                        )

                        Text(":", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        // Minute Picker (0..59)
                        WheelPicker(
                            items = (0..59).map { it.toString().padStart(2, '0') },
                            initialIndex = selectedMinute,
                            onItemSelected = { selectedMinute = it }
                        )

                        // AM/PM Picker
                        WheelPicker(
                            items = listOf("AM", "PM"),
                            initialIndex = if (isAm) 0 else 1,
                            onItemSelected = { isAm = (it == 0) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reminder Description Note
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Text(
                        text = "REMINDER DESCRIPTION (OPTIONAL)",
                        color = Color(0xFF5A5D66),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("What's this about?", color = Color(0xFFB0B3B8), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Note", tint = Color(0xFFB0B3B8), modifier = Modifier.size(18.dp))
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1F24),
                            unfocusedContainerColor = Color(0xFF1E1F24),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF6B9DFE)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color(0xFFB0B3B8), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            val resultCal = Calendar.getInstance()
                            resultCal.set(Calendar.HOUR, if (selectedHour == 12) 0 else selectedHour)
                            resultCal.set(Calendar.MINUTE, selectedMinute)
                            resultCal.set(Calendar.AM_PM, if (isAm) Calendar.AM else Calendar.PM)
                            resultCal.set(Calendar.SECOND, 0)
                            
                            // If selected time is before now, assume next day
                            if (resultCal.timeInMillis < System.currentTimeMillis()) {
                                resultCal.add(Calendar.DAY_OF_YEAR, 1)
                            }
                            
                            onSave(resultCal.timeInMillis, note)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9DFE)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState(initialIndex)
    val itemHeight = 44.dp
    
    // Add empty items for padding
    val paddedItems = listOf("", "") + items + listOf("", "")

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                // Account for the padding
                val midIndex = index
                if (midIndex in items.indices) {
                    onItemSelected(midIndex)
                }
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        modifier = Modifier
            .height(itemHeight * 3)
            .width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(paddedItems.size) { index ->
            val text = paddedItems[index]
            val isSelected = index == listState.firstVisibleItemIndex + 2
            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = if (isSelected) Color.White else Color(0xFF5A5D66),
                    fontSize = if (isSelected) 24.sp else 18.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                )
            }
        }
    }
}
