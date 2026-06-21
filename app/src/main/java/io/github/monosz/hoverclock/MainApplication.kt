package io.github.monosz.hoverclock

import android.app.Application
import io.github.monosz.hoverclock.data.SettingsRepository

class MainApplication : Application() {
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
