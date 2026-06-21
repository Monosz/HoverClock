package io.github.monosz.hoverclock.ui.navigation

sealed class AppScreen {
    data class Configure(val tab: MainTab, val instanceId: String) : AppScreen()
}
