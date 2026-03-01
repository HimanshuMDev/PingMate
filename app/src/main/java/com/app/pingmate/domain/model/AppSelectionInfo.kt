package com.app.pingmate.domain.model

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color

data class AppSelectionInfo(
    val packageName: String,
    val appName: String,
    val description: String,
    val isEnabled: Boolean = false,
    val iconColor: Color? = null,
    val iconDrawable: Drawable? = null 
)

