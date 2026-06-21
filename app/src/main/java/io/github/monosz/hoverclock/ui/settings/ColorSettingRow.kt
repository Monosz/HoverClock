package io.github.monosz.hoverclock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import io.github.monosz.hoverclock.R
import io.github.monosz.hoverclock.util.toComposeColor
import io.github.monosz.hoverclock.util.toStoredLong

@Composable
fun ColorSettingRow(
    label: String,
    colorArgb: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colorArgb.toComposeColor()),
        )
    }

    if (showDialog) {
        ColorPickerDialog(
            label = label,
            initialColorArgb = colorArgb,
            onDismiss = { showDialog = false },
            onConfirm = { selected ->
                onColorSelected(selected)
                showDialog = false
            },
        )
    }
}

@Composable
private fun ColorPickerDialog(
    label: String,
    initialColorArgb: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val controller = rememberColorPickerController()
    var selectedColor by remember(initialColorArgb) { mutableLongStateOf(initialColorArgb) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HsvColorPicker(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                    controller = controller,
                    initialColor = initialColorArgb.toComposeColor(),
                    onColorChanged = { envelope ->
                        selectedColor = envelope.color.toStoredLong()
                    },
                )
                BrightnessSlider(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(35.dp),
                    controller = controller,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedColor) }) {
                Text(stringResource(R.string.color_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
