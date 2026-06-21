# HoverClock

HoverClock is a lightweight, open-source Android app that provides floating time overlays — **Clock**, **Stopwatch**, and **Timer** — that stay visible over other apps.

## Features

- Floating overlay for Clock, Stopwatch, and Timer modes
- Drag to reposition (with optional lock)
- Material 3 / Material You dynamic theming
- Configurable appearance: transparency, font size, corner radius
- Per-mode settings persisted with DataStore
- No ads, analytics, accounts, or unnecessary permissions

## Architecture

```
ui/          → Compose screens (home, settings, overlay)
service/     → OverlayService (foreground service)
overlay/     → OverlayManager (WindowManager)
engine/      → TimeProvider implementations
model/       → TimeMode, TimeState, OverlaySettings
data/        → SettingsRepository (DataStore)
```

The overlay consumes `TimeState` from a `TimeProvider`, keeping it independent of the active mode.

## Permissions

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw overlay over other apps |
| `FOREGROUND_SERVICE` / `SPECIAL_USE` | Keep overlay alive in background |
| `POST_NOTIFICATIONS` | Foreground service notification (API 33+) |

## Building

```bash
./gradlew assembleDebug
```

## Usage

1. Open HoverClock and tap **Configure** to adjust mode settings.
2. Tap **Launch** to start the floating overlay.
3. Grant overlay permission when prompted.
4. Drag the overlay to reposition it.
5. For Stopwatch and Timer, tap the overlay to start/pause.
6. Stop the overlay from the persistent notification.
