package io.github.monosz.hoverclock.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.monosz.hoverclock.R
import io.github.monosz.hoverclock.data.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun InstanceNameField(
    instanceId: String,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val instances by settingsRepository.allInstances().collectAsStateWithLifecycle(initialValue = emptyList())
    val currentName = instances.firstOrNull { it.id == instanceId }?.name.orEmpty()
    var text by remember(instanceId, currentName) { mutableStateOf(currentName) }

    fun saveName() {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty() && trimmed != currentName) {
            scope.launch { settingsRepository.renameInstance(instanceId, trimmed) }
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) saveName()
                },
        label = { androidx.compose.material3.Text(stringResource(R.string.setting_name)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { saveName() }),
    )
}
