# Chrona Android

Chrona is a native Android MVP for an AI schedule and notification assistant.

## Build

From PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File '../scripts/build-android.ps1'
```

The script installs Gradle, Android SDK command-line tools, and a project-local JDK under `../.tools` when they are missing or when the system Java is incompatible.

## Current MVP

- One-line Chinese schedule input.
- Rule-based local parsing.
- Optional user-provided OpenAI-compatible API settings with local parser fallback.
- User confirmation before saving.
- Room local persistence.
- WorkManager reminder scheduling and cancellation.
- Compose UI using split Chrona character assets.

## Debug APK

After a successful build:

`app/build/outputs/apk/debug/app-debug.apk`

## Notes

- Android instrumentation tests require a connected device or emulator.
- API settings are stored locally on the device. If no API is configured, Chrona uses the local rule-based parser.
- The project path may contain Chinese characters; Gradle is configured with `android.overridePathCheck=true` for this workspace.
