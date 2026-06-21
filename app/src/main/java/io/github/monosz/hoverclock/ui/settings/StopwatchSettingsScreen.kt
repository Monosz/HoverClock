package io.github.monosz.hoverclock.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.monosz.hoverclock.R
import io.github.monosz.hoverclock.data.SettingsRepository
import io.github.monosz.hoverclock.model.OverlaySettings
import kotlinx.coroutines.launch

@Composable
fun StopwatchSettingsScreen(
    instanceId: String,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val showMilliseconds by settingsRepository.stopwatchShowMilliseconds(instanceId).collectAsStateWithLifecycle(
        initialValue = true,
    )
    val autoStart by settingsRepository.stopwatchAutoStart(instanceId).collectAsStateWithLifecycle(initialValue = false)
    val overlaySettings by settingsRepository.overlaySettings(instanceId).collectAsStateWithLifecycle(
        initialValue = OverlaySettings(),
    )

    SettingsScreenContainer(modifier = modifier) {
        InstanceNameField(instanceId = instanceId, settingsRepository = settingsRepository)

        InstanceIconPickerField(instanceId = instanceId, settingsRepository = settingsRepository)

        SettingSwitch(
            label = stringResource(R.string.setting_show_milliseconds),
            checked = showMilliseconds,
            onCheckedChange = { scope.launch { settingsRepository.setStopwatchShowMilliseconds(instanceId, it) } },
        )
        SettingSwitch(
            label = stringResource(R.string.setting_auto_start),
            checked = autoStart,
            onCheckedChange = { scope.launch { settingsRepository.setStopwatchAutoStart(instanceId, it) } },
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
