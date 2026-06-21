package io.github.monosz.hoverclock.overlay

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Intercepts all touches so [ComposeView] children cannot consume them.
 * Overlay gestures (drag, tap, long-press) are handled by the parent.
 */
internal class OverlayRootLayout(
    context: Context,
    private val onOverlayTouch: (MotionEvent) -> Boolean,
) : FrameLayout(context) {
    init {
        isClickable = true
        isFocusable = true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

    override fun onTouchEvent(event: MotionEvent): Boolean = onOverlayTouch(event)
}
