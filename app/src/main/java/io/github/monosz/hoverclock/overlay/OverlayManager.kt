package io.github.monosz.hoverclock.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.monosz.hoverclock.data.SettingsRepository
import io.github.monosz.hoverclock.engine.TimeProvider
import io.github.monosz.hoverclock.model.OverlaySettings
import io.github.monosz.hoverclock.model.TimeMode
import io.github.monosz.hoverclock.model.TimeState
import io.github.monosz.hoverclock.ui.overlay.OverlayContent
import io.github.monosz.hoverclock.ui.theme.HoverClockTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class OverlayManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val instanceId: String,
    private val scope: CoroutineScope,
    private val timeMode: TimeMode,
    private val defaultSpawnOffsetPx: Pair<Int, Int> = 0 to 200,
    private val onSingleTap: () -> Unit = {},
    private val onDoubleTap: () -> Unit = {},
    private val onLongPress: () -> Unit = {},
) : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: OverlayRootLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var timeState by mutableStateOf(
        TimeState(
            mode = TimeMode.Clock,
            formattedText = "00:00:00",
            isRunning = false,
        ),
    )
    private var overlaySettings by mutableStateOf(OverlaySettings())
    private var lockPosition = false
    private var keepScreenOn = false
    private var isDragging = false

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    suspend fun show(timeProvider: TimeProvider) {
        if (rootView != null) return

        val initialSettings = settingsRepository.overlaySettings(instanceId).first()
        overlaySettings = initialSettings
        lockPosition = initialSettings.lockPosition
        keepScreenOn = initialSettings.keepScreenOn

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val params = createLayoutParams()
        if (initialSettings.positionX >= 0 && initialSettings.positionY >= 0) {
            params.x = initialSettings.positionX
            params.y = initialSettings.positionY
        } else {
            params.x = defaultSpawnOffsetPx.first
            params.y = defaultSpawnOffsetPx.second
        }

        lateinit var gestureDetector: GestureDetectorCompat
        lateinit var root: OverlayRootLayout

        var dragStartRawX = 0f
        var dragStartRawY = 0f
        var paramStartX = 0
        var paramStartY = 0
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

        gestureDetector =
            GestureDetectorCompat(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDown(e: MotionEvent): Boolean = true

                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        if (timeMode != TimeMode.Clock) {
                            onSingleTap()
                        }
                        return true
                    }

                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        if (timeMode != TimeMode.Clock) {
                            onDoubleTap()
                        }
                        return true
                    }

                    override fun onLongPress(e: MotionEvent) {
                        if (isDragging) return
                        onLongPress()
                    }
                },
            )

        root =
            OverlayRootLayout(context) { event ->
                gestureDetector.onTouchEvent(event)
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartRawX = event.rawX
                        dragStartRawY = event.rawY
                        paramStartX = params.x
                        paramStartY = params.y
                        isDragging = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!lockPosition) {
                            val dx = event.rawX - dragStartRawX
                            val dy = event.rawY - dragStartRawY
                            if (!isDragging && (dx * dx + dy * dy) >= touchSlop * touchSlop) {
                                isDragging = true
                            }
                            if (isDragging) {
                                params.x = clampX(paramStartX + dx.roundToInt())
                                params.y = clampY(paramStartY + dy.roundToInt())
                                windowManager.updateViewLayout(root, params)
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging && !lockPosition) {
                            scope.launch {
                                settingsRepository.setOverlayPosition(instanceId, params.x, params.y)
                            }
                        }
                        isDragging = false
                    }
                }
                true
            }.apply {
                setViewTreeLifecycleOwner(this@OverlayManager)
                setViewTreeSavedStateRegistryOwner(this@OverlayManager)
            }

        val composeView =
            ComposeView(context).apply {
                isClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                setContent {
                    HoverClockTheme {
                        OverlayContent(
                            timeState = timeState,
                            settings = overlaySettings,
                        )
                    }
                }
            }
        root.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        windowManager.addView(root, params)
        rootView = root
        layoutParams = params
        updateKeepScreenOn()

        scope.launch {
            combine(
                timeProvider.state,
                settingsRepository.overlaySettings(instanceId),
            ) { state, settings ->
                state to settings
            }.collect { (state, settings) ->
                timeState = state
                overlaySettings = settings
                lockPosition = settings.lockPosition
                if (keepScreenOn != settings.keepScreenOn) {
                    keepScreenOn = settings.keepScreenOn
                    updateKeepScreenOn()
                }
            }
        }
    }

    fun hide() {
        rootView?.let { view ->
            windowManager.removeView(view)
        }
        rootView = null
        layoutParams = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }
    }

    private fun updateKeepScreenOn() {
        layoutParams?.let { params ->
            params.flags =
                if (keepScreenOn) {
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                } else {
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                }
            rootView?.let { windowManager.updateViewLayout(it, params) }
        }
    }

    private fun clampX(x: Int): Int {
        val display = windowManager.currentWindowMetrics.bounds
        val maxX = display.width() - 100
        return x.coerceIn(0, maxX.coerceAtLeast(0))
    }

    private fun clampY(y: Int): Int {
        val display = windowManager.currentWindowMetrics.bounds
        val maxY = display.height() - 100
        return y.coerceIn(0, maxY.coerceAtLeast(0))
    }
}
