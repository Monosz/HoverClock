# HoverClock

A lightweight Android app for floating Clock, Stopwatch, and Timer overlays.

> [!IMPORTANT]
> HoverClock started as an AI-generated MVP to quickly validate the idea. While it's functional, the codebase is still being cleaned up. Expect bugs and breaking changes until a stable release if you do decide to use it.

## Features

* Floating Clock, Stopwatch, and Timer overlays that stay over other apps.
* Launch multiple floating overlays at the same time.
* Drag to reposition, with an optional lock.
* Customizable transparency, font size, and corner radius.
* Material You dynamic theming.
* Per-mode settings saved with DataStore.
* No ads, analytics, or unnecessary permissions.

## Permissions

* **Display over other apps** – Required to show floating overlays.
* **Foreground Service** – Keeps overlays running in the background.
* **Notifications** – Required for the foreground service notification (Android 13+).

## Building

```bash
./gradlew assembleDebug
```

## Usage

1. Open the app and tap **Configure** to change settings.
2. Tap **Launch** to start one or more floating overlays.
3. Grant the overlay permission if prompted.
4. Drag overlays to reposition them.
5. Tap a Stopwatch or Timer overlay to start or pause it.
6. Stop overlays from the persistent notification.

## Architecture

* `ui/` – Compose screens.
* `service/` – Foreground service.
* `overlay/` – Window management.
* `engine/` – Time providers and overlay logic.
* `model/` – Data models.
* `data/` – DataStore-backed settings.
