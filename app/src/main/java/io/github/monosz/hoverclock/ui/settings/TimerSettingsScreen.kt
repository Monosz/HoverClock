package io.github.monosz.hoverclock.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.monosz.hoverclock.R
import io.github.monosz.hoverclock.data.SettingsRepository
import io.github.monosz.hoverclock.model.OverlaySettings
import kotlinx.coroutines.launch

@Composable
fun TimerSettingsScreen(
    instanceId: String,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val durationMillis by settingsRepository.timerDurationMillis(instanceId).collectAsStateWithLifecycle(
        initialValue = 5 * 60 * 1000L,
    )
    val showMilliseconds by settingsRepository.timerShowMilliseconds(instanceId).collectAsStateWithLifecycle(
        initialValue = false,
    )
    val alertWhenFinished by settingsRepository.timerAlertWhenFinished(instanceId).collectAsStateWithLifecycle(
        initialValue = true,
    )
    val overlaySettings by settingsRepository.overlaySettings(instanceId).collectAsStateWithLifecycle(
        initialValue = OverlaySettings(),
    )

    val totalSeconds = (durationMillis / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    SettingsScreenContainer(modifier = modifier) {
        InstanceNameField(instanceId = instanceId, settingsRepository = settingsRepository)

        InstanceIconPickerField(instanceId = instanceId, settingsRepository = settingsRepository)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DurationStepper(
                label = stringResource(R.string.setting_hours),
                value = hours,
                range = 0..23,
                onValueChange = { h ->
                    val newDuration = ((h * 3600L) + (minutes * 60L) + seconds) * 1000L
                    scope.launch { settingsRepository.setTimerDurationMillis(instanceId, newDuration) }
                },
            )
            DurationStepper(
                label = stringResource(R.string.setting_minutes),
                value = minutes,
                range = 0..59,
                onValueChange = { m ->
                    val newDuration = ((hours * 3600L) + (m * 60L) + seconds) * 1000L
                    scope.launch {
                        settingsRepository.setTimerDurationMillis(instanceId, newDuration.coerceAtLeast(1000L))
                    }
                },
            )
            DurationStepper(
                label = stringResource(R.string.setting_seconds),
                value = seconds,
                range = 0..59,
                onValueChange = { s ->
                    val newDuration = ((hours * 3600L) + (minutes * 60L) + s) * 1000L
                    scope.launch {
                        settingsRepository.setTimerDurationMillis(instanceId, newDuration.coerceAtLeast(1000L))
                    }
                },
            )
        }

        SettingSwitch(
            label = stringResource(R.string.setting_show_milliseconds),
            checked = showMilliseconds,
            onCheckedChange = { scope.launch { settingsRepository.setTimerShowMilliseconds(instanceId, it) } },
        )
        SettingSwitch(
            label = stringResource(R.string.setting_alert_when_finished),
            checked = alertWhenFinished,
            onCheckedChange = { scope.launch { settingsRepository.setTimerAlertWhenFinished(instanceId, it) } },
        )

        AppearanceSection(
            settings = overlaySettings,
            onRunningBackgroundColorChange = {
                scope.launch { settingsRepository.setRunningBackgroundColor(instanceId, it) }
            },
            onPausedBackgroundColorChange = {
                scope.launch { settingsRepository.setPausedBackgroundColor(instanceId, it) }
            },
            onTextColorChange = { scope.launch { settingsRepository.setTextColor(instanceId, it) } },
            onBackgroundAlphaChange = { scope.launch { settingsRepository.setBackgroundAlpha(instanceId, it) } },
            onFontSizeChange = { scope.launch { settingsRepository.setFontSizeSp(instanceId, it) } },
            onCornerRadiusChange = { scope.launch { settingsRepository.setCornerRadiusDp(instanceId, it) } },
            onLockPositionChange = { scope.launch { settingsRepository.setLockPosition(instanceId, it) } },
            onKeepScreenOnChange = { scope.launch { settingsRepository.setKeepScreenOn(instanceId, it) } },
        )
    }
}

@Composable
private fun DurationStepper(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        androidx.compose.material3.Text(text = "$label: $value")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.TextButton(onClick = {
                if (value > range.first) onValueChange(value - 1)
            }) {
                androidx.compose.material3.Text("-")
            }
            androidx.compose.material3.TextButton(onClick = {
                if (value < range.last) onValueChange(value + 1)
            }) {
                androidx.compose.material3.Text("+")
            }
        }
    }
}
