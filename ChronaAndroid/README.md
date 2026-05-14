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
- User confirmation before saving.
- Room local persistence.
- WorkManager reminder scheduling and cancellation.
- Compose UI using the provided Chrona character asset.

## Debug APK

After a successful build:

`app/build/outputs/apk/debug/app-debug.apk`

## Notes

- Android instrumentation tests require a connected device or emulator.
- The project path may contain Chinese characters; Gradle is configured with `android.overridePathCheck=true` for this workspace.
