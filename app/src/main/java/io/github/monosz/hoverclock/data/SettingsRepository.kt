package io.github.monosz.hoverclock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.monosz.hoverclock.model.InstanceIcon
import io.github.monosz.hoverclock.model.OverlayInstance
import io.github.monosz.hoverclock.model.OverlaySettings
import io.github.monosz.hoverclock.model.TimeMode
import io.github.monosz.hoverclock.ui.navigation.MainTab
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hoverclock_settings")

class SettingsRepository(private val context: Context) {
    private val initMutex = Mutex()
    private var defaultsInitialized = false

    fun instances(mode: TimeMode): Flow<List<OverlayInstance>> =
        context.dataStore.data
            .onStart { ensureDefaultInstances() }
            .map { prefs -> parseInstances(prefs[Keys.OVERLAY_INSTANCES]).filter { it.mode == mode } }

    fun allInstances(): Flow<List<OverlayInstance>> =
        context.dataStore.data
            .onStart { ensureDefaultInstances() }
            .map { prefs -> parseInstances(prefs[Keys.OVERLAY_INSTANCES]) }

    fun lastSelectedTab(): Flow<MainTab> =
        context.dataStore.data
            .map { prefs -> MainTab.fromName(prefs[Keys.LAST_SELECTED_TAB]) }

    suspend fun setLastSelectedTab(tab: MainTab) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SELECTED_TAB] = tab.name
        }
    }

    suspend fun awaitMainScreenReady(): MainTab {
        ensureDefaultInstances()
        val tab = lastSelectedTab().first()
        instances(tab.timeMode).first()
        return tab
    }

    suspend fun getInstance(id: String): OverlayInstance? {
        ensureDefaultInstances()
        return parseInstances(context.dataStore.data.first()[Keys.OVERLAY_INSTANCES])
            .firstOrNull { it.id == id }
    }

    suspend fun createInstance(mode: TimeMode): OverlayInstance {
        ensureDefaultInstances()
        val existing = parseInstances(context.dataStore.data.first()[Keys.OVERLAY_INSTANCES])
        val instance =
            OverlayInstance(
                id = UUID.randomUUID().toString(),
                mode = mode,
                name = InstanceNameGenerator.nextName(mode, existing),
            )
        context.dataStore.edit { prefs ->
            val updated = existing + instance
            prefs[Keys.OVERLAY_INSTANCES] = serializeInstances(updated)
        }
        return instance
    }

    suspend fun renameInstance(
        id: String,
        name: String,
    ) {
        ensureDefaultInstances()
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { prefs ->
            val instances =
                parseInstances(prefs[Keys.OVERLAY_INSTANCES])
                    .map { if (it.id == id) it.copy(name = trimmed) else it }
            prefs[Keys.OVERLAY_INSTANCES] = serializeInstances(instances)
        }
    }

    suspend fun setInstanceIcon(
        id: String,
        iconName: String,
    ) {
        if (!InstanceIcon.isValidName(iconName)) return
        ensureDefaultInstances()
        context.dataStore.edit { prefs ->
            val instances =
                parseInstances(prefs[Keys.OVERLAY_INSTANCES])
                    .map { if (it.id == id) it.copy(iconName = iconName) else it }
            prefs[Keys.OVERLAY_INSTANCES] = serializeInstances(instances)
        }
    }

    suspend fun deleteInstance(id: String) {
        ensureDefaultInstances()
        context.dataStore.edit { prefs ->
            val instances = parseInstances(prefs[Keys.OVERLAY_INSTANCES]).filter { it.id != id }
            prefs[Keys.OVERLAY_INSTANCES] = serializeInstances(instances)
            InstanceKeys.removeAll(id, prefs)
        }
    }

    fun overlaySettings(instanceId: String): Flow<OverlaySettings> =
        context.dataStore.data
            .onStart { ensureDefaultInstances() }
            .map { prefs -> readOverlaySettings(prefs, instanceId) }

    fun clockUse24Hour(instanceId: String): Flow<Boolean> = instanceBooleanFlow(instanceId, InstanceKeys.CLOCK_USE_24_HOUR, false)

    fun clockShowSeconds(instanceId: String): Flow<Boolean> = instanceBooleanFlow(instanceId, InstanceKeys.CLOCK_SHOW_SECONDS, true)

    fun stopwatchShowMilliseconds(instanceId: String): Flow<Boolean> =
        instanceBooleanFlow(instanceId, InstanceKeys.STOPWATCH_SHOW_MILLISECONDS, true)

    fun stopwatchAutoStart(instanceId: String): Flow<Boolean> = instanceBooleanFlow(instanceId, InstanceKeys.STOPWATCH_AUTO_START, false)

    fun timerDurationMillis(instanceId: String): Flow<Long> =
        instanceLongFlow(instanceId, InstanceKeys.TIMER_DURATION_MILLIS, 5 * 60 * 1000L)

    fun timerShowMilliseconds(instanceId: String): Flow<Boolean> =
        instanceBooleanFlow(instanceId, InstanceKeys.TIMER_SHOW_MILLISECONDS, false)

    fun timerAlertWhenFinished(instanceId: String): Flow<Boolean> =
        instanceBooleanFlow(instanceId, InstanceKeys.TIMER_ALERT_WHEN_FINISHED, true)

    suspend fun setOverlayPosition(
        instanceId: String,
        x: Int,
        y: Int,
    ) {
        context.dataStore.edit { prefs ->
            prefs[InstanceKeys.intKey(instanceId, InstanceKeys.POSITION_X)] = x
            prefs[InstanceKeys.intKey(instanceId, InstanceKeys.POSITION_Y)] = y
        }
    }

    suspend fun setBackgroundAlpha(
        instanceId: String,
        alpha: Float,
    ) {
        editFloat(instanceId, InstanceKeys.BACKGROUND_ALPHA, alpha)
    }

    suspend fun setRunningBackgroundColor(
        instanceId: String,
        color: Long,
    ) {
        editLong(instanceId, InstanceKeys.BACKGROUND_COLOR, color)
    }

    suspend fun setPausedBackgroundColor(
        instanceId: String,
        color: Long,
    ) {
        editLong(instanceId, InstanceKeys.PAUSED_BACKGROUND_COLOR, color)
    }

    suspend fun setTextColor(
        instanceId: String,
        color: Long,
    ) {
        editLong(instanceId, InstanceKeys.TEXT_COLOR, color)
    }

    suspend fun setFontSizeSp(
        instanceId: String,
        size: Float,
    ) {
        editFloat(instanceId, InstanceKeys.FONT_SIZE_SP, size)
    }

    suspend fun setCornerRadiusDp(
        instanceId: String,
        radius: Float,
    ) {
        editFloat(instanceId, InstanceKeys.CORNER_RADIUS_DP, radius)
    }

    suspend fun setLockPosition(
        instanceId: String,
        locked: Boolean,
    ) {
        editBoolean(instanceId, InstanceKeys.LOCK_POSITION, locked)
    }

    suspend fun setKeepScreenOn(
        instanceId: String,
        enabled: Boolean,
    ) {
        editBoolean(instanceId, InstanceKeys.KEEP_SCREEN_ON, enabled)
    }

    suspend fun setClockUse24Hour(
        instanceId: String,
        use24Hour: Boolean,
    ) {
        editBoolean(instanceId, InstanceKeys.CLOCK_USE_24_HOUR, use24Hour)
    }

    suspend fun setClockShowSeconds(
        instanceId: String,
        showSeconds: Boolean,
    ) {
        editBoolean(instanceId, InstanceKeys.CLOCK_SHOW_SECONDS, showSeconds)
    }

    suspend fun setStopwatchShowMilliseconds(
        instanceId: String,
        show: Boolean,
    ) {
        editBoolean(instanceId, InstanceKeys.STOPWATCH_SHOW_MILLISECONDS, show)
    }

    suspend fun setStopwatchAutoStart(
        instanceId: String,
        autoStart: Boolean,
    ) {
        editBoolean(instanceId, InstanceKeys.STOPWATCH_AUTO_START, autoStart)
    }

    suspend fun setTimerDurationMillis(
        instanceId: String,
        duration: Long,
    ) {
        editLong(instanceId, InstanceKeys.TIMER_DURATION_MILLIS, duration)
    }

    suspend fun setTimerShowMilliseconds(
        instanceId: String,
        show: Boolean,
    ) {
        editBoolean(instanceId, InstanceKeys.TIMER_SHOW_MILLISECONDS, show)
    }

    suspend fun setTimerAlertWhenFinished(
        instanceId: String,
        alert: Boolean,
    ) {
        editBoolean(instanceId, InstanceKeys.TIMER_ALERT_WHEN_FINISHED, alert)
    }

    suspend fun ensureDefaultInstances() {
        if (defaultsInitialized) return
        initMutex.withLock {
            if (defaultsInitialized) return
            val prefs = context.dataStore.data.first()
            val existing = parseInstances(prefs[Keys.OVERLAY_INSTANCES])
            if (existing.isEmpty()) {
                val defaults =
                    listOf(
                        OverlayInstance(
                            id = UUID.randomUUID().toString(),
                            mode = TimeMode.Clock,
                            name = "Clock 1",
                        ),
                        OverlayInstance(
                            id = UUID.randomUUID().toString(),
                            mode = TimeMode.Stopwatch,
                            name = "Stopwatch 1",
                        ),
                        OverlayInstance(
                            id = UUID.randomUUID().toString(),
                            mode = TimeMode.Timer,
                            name = "Timer 1",
                        ),
                    )
                context.dataStore.edit { mutablePrefs ->
                    mutablePrefs[Keys.OVERLAY_INSTANCES] = serializeInstances(defaults)
                }
            }
            defaultsInitialized = true
        }
    }

    private fun readOverlaySettings(
        prefs: Preferences,
        instanceId: String,
    ): OverlaySettings =
        OverlaySettings(
            runningBackgroundColor = prefs[InstanceKeys.longKey(instanceId, InstanceKeys.BACKGROUND_COLOR)] ?: 0xFF1C1B1F,
            pausedBackgroundColor = prefs[InstanceKeys.longKey(instanceId, InstanceKeys.PAUSED_BACKGROUND_COLOR)] ?: 0xFF5D4037,
            textColor = prefs[InstanceKeys.longKey(instanceId, InstanceKeys.TEXT_COLOR)] ?: 0xFFFFFFFF,
            backgroundAlpha = prefs[InstanceKeys.floatKey(instanceId, InstanceKeys.BACKGROUND_ALPHA)] ?: 0.85f,
            fontSizeSp = prefs[InstanceKeys.floatKey(instanceId, InstanceKeys.FONT_SIZE_SP)] ?: 32f,
            cornerRadiusDp = prefs[InstanceKeys.floatKey(instanceId, InstanceKeys.CORNER_RADIUS_DP)] ?: 16f,
            positionX = prefs[InstanceKeys.intKey(instanceId, InstanceKeys.POSITION_X)] ?: -1,
            positionY = prefs[InstanceKeys.intKey(instanceId, InstanceKeys.POSITION_Y)] ?: -1,
            lockPosition = prefs[InstanceKeys.booleanKey(instanceId, InstanceKeys.LOCK_POSITION)] ?: false,
            keepScreenOn = prefs[InstanceKeys.booleanKey(instanceId, InstanceKeys.KEEP_SCREEN_ON)] ?: false,
        )

    private fun instanceBooleanFlow(
        instanceId: String,
        suffix: String,
        default: Boolean,
    ): Flow<Boolean> =
        context.dataStore.data
            .onStart { ensureDefaultInstances() }
            .map { prefs -> prefs[InstanceKeys.booleanKey(instanceId, suffix)] ?: default }

    private fun instanceLongFlow(
        instanceId: String,
        suffix: String,
        default: Long,
    ): Flow<Long> =
        context.dataStore.data
            .onStart { ensureDefaultInstances() }
            .map { prefs -> prefs[InstanceKeys.longKey(instanceId, suffix)] ?: default }

    private suspend fun editBoolean(
        instanceId: String,
        suffix: String,
        value: Boolean,
    ) {
        context.dataStore.edit { prefs ->
            prefs[InstanceKeys.booleanKey(instanceId, suffix)] = value
        }
    }

    private suspend fun editFloat(
        instanceId: String,
        suffix: String,
        value: Float,
    ) {
        context.dataStore.edit { prefs ->
            prefs[InstanceKeys.floatKey(instanceId, suffix)] = value
        }
    }

    private suspend fun editLong(
        instanceId: String,
        suffix: String,
        value: Long,
    ) {
        context.dataStore.edit { prefs ->
            prefs[InstanceKeys.longKey(instanceId, suffix)] = value
        }
    }

    private fun parseInstances(json: String?): List<OverlayInstance> {
        if (json.isNullOrBlank()) return emptyList()
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val mode = TimeMode.entries.firstOrNull { it.name == obj.getString("mode") } ?: continue
                add(
                    OverlayInstance(
                        id = obj.getString("id"),
                        mode = mode,
                        name = obj.getString("name"),
                        iconName = parseIconName(obj, mode),
                    ),
                )
            }
        }
    }

    private fun parseIconName(
        obj: JSONObject,
        mode: TimeMode,
    ): String {
        if (obj.has("iconName")) {
            val name = obj.getString("iconName")
            if (InstanceIcon.isValidName(name)) return name
        }
        return InstanceIcon.defaultFor(mode).name
    }

    private fun serializeInstances(instances: List<OverlayInstance>): String {
        val array = JSONArray()
        instances.forEach { instance ->
            array.put(
                JSONObject()
                    .put("id", instance.id)
                    .put("mode", instance.mode.name)
                    .put("name", instance.name)
                    .put("iconName", instance.iconName),
            )
        }
        return array.toString()
    }

    private object Keys {
        val OVERLAY_INSTANCES = stringPreferencesKey("overlay_instances")
        val LAST_SELECTED_TAB = stringPreferencesKey("last_selected_tab")
    }

    private object InstanceKeys {
        const val BACKGROUND_COLOR = "background_color"
        const val PAUSED_BACKGROUND_COLOR = "paused_background_color"
        const val TEXT_COLOR = "text_color"
        const val BACKGROUND_ALPHA = "background_alpha"
        const val FONT_SIZE_SP = "font_size_sp"
        const val CORNER_RADIUS_DP = "corner_radius_dp"
        const val POSITION_X = "position_x"
        const val POSITION_Y = "position_y"
        const val LOCK_POSITION = "lock_position"
        const val KEEP_SCREEN_ON = "keep_screen_on"
        const val CLOCK_USE_24_HOUR = "clock_use_24_hour"
        const val CLOCK_SHOW_SECONDS = "clock_show_seconds"
        const val STOPWATCH_SHOW_MILLISECONDS = "stopwatch_show_milliseconds"
        const val STOPWATCH_AUTO_START = "stopwatch_auto_start"
        const val TIMER_DURATION_MILLIS = "timer_duration_millis"
        const val TIMER_SHOW_MILLISECONDS = "timer_show_milliseconds"
        const val TIMER_ALERT_WHEN_FINISHED = "timer_alert_when_finished"

        fun longKey(
            instanceId: String,
            suffix: String,
        ) = longPreferencesKey("instance_${instanceId}_$suffix")

        fun floatKey(
            instanceId: String,
            suffix: String,
        ) = floatPreferencesKey("instance_${instanceId}_$suffix")

        fun intKey(
            instanceId: String,
            suffix: String,
        ) = intPreferencesKey("instance_${instanceId}_$suffix")

        fun booleanKey(
            instanceId: String,
            suffix: String,
        ) = booleanPreferencesKey("instance_${instanceId}_$suffix")

        fun removeAll(
            instanceId: String,
            prefs: androidx.datastore.preferences.core.MutablePreferences,
        ) {
            prefs.remove(longKey(instanceId, BACKGROUND_COLOR))
            prefs.remove(longKey(instanceId, PAUSED_BACKGROUND_COLOR))
            prefs.remove(longKey(instanceId, TEXT_COLOR))
            prefs.remove(floatKey(instanceId, BACKGROUND_ALPHA))
            prefs.remove(floatKey(instanceId, FONT_SIZE_SP))
            prefs.remove(floatKey(instanceId, CORNER_RADIUS_DP))
            prefs.remove(intKey(instanceId, POSITION_X))
            prefs.remove(intKey(instanceId, POSITION_Y))
            prefs.remove(booleanKey(instanceId, LOCK_POSITION))
            prefs.remove(booleanKey(instanceId, KEEP_SCREEN_ON))
            prefs.remove(booleanKey(instanceId, CLOCK_USE_24_HOUR))
            prefs.remove(booleanKey(instanceId, CLOCK_SHOW_SECONDS))
            prefs.remove(booleanKey(instanceId, STOPWATCH_SHOW_MILLISECONDS))
            prefs.remove(booleanKey(instanceId, STOPWATCH_AUTO_START))
            prefs.remove(longKey(instanceId, TIMER_DURATION_MILLIS))
            prefs.remove(booleanKey(instanceId, TIMER_SHOW_MILLISECONDS))
            prefs.remove(booleanKey(instanceId, TIMER_ALERT_WHEN_FINISHED))
        }
    }
}
