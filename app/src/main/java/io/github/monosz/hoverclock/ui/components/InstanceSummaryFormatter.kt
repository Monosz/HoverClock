package io.github.monosz.hoverclock.ui.components

enum class FontSizeLabel {
    Small,
    Medium,
    Large,
}

object InstanceSummaryFormatter {
    fun fontSizeLabel(fontSizeSp: Float): FontSizeLabel =
        when {
            fontSizeSp <= 24f -> FontSizeLabel.Small
            fontSizeSp <= 40f -> FontSizeLabel.Medium
            else -> FontSizeLabel.Large
        }

    fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun buildSummary(
        sizeLabel: String,
        secondToken: String,
    ): String = "$sizeLabel · $secondToken"
}
