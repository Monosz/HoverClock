package io.github.monosz.hoverclock.model

data class OverlayInstance(
    val id: String,
    val mode: TimeMode,
    val name: String,
    val iconName: String = InstanceIcon.defaultFor(mode).name,
)
