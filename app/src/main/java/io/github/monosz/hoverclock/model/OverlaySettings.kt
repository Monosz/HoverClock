package io.github.monosz.hoverclock.model

data class OverlaySettings(
    val runningBackgroundColor: Long = 0xFF1C1B1F,
    val pausedBackgroundColor: Long = 0xFF5D4037,
    val textColor: Long = 0xFFFFFFFF,
    val backgroundAlpha: Float = 0.85f,
    val fontSizeSp: Float = 32f,
    val cornerRadiusDp: Float = 16f,
    val positionX: Int = -1,
    val positionY: Int = -1,
    val lockPosition: Boolean = false,
    val keepScreenOn: Boolean = false,
)
