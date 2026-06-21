package io.github.monosz.hoverclock.service

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import io.github.monosz.hoverclock.MainActivity
import io.github.monosz.hoverclock.MainApplication
import io.github.monosz.hoverclock.R
import io.github.monosz.hoverclock.data.SettingsRepository
import io.github.monosz.hoverclock.engine.ClockEngine
import io.github.monosz.hoverclock.engine.StopwatchEngine
import io.github.monosz.hoverclock.engine.TimeProvider
import io.github.monosz.hoverclock.engine.TimerEngine
import io.github.monosz.hoverclock.model.OverlayInstance
import io.github.monosz.hoverclock.model.TimeMode
import io.github.monosz.hoverclock.overlay.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OverlayService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var settingsRepository: SettingsRepository
    private val sessions = mutableMapOf<String, OverlaySession>()

    override fun onCreate() {
        super.onCreate()
        settingsRepository = (application as MainApplication).settingsRepository
        createNotificationChannel()
        instance = this
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_STOP_ALL -> {
                stopAllSessions()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_STOP_INSTANCE -> {
                val instanceId = intent.getStringExtra(EXTRA_INSTANCE_ID) ?: return START_NOT_STICKY
                serviceScope.launch {
                    stopSession(instanceId)
                    stopSelfIfEmpty()
                }
                return START_STICKY
            }
        }

        val instanceId = intent?.getStringExtra(EXTRA_INSTANCE_ID) ?: return START_NOT_STICKY

        serviceScope.launch {
            startSession(instanceId)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopAllSessions()
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun startSession(instanceId: String) {
        if (sessions.containsKey(instanceId)) return

        settingsRepository.ensureDefaultInstances()
        val overlayInstance = settingsRepository.getInstance(instanceId) ?: return
        val mode = overlayInstance.mode

        val isFirstSession = sessions.isEmpty()
        if (isFirstSession) {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        val (provider, observerJobs) = createEngine(overlayInstance)
        val spawnOffsetIndex = sessions.size
        val density = resources.displayMetrics.density

        val overlayManager =
            OverlayManager(
                context = this,
                settingsRepository = settingsRepository,
                instanceId = instanceId,
                scope = serviceScope,
                timeMode = mode,
                defaultSpawnOffsetPx = OverlaySpawnOffset.offsetPx(spawnOffsetIndex, density),
                onSingleTap = { handleOverlayTap(provider) },
                onDoubleTap = { handleOverlayReset(provider) },
                onLongPress = { showExitConfirmation(overlayInstance) },
            )

        val session =
            OverlaySession(
                instanceId = instanceId,
                instanceName = overlayInstance.name,
                mode = mode,
                overlayManager = overlayManager,
                timeProvider = provider,
                observerJobs = observerJobs,
            )
        sessions[instanceId] = session
        syncActiveInstanceIds()
        overlayManager.show(provider)
        updateNotification()
    }

    private fun stopSession(instanceId: String) {
        val session = sessions.remove(instanceId) ?: return
        session.observerJobs.forEach { it.cancel() }
        session.overlayManager.hide()
        session.timeProvider.release()
        syncActiveInstanceIds()
        updateNotification()
    }

    private fun stopAllSessions() {
        sessions.keys.toList().forEach { stopSession(it) }
    }

    private fun stopSelfIfEmpty() {
        if (sessions.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun syncActiveInstanceIds() {
        _activeInstanceIds.value = sessions.keys.toSet()
    }

    private suspend fun createEngine(overlayInstance: OverlayInstance): Pair<TimeProvider, List<Job>> {
        val instanceId = overlayInstance.id
        return when (overlayInstance.mode) {
            TimeMode.Clock -> {
                val use24Hour = settingsRepository.clockUse24Hour(instanceId).first()
                val showSeconds = settingsRepository.clockShowSeconds(instanceId).first()
                val engine = ClockEngine(serviceScope, use24Hour, showSeconds)
                val job = launchClockSettingsObserver(engine, instanceId)
                engine to listOf(job)
            }
            TimeMode.Stopwatch -> {
                val showMs = settingsRepository.stopwatchShowMilliseconds(instanceId).first()
                val autoStart = settingsRepository.stopwatchAutoStart(instanceId).first()
                val engine = StopwatchEngine(serviceScope, showMs, autoStart)
                val job = launchStopwatchSettingsObserver(engine, instanceId)
                engine to listOf(job)
            }
            TimeMode.Timer -> {
                val duration = settingsRepository.timerDurationMillis(instanceId).first()
                val showMs = settingsRepository.timerShowMilliseconds(instanceId).first()
                val alert = settingsRepository.timerAlertWhenFinished(instanceId).first()
                val instanceName = overlayInstance.name
                val engine =
                    TimerEngine(
                        scope = serviceScope,
                        durationMillis = duration,
                        showMilliseconds = showMs,
                        onFinished = {
                            if (alert) showTimerFinishedNotification(instanceName)
                        },
                    )
                val job = launchTimerSettingsObserver(engine, instanceId)
                engine to listOf(job)
            }
        }
    }

    private fun launchClockSettingsObserver(
        engine: ClockEngine,
        instanceId: String,
    ): Job {
        return serviceScope.launch {
            combine(
                settingsRepository.clockUse24Hour(instanceId),
                settingsRepository.clockShowSeconds(instanceId),
            ) { use24Hour, showSeconds ->
                engine.updateSettings(use24Hour, showSeconds)
            }.collect { }
        }
    }

    private fun launchStopwatchSettingsObserver(
        engine: StopwatchEngine,
        instanceId: String,
    ): Job {
        return serviceScope.launch {
            settingsRepository.stopwatchShowMilliseconds(instanceId).collect { showMs ->
                engine.updateSettings(showMs)
            }
        }
    }

    private fun launchTimerSettingsObserver(
        engine: TimerEngine,
        instanceId: String,
    ): Job {
        return serviceScope.launch {
            combine(
                settingsRepository.timerDurationMillis(instanceId),
                settingsRepository.timerShowMilliseconds(instanceId),
            ) { duration, showMs ->
                engine.updateSettings(duration, showMs)
            }.collect { }
        }
    }

    private fun handleOverlayTap(provider: TimeProvider) {
        when (provider) {
            is StopwatchEngine -> provider.toggle()
            is TimerEngine -> provider.toggle()
            else -> Unit
        }
    }

    private fun handleOverlayReset(provider: TimeProvider) {
        when (provider) {
            is StopwatchEngine -> provider.reset()
            is TimerEngine -> provider.reset()
            else -> Unit
        }
    }

    private fun showExitConfirmation(overlayInstance: OverlayInstance) {
        val dialog =
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.overlay_exit_title, overlayInstance.name))
                .setMessage(getString(R.string.overlay_exit_message, overlayInstance.name))
                .setPositiveButton(R.string.overlay_exit_confirm) { _, _ ->
                    stopInstance(this, overlayInstance.id)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val names = sessions.values.map { it.instanceName }
        val openAppIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val stopAllIntent =
            PendingIntent.getService(
                this,
                1,
                Intent(this, OverlayService::class.java).apply { action = ACTION_STOP_ALL },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val title =
            if (names.size == 1) {
                getString(R.string.notification_title, names.first())
            } else {
                getString(R.string.notification_title_multiple, names.size)
            }
        val summary = OverlayNotificationText.summaryLine(names)

        val inboxStyle =
            NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
        names.forEach { inboxStyle.addLine(it) }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(summary.ifEmpty { getString(R.string.notification_text) })
            .setStyle(inboxStyle)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.notification_stop_all), stopAllIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        if (sessions.isEmpty()) return
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    private fun showTimerFinishedNotification(instanceName: String) {
        val manager = getSystemService(NotificationManager::class.java)
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.timer_finished_title, instanceName))
                .setContentText(getString(R.string.timer_finished_text, instanceName))
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
        manager.notify(TIMER_FINISHED_NOTIFICATION_ID + instanceName.hashCode(), notification)
    }

    companion object {
        const val EXTRA_INSTANCE_ID = "extra_instance_id"
        private const val ACTION_START = "io.github.monosz.hoverclock.START_OVERLAY"
        private const val ACTION_STOP_INSTANCE = "io.github.monosz.hoverclock.STOP_OVERLAY_INSTANCE"
        private const val ACTION_STOP_ALL = "io.github.monosz.hoverclock.STOP_ALL_OVERLAYS"
        private const val CHANNEL_ID = "hoverclock_overlay"
        private const val NOTIFICATION_ID = 1
        private const val TIMER_FINISHED_NOTIFICATION_ID = 1000

        private val _activeInstanceIds = MutableStateFlow<Set<String>>(emptySet())
        val activeInstanceIds: StateFlow<Set<String>> = _activeInstanceIds.asStateFlow()

        @Volatile
        var instance: OverlayService? = null
            private set

        fun isRunning(): Boolean = _activeInstanceIds.value.isNotEmpty()

        fun isInstanceRunning(instanceId: String): Boolean = instanceId in _activeInstanceIds.value

        fun start(
            context: Context,
            instanceId: String,
        ) {
            val intent =
                Intent(context, OverlayService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_INSTANCE_ID, instanceId)
                }
            context.startForegroundService(intent)
        }

        fun stopInstance(
            context: Context,
            instanceId: String,
        ) {
            val intent =
                Intent(context, OverlayService::class.java).apply {
                    action = ACTION_STOP_INSTANCE
                    putExtra(EXTRA_INSTANCE_ID, instanceId)
                }
            context.startService(intent)
        }

        fun stopAll(context: Context) {
            val intent =
                Intent(context, OverlayService::class.java).apply {
                    action = ACTION_STOP_ALL
                }
            context.startService(intent)
        }

        fun stop(context: Context) = stopAll(context)
    }
}
