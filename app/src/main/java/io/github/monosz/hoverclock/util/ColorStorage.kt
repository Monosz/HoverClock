package io.github.monosz.hoverclock.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun Color.toStoredLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

fun Long.toComposeColor(): Color = Color(this)
