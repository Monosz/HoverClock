package io.github.monosz.hoverclock.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.monosz.hoverclock.R
import io.github.monosz.hoverclock.model.OverlaySettings

@Composable
fun AppearanceSection(
    settings: OverlaySettings,
    onRunningBackgroundColorChange: (Long) -> Unit,
    onPausedBackgroundColorChange: (Long) -> Unit,
    onTextColorChange: (Long) -> Unit,
    onBackgroundAlphaChange: (Float) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onLockPositionChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.appearance_section_title),
            style = MaterialTheme.typography.titleMedium,
        )

        ColorSettingRow(
            label = stringResource(R.string.setting_running_background_color),
            colorArgb = settings.runningBackgroundColor,
            onColorSelected = onRunningBackgroundColorChange,
        )
        ColorSettingRow(
            label = stringResource(R.string.setting_paused_background_color),
            colorArgb = settings.pausedBackgroundColor,
            onColorSelected = onPausedBackgroundColorChange,
        )
        ColorSettingRow(
            label = stringResource(R.string.setting_text_color),
            colorArgb = settings.textColor,
            onColorSelected = onTextColorChange,
        )

        SettingSlider(
            label = stringResource(R.string.setting_background_opacity),
            value = settings.backgroundAlpha,
            valueRange = 0.2f..1f,
            onValueChange = onBackgroundAlphaChange,
            valueLabel = "${(settings.backgroundAlpha * 100).toInt()}%",
        )

        SettingSlider(
            label = stringResource(R.string.setting_font_size),
            value = settings.fontSizeSp,
            valueRange = 16f..64f,
            onValueChange = onFontSizeChange,
            valueLabel = "${settings.fontSizeSp.toInt()} sp",
        )

        SettingSlider(
            label = stringResource(R.string.setting_corner_radius),
            value = settings.cornerRadiusDp,
            valueRange = 0f..32f,
            onValueChange = onCornerRadiusChange,
            valueLabel = "${settings.cornerRadiusDp.toInt()} dp",
        )

        SettingSwitch(
            label = stringResource(R.string.setting_lock_position),
            checked = settings.lockPosition,
            onCheckedChange = onLockPositionChange,
        )

        SettingSwitch(
            label = stringResource(R.string.setting_keep_screen_on),
            checked = settings.keepScreenOn,
            onCheckedChange = onKeepScreenOnChange,
        )
    }
}

@Composable
fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = valueLabel, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

@Composable
fun SettingsScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}
