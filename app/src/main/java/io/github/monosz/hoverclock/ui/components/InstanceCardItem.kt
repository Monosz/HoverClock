package io.github.monosz.hoverclock.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.monosz.hoverclock.R
import io.github.monosz.hoverclock.data.SettingsRepository
import io.github.monosz.hoverclock.model.InstanceIcon
import io.github.monosz.hoverclock.model.OverlayInstance
import io.github.monosz.hoverclock.model.OverlaySettings
import io.github.monosz.hoverclock.model.TimeMode
import io.github.monosz.hoverclock.ui.settings.InstanceIconPickerDialog
import kotlinx.coroutines.launch

@Composable
fun InstanceCardItem(
    instance: OverlayInstance,
    settingsRepository: SettingsRepository,
    isRunning: Boolean,
    onLaunch: () -> Unit,
    onStop: () -> Unit,
    onConfigure: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var showIconPicker by remember { mutableStateOf(false) }
    val overlaySettings by settingsRepository.overlaySettings(instance.id).collectAsStateWithLifecycle(
        initialValue = OverlaySettings(),
    )
    val sizeLabel = fontSizeLabelString(InstanceSummaryFormatter.fontSizeLabel(overlaySettings.fontSizeSp))
    val icon = InstanceIcon.fromName(instance.iconName, instance.mode)

    val secondToken =
        when (instance.mode) {
            TimeMode.Clock -> {
                val use24Hour by settingsRepository.clockUse24Hour(instance.id).collectAsStateWithLifecycle(
                    initialValue = false,
                )
                stringResource(if (use24Hour) R.string.summary_24h else R.string.summary_12h)
            }
            TimeMode.Stopwatch -> {
                val showMs by settingsRepository.stopwatchShowMilliseconds(instance.id).collectAsStateWithLifecycle(
                    initialValue = true,
                )
                stringResource(if (showMs) R.string.summary_ms else R.string.summary_standard)
            }
            TimeMode.Timer -> {
                val durationMillis by settingsRepository.timerDurationMillis(instance.id).collectAsStateWithLifecycle(
                    initialValue = 5 * 60 * 1000L,
                )
                InstanceSummaryFormatter.formatDuration(durationMillis)
            }
        }

    InstanceCard(
        instance = instance,
        icon = icon.imageVector(),
        isRunning = isRunning,
        summaryText = InstanceSummaryFormatter.buildSummary(sizeLabel, secondToken),
        onToggle = { enabled ->
            if (enabled) onLaunch() else onStop()
        },
        onConfigure = onConfigure,
        onDelete = onDelete,
        onIconClick = { showIconPicker = true },
        modifier = modifier,
    )

    if (showIconPicker) {
        InstanceIconPickerDialog(
            selectedIconName = instance.iconName,
            onDismiss = { showIconPicker = false },
            onIconSelected = { iconName ->
                scope.launch { settingsRepository.setInstanceIcon(instance.id, iconName) }
            },
        )
    }
}

@Composable
private fun fontSizeLabelString(label: FontSizeLabel): String =
    when (label) {
        FontSizeLabel.Small -> stringResource(R.string.font_size_small)
        FontSizeLabel.Medium -> stringResource(R.string.font_size_medium)
        FontSizeLabel.Large -> stringResource(R.string.font_size_large)
    }
