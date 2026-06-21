package io.github.monosz.hoverclock.service

object OverlaySpawnOffset {
    private const val OFFSET_DP = 48
    private const val BASE_Y_DP = 200

    fun offsetPx(
        sessionIndex: Int,
        density: Float,
    ): Pair<Int, Int> {
        val offsetPx = (sessionIndex * OFFSET_DP * density).toInt()
        val baseYPx = (BASE_Y_DP * density).toInt()
        return offsetPx to (baseYPx + offsetPx)
    }
}

object OverlayNotificationText {
    fun summaryLine(
        names: List<String>,
        maxLength: Int = 80,
    ): String {
        if (names.isEmpty()) return ""
        val joined = names.joinToString(", ")
        return if (joined.length <= maxLength) joined else joined.take(maxLength - 1) + "…"
    }
}
