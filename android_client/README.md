# Sense HAT Android Client

Native Android client for the Raspberry Pi Sense HAT dashboard. It mirrors the current web client with a light gradient background, white dashboard cards, blue accent controls, two configurable sensor sources, manual refresh, auto-refresh, compass heading, and a settings section.

## What the app does

- Reads the same API routes used by the web client:
  - `/api/get_temperature`
  - `/api/get_humidity`
  - `/api/get_pressure`
  - `/api/get_north`
- Displays two Sense HAT sources.
- Shows temperature, humidity, pressure, north heading, compass direction, source health, and last update time.
- Lets users configure source labels, API base URLs, refresh interval, and API timeout.
- Stores settings locally with Android `SharedPreferences`.

## Build and run

1. Open `android_client` in Android Studio.
2. Let Android Studio sync the Gradle project.
3. If prompted, install the Android SDK platform for `compileSdk 35`.
4. Run the `app` configuration on an emulator or Android device.

Command-line build, if Gradle is installed or Android Studio has generated a wrapper:

```powershell
cd android_client
gradle :app:assembleDebug
```

The debug APK will be created under `android_client/app/build/outputs/apk/debug/`.

## Configuration

Default API sources are defined in `MainActivity.java`:

```java
private static final String DEFAULT_URL_A = "https://sensors.example.com/sensehat-a";
private static final String DEFAULT_URL_B = "https://sensors.example.com/sensehat-b";
```

Users can also edit the API URLs from the in-app settings section. The app allows cleartext HTTP traffic so it can connect to local Raspberry Pi or LAN test servers during development.

## Created files

- `settings.gradle` - Android project settings.
- `build.gradle` - root Android Gradle plugin declaration.
- `app/build.gradle` - app module configuration.
- `app/src/main/AndroidManifest.xml` - app manifest and internet permission.
- `app/src/main/res/values/colors.xml` - palette matching the web client.
- `app/src/main/res/values/strings.xml` - app name.
- `app/src/main/res/values/styles.xml` - no-action-bar Android theme.
- `app/src/main/java/com/example/sensehatclient/MainActivity.java` - native UI, settings, polling, and compass view.
- `app/src/main/java/com/example/sensehatclient/SensorApiClient.java` - API calls and numeric response parsing.
- `app/src/main/java/com/example/sensehatclient/SensorSource.java` - source configuration model.
- `app/src/main/java/com/example/sensehatclient/SensorReading.java` - reading model, heading normalization, and direction labels.

## Notes for reuse

Before publishing your own fork, update the package name, launcher icon, project text, repository links, and default API URLs.
