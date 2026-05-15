# Chrona Android

Chrona is a native Android AI schedule and notification assistant.

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
- Stable long-input API parsing with local segmentation and output token limits.
- User confirmation before saving.
- Room local persistence.
- Local behavior event logging for create, complete, and delete actions.
- Local insights for completion rate, productive hour, overdue tasks, and suggestions.
- WorkManager reminder scheduling and cancellation.
- Compose UI with four companion scenes: home, AI chat, execution, and summary.

## Debug APK

After a successful build:

`app/build/outputs/apk/debug/app-debug.apk`

The build script also copies the current package to:

`../release/Chrona-debug.apk`

## Notes

- Android instrumentation tests require a connected device or emulator.
- API settings are stored locally on the device. If no API is configured, Chrona uses the local rule-based parser.
- Behavior insights are computed locally from Room data. API-based summaries should use compact statistics, not raw history.
- The project path may contain Chinese characters; Gradle is configured with `android.overridePathCheck=true` for this workspace.
