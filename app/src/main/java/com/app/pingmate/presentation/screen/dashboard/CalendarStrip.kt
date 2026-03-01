package com.app.pingmate.presentation.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarStrip(
    selectedDate: Date?,
    onDateSelected: (Date?) -> Unit
) {
    val dates = remember {
        val list = mutableListOf<Pair<String, Date?>>()
        
        val calendar = Calendar.getInstance()
        // Today
        list.add("Today" to calendar.time)
        
        // Yesterday
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        list.add("Yesterday" to calendar.time)
        
        // Next 14 days in the past
        val format = SimpleDateFormat("MMM dd", Locale.getDefault())
        for (i in 0 until 14) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            list.add(format.format(calendar.time) to calendar.time)
        }
        list
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dates) { (label, date) ->
            val isSelected = if (selectedDate == null && date == null) {
                true 
            } else if (selectedDate != null && date != null) {
                val cal1 = Calendar.getInstance().apply { time = selectedDate }
                val cal2 = Calendar.getInstance().apply { time = date }
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
            } else {
                false
            }

            Box(
                modifier = Modifier
                    .background(
                        color = if (isSelected) Color(0xFF2C3246) else Color(0xFF1E1F24),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onDateSelected(date) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color(0xFFB0B3B8),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
        
        item {
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState()
            
            Box(
                modifier = Modifier
                    .background(color = Color(0xFF1E1F24), shape = RoundedCornerShape(20.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick Date",
                    tint = Color(0xFFB0B3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val selMillis = datePickerState.selectedDateMillis
                            if (selMillis != null) {
                                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                cal.timeInMillis = selMillis
                                val localCal = Calendar.getInstance()
                                localCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                                localCal.set(Calendar.MILLISECOND, 0)
                                onDateSelected(localCal.time)
                            }
                            showDatePicker = false
                        }) { Text("OK", color = Color(0xFF6B9DFE)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("CANCEL", color = Color(0xFFB0B3B8)) }
                    },
                    colors = DatePickerDefaults.colors(
                        containerColor = Color(0xFF1E1F24)
                    )
                ) {
                    DatePicker(
                        state = datePickerState,
                        colors = DatePickerDefaults.colors(
                            titleContentColor = Color.White,
                            headlineContentColor = Color.White,
                            weekdayContentColor = Color(0xFFB0B3B8),
                            navigationContentColor = Color.White,
                            yearContentColor = Color.White,
                            currentYearContentColor = Color(0xFF6B9DFE),
                            selectedYearContentColor = Color.White,
                            selectedYearContainerColor = Color(0xFF6B9DFE),
                            dayContentColor = Color.White,
                            selectedDayContentColor = Color.White,
                            selectedDayContainerColor = Color(0xFF6B9DFE),
                            todayContentColor = Color(0xFF6B9DFE),
                            todayDateBorderColor = Color(0xFF6B9DFE)
                        )
                    )
                }
            }
        }
    }
}
