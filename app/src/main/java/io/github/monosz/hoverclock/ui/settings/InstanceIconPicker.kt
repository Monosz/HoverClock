package io.github.monosz.hoverclock.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.monosz.hoverclock.R
import io.github.monosz.hoverclock.data.SettingsRepository
import io.github.monosz.hoverclock.model.InstanceIcon
import io.github.monosz.hoverclock.model.TimeMode
import kotlinx.coroutines.launch

@Composable
fun InstanceIconPickerField(
    instanceId: String,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val instances by settingsRepository.allInstances().collectAsStateWithLifecycle(initialValue = emptyList())
    val instance = instances.firstOrNull { it.id == instanceId }
    val fallbackMode = instance?.mode ?: TimeMode.Clock
    val iconName = instance?.iconName ?: InstanceIcon.defaultFor(fallbackMode).name

    InstanceIconPickerSection(
        selectedIconName = iconName,
        onIconSelect = { scope.launch { settingsRepository.setInstanceIcon(instanceId, it) } },
        modifier = modifier,
    )
}

@Composable
fun InstanceIconPickerSection(
    selectedIconName: String,
    onIconSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.setting_instance_icon),
            style = MaterialTheme.typography.bodyLarge,
        )
        InstanceIconGrid(
            selectedIconName = selectedIconName,
            onIconSelected = onIconSelect,
        )
    }
}

@Composable
fun InstanceIconPickerDialog(
    selectedIconName: String,
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.setting_instance_icon),
                    style = MaterialTheme.typography.titleLarge,
                )
                InstanceIconGrid(
                    selectedIconName = selectedIconName,
                    onIconSelected = {
                        onIconSelected(it)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InstanceIconGrid(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectIconDescription = stringResource(R.string.action_select_icon)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InstanceIcon.all.forEach { icon ->
            val selected = icon.name == selectedIconName
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (selected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onIconSelected(icon.name) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon.imageVector(),
                    contentDescription = selectIconDescription,
                    modifier = Modifier.size(24.dp),
                    tint =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}
