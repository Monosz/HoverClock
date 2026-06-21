package io.github.monosz.hoverclock.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import io.github.monosz.hoverclock.service.OverlayService

fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

fun overlayPermissionIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    )

@Composable
fun rememberOverlayLaunchHandler(context: Context): (String) -> Unit {
    var pendingInstanceId by remember { mutableStateOf<String?>(null) }

    fun launchService(instanceId: String) {
        OverlayService.start(context, instanceId)
        pendingInstanceId = null
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _ ->
            pendingInstanceId?.let(::launchService)
        }

    val overlaySettingsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            pendingInstanceId?.let { instanceId ->
                if (canDrawOverlays(context)) {
                    requestNotificationThenLaunch(context, notificationPermissionLauncher) {
                        launchService(instanceId)
                    }
                } else {
                    pendingInstanceId = null
                }
            }
        }

    return remember(context) {
        { instanceId: String ->
            pendingInstanceId = instanceId
            if (!canDrawOverlays(context)) {
                overlaySettingsLauncher.launch(overlayPermissionIntent(context))
            } else {
                requestNotificationThenLaunch(context, notificationPermissionLauncher) {
                    launchService(instanceId)
                }
            }
        }
    }
}

private fun requestNotificationThenLaunch(
    context: Context,
    notificationPermissionLauncher: ActivityResultLauncher<String>,
    launch: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
    }
    launch()
}
