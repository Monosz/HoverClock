package io.github.monosz.hoverclock.ui.mode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.monosz.hoverclock.R
import io.github.monosz.hoverclock.data.SettingsRepository
import io.github.monosz.hoverclock.model.OverlayInstance
import io.github.monosz.hoverclock.model.TimeMode
import io.github.monosz.hoverclock.service.OverlayService
import io.github.monosz.hoverclock.ui.components.InstanceCardItem
import kotlinx.coroutines.launch

@Composable
fun ModeInstancesScreen(
    mode: TimeMode,
    settingsRepository: SettingsRepository,
    onLaunch: (String) -> Unit,
    onStop: (String) -> Unit,
    onConfigure: (OverlayInstance) -> Unit,
    onDeleteInstance: suspend (OverlayInstance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val instances by settingsRepository.instances(mode).collectAsStateWithLifecycle(initialValue = emptyList())
    val activeInstanceIds by OverlayService.activeInstanceIds.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<OverlayInstance?>(null) }

    val description =
        when (mode) {
            TimeMode.Clock -> stringResource(R.string.mode_clock_description)
            TimeMode.Stopwatch -> stringResource(R.string.mode_stopwatch_description)
            TimeMode.Timer -> stringResource(R.string.mode_timer_description)
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        instances.forEach { instance ->
            InstanceCardItem(
                instance = instance,
                settingsRepository = settingsRepository,
                isRunning = instance.id in activeInstanceIds,
                onLaunch = { onLaunch(instance.id) },
                onStop = { onStop(instance.id) },
                onConfigure = { onConfigure(instance) },
                onDelete = { pendingDelete = instance },
            )
        }
    }

    pendingDelete?.let { instance ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_instance_title)) },
            text = { Text(stringResource(R.string.delete_instance_message, instance.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            onDeleteInstance(instance)
                            pendingDelete = null
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
