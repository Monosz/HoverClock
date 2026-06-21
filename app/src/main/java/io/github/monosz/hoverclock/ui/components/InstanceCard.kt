package io.github.monosz.hoverclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.monosz.hoverclock.R
import io.github.monosz.hoverclock.model.InstanceIcon
import io.github.monosz.hoverclock.model.OverlayInstance
import io.github.monosz.hoverclock.model.TimeMode
import io.github.monosz.hoverclock.ui.theme.HoverClockTheme

@Composable
fun InstanceCard(
    instance: OverlayInstance,
    icon: ImageVector,
    isRunning: Boolean,
    summaryText: String,
    onToggle: (enabled: Boolean) -> Unit,
    onConfigure: () -> Unit,
    onDelete: () -> Unit,
    onIconClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(onClick = onIconClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(R.string.setting_instance_icon),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = instance.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isRunning,
                    onCheckedChange = onToggle,
                )
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_open_settings)) },
                        onClick = {
                            menuExpanded = false
                            onConfigure()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete_instance)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onConfigure)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewInstanceCard() {
    HoverClockTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InstanceCard(
                instance =
                    OverlayInstance(
                        id = "2",
                        name = "Clock 1",
                        mode = TimeMode.Clock,
                        iconName = InstanceIcon.Schedule.name,
                    ),
                icon = InstanceIcon.Schedule.imageVector(),
                isRunning = true,
                summaryText = "Large · 24h",
                onToggle = {},
                onConfigure = {},
                onDelete = {},
            )
            InstanceCard(
                instance =
                    OverlayInstance(
                        id = "1",
                        name = "Stopwatch 1",
                        mode = TimeMode.Stopwatch,
                        iconName = InstanceIcon.Hourglass.name,
                    ),
                icon = InstanceIcon.Hourglass.imageVector(),
                isRunning = false,
                summaryText = "Medium · Standard",
                onToggle = {},
                onConfigure = {},
                onDelete = {},
            )
            InstanceCard(
                instance =
                    OverlayInstance(
                        id = "3",
                        name = "Timer 1",
                        mode = TimeMode.Timer,
                        iconName = InstanceIcon.Timer.name,
                    ),
                icon = InstanceIcon.Timer.imageVector(),
                isRunning = false,
                summaryText = "Medium · 10:00",
                onToggle = {},
                onConfigure = {},
                onDelete = {},
            )
        }
    }
}
