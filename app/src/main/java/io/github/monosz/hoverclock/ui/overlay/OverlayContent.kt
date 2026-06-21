package io.github.monosz.hoverclock.ui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.monosz.hoverclock.model.OverlaySettings
import io.github.monosz.hoverclock.model.TimeMode
import io.github.monosz.hoverclock.model.TimeState
import io.github.monosz.hoverclock.util.toComposeColor

@Composable
fun OverlayContent(
    timeState: TimeState,
    settings: OverlaySettings,
    modifier: Modifier = Modifier,
) {
    val baseColorArgb =
        when {
            timeState.mode == TimeMode.Clock -> settings.runningBackgroundColor
            timeState.isRunning -> settings.runningBackgroundColor
            else -> settings.pausedBackgroundColor
        }
    val backgroundColor = baseColorArgb.toComposeColor().copy(alpha = settings.backgroundAlpha)
    val textColor = settings.textColor.toComposeColor()

    Surface(
        modifier =
            modifier.semantics {
                contentDescription = "${timeState.mode.name}: ${timeState.formattedText}"
            },
        shape = RoundedCornerShape(settings.cornerRadiusDp.dp),
        color = backgroundColor,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = timeState.formattedText,
                color = textColor,
                fontSize = settings.fontSizeSp.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
