package io.github.monosz.hoverclock.service

import io.github.monosz.hoverclock.engine.TimeProvider
import io.github.monosz.hoverclock.model.TimeMode
import io.github.monosz.hoverclock.overlay.OverlayManager
import kotlinx.coroutines.Job

internal class OverlaySession(
    val instanceId: String,
    val instanceName: String,
    val mode: TimeMode,
    val overlayManager: OverlayManager,
    val timeProvider: TimeProvider,
    val observerJobs: List<Job>,
)
