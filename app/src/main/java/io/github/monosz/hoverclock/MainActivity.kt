package io.github.monosz.hoverclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HourglassFull
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.monosz.hoverclock.data.SettingsRepository
import io.github.monosz.hoverclock.model.TimeMode
import io.github.monosz.hoverclock.service.OverlayService
import io.github.monosz.hoverclock.service.OverlayService.Companion.activeInstanceIds
import io.github.monosz.hoverclock.ui.about.AboutSheet
import io.github.monosz.hoverclock.ui.mode.ModeInstancesScreen
import io.github.monosz.hoverclock.ui.navigation.AppScreen
import io.github.monosz.hoverclock.ui.navigation.MainTab
import io.github.monosz.hoverclock.ui.settings.ClockSettingsScreen
import io.github.monosz.hoverclock.ui.settings.StopwatchSettingsScreen
import io.github.monosz.hoverclock.ui.settings.TimerSettingsScreen
import io.github.monosz.hoverclock.ui.theme.HoverClockTheme
import io.github.monosz.hoverclock.util.rememberOverlayLaunchHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    private val settingsRepository: SettingsRepository by lazy {
        (application as MainApplication).settingsRepository
    }

    private val startupReady = AtomicBoolean(false)
    private val initialTab = MutableStateFlow<MainTab?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !startupReady.get() }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val tab = settingsRepository.awaitMainScreenReady()
            initialTab.value = tab
            startupReady.set(true)
        }

        setContent {
            val preloadedTab by initialTab.collectAsStateWithLifecycle()
            val startupTab = preloadedTab ?: return@setContent

            HoverClockTheme {
                val selectedTab by settingsRepository.lastSelectedTab().collectAsStateWithLifecycle(
                    initialValue = startupTab,
                )
                var configureInstanceId by rememberSaveable { mutableStateOf<String?>(null) }
                var configureTab by rememberSaveable { mutableStateOf<MainTab?>(null) }
                var showStopAllDialog by rememberSaveable { mutableStateOf(false) }
                var showAboutSheet by rememberSaveable { mutableStateOf(false) }
                val launchOverlay = rememberOverlayLaunchHandler(this)
                val scope = rememberCoroutineScope()
                val activeInstanceIds by OverlayService.activeInstanceIds.collectAsStateWithLifecycle()
                val allInstances by settingsRepository.allInstances().collectAsStateWithLifecycle(
                    initialValue = emptyList(),
                )

                val configureScreen =
                    remember(configureInstanceId, configureTab) {
                        if (configureInstanceId != null && configureTab != null) {
                            AppScreen.Configure(tab = configureTab!!, instanceId = configureInstanceId!!)
                        } else {
                            null
                        }
                    }

                val configureInstanceName =
                    configureScreen?.let { screen ->
                        allInstances.firstOrNull { it.id == screen.instanceId }?.name
                    }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    when {
                                        configureScreen != null ->
                                            configureInstanceName
                                                ?: stringResource(R.string.action_configure)
                                        else -> "Hover${when (selectedTab) {
                                            MainTab.Clock -> stringResource(R.string.tab_clock)
                                            MainTab.Stopwatch -> stringResource(R.string.tab_stopwatch)
                                            MainTab.Timer -> stringResource(R.string.tab_timer)
                                        }}"
                                    },
                                )
                            },
                            navigationIcon = {
                                if (configureScreen != null) {
                                    IconButton(
                                        onClick = {
                                            configureInstanceId = null
                                            configureTab = null
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.action_back),
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (configureScreen == null) {
                                    if (activeInstanceIds.isNotEmpty()) {
                                        IconButton(onClick = { showStopAllDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Outlined.StopCircle,
                                                contentDescription = stringResource(R.string.action_stop_all_overlays),
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                    IconButton(onClick = { showAboutSheet = true }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = stringResource(R.string.action_about),
                                        )
                                    }
                                }
                            },
                        )
                    },
                    floatingActionButton = {
                        if (configureScreen == null) {
                            FloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        settingsRepository.createInstance(selectedTab.timeMode)
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.action_add_instance),
                                )
                            }
                        }
                    },
                    bottomBar = {
                        if (configureScreen == null) {
                            NavigationBar {
                                MainTab.entries.forEach { tab ->
                                    val selected = selectedTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { scope.launch { settingsRepository.setLastSelectedTab(tab) } },
                                        icon = {
                                            Icon(
                                                imageVector =
                                                    when (tab) {
                                                        MainTab.Clock ->
                                                            if (selected) {
                                                                Icons.Default.Schedule
                                                            } else {
                                                                Icons.Outlined.Schedule
                                                            }
                                                        MainTab.Stopwatch ->
                                                            if (selected) {
                                                                Icons.Default.HourglassFull
                                                            } else {
                                                                Icons.Outlined.HourglassEmpty
                                                            }
                                                        MainTab.Timer ->
                                                            if (selected) {
                                                                Icons.Default.Timer
                                                            } else {
                                                                Icons.Outlined.Timer
                                                            }
                                                    },
                                                contentDescription =
                                                    when (tab) {
                                                        MainTab.Clock -> stringResource(R.string.tab_clock)
                                                        MainTab.Stopwatch -> stringResource(R.string.tab_stopwatch)
                                                        MainTab.Timer -> stringResource(R.string.tab_timer)
                                                    },
                                            )
                                        },
                                        label = {
                                            Text(
                                                when (tab) {
                                                    MainTab.Clock -> stringResource(R.string.tab_clock)
                                                    MainTab.Stopwatch -> stringResource(R.string.tab_stopwatch)
                                                    MainTab.Timer -> stringResource(R.string.tab_timer)
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    when (val screen = configureScreen) {
                        null ->
                            ModeInstancesScreen(
                                mode = selectedTab.timeMode,
                                settingsRepository = settingsRepository,
                                modifier =
                                    Modifier
                                        .padding(innerPadding)
                                        .padding(bottom = 88.dp),
                                onLaunch = { instanceId ->
                                    launchOverlay(instanceId)
                                },
                                onStop = { instanceId ->
                                    OverlayService.stopInstance(this, instanceId)
                                },
                                onConfigure = { instance ->
                                    configureTab = MainTab.fromTimeMode(instance.mode)
                                    configureInstanceId = instance.id
                                },
                                onDeleteInstance = { instance ->
                                    if (OverlayService.isInstanceRunning(instance.id)) {
                                        OverlayService.stopInstance(this@MainActivity, instance.id)
                                    }
                                    settingsRepository.deleteInstance(instance.id)
                                },
                            )
                        is AppScreen.Configure ->
                            when (screen.tab.timeMode) {
                                TimeMode.Clock ->
                                    ClockSettingsScreen(
                                        instanceId = screen.instanceId,
                                        settingsRepository = settingsRepository,
                                        modifier = Modifier.padding(innerPadding),
                                    )
                                TimeMode.Stopwatch ->
                                    StopwatchSettingsScreen(
                                        instanceId = screen.instanceId,
                                        settingsRepository = settingsRepository,
                                        modifier = Modifier.padding(innerPadding),
                                    )
                                TimeMode.Timer ->
                                    TimerSettingsScreen(
                                        instanceId = screen.instanceId,
                                        settingsRepository = settingsRepository,
                                        modifier = Modifier.padding(innerPadding),
                                    )
                            }
                    }
                }

                if (showStopAllDialog) {
                    AlertDialog(
                        onDismissRequest = { showStopAllDialog = false },
                        title = { Text(stringResource(R.string.stop_all_dialog_title)) },
                        text = {
                            Text(
                                stringResource(
                                    R.string.stop_all_dialog_message,
                                    activeInstanceIds.size,
                                ),
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    OverlayService.stopAll(this@MainActivity)
                                    showStopAllDialog = false
                                },
                            ) {
                                Text(stringResource(R.string.notification_stop_all))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStopAllDialog = false }) {
                                Text(stringResource(android.R.string.cancel))
                            }
                        },
                    )
                }

                if (showAboutSheet) {
                    AboutSheet(onDismiss = { showAboutSheet = false })
                }
            }
        }
    }
}
