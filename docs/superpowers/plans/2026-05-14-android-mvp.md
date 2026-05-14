# AI 智能日程助手 Android MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable native Android MVP for Chrona that parses one-line Chinese schedule input, lets the user confirm tasks, saves them locally, and schedules basic reminders.

**Architecture:** Create a single Android app module under `ChronaAndroid/`. Keep parsing, persistence, reminder scheduling, and UI in small focused files with repository boundaries. Use a local rule-based parser behind a `TaskParser` interface so a future LLM parser can replace it without changing the UI.

**Tech Stack:** Kotlin, Jetpack Compose, Room, WorkManager, Kotlin coroutines, JUnit, AndroidX test, project-local PowerShell setup scripts.

---

## File Structure

- Create: `C:/Users/suifeng/Desktop/日历/.gitignore`  
  Ignore build outputs, project-local tools, Android local properties, and visual brainstorming cache.
- Create: `C:/Users/suifeng/Desktop/日历/scripts/setup-android.ps1`  
  Detect or install JDK, Gradle, Android command-line tools, platform SDK, and build tools into `C:/Users/suifeng/Desktop/日历/.tools`.
- Create: `C:/Users/suifeng/Desktop/日历/scripts/build-android.ps1`  
  Run setup, export environment variables for this process, and build the debug APK.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/settings.gradle.kts`  
  Define plugin management and include `:app`.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/build.gradle.kts`  
  Top-level Gradle plugin versions.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/build.gradle.kts`  
  Android app dependencies and test configuration.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/AndroidManifest.xml`  
  Activity, notification permission, and WorkManager notification metadata.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/res/values/styles.xml`  
  App-level light theme referenced by the manifest.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/MainActivity.kt`  
  App entry, notification permission request, Room/repository wiring.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleTask.kt`  
  Room entity and task status enum.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleTaskDao.kt`  
  DAO for insert, observe, update status, and delete.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/AppDatabase.kt`  
  Room database singleton.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleRepository.kt`  
  Repository that persists tasks and schedules reminders.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/parser/TaskParser.kt`  
  Parser contract and parsed task model.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/parser/RuleBasedTaskParser.kt`  
  Deterministic Chinese schedule parser.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/reminder/ReminderScheduler.kt`  
  Reminder scheduling interface and WorkManager implementation.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/reminder/ReminderWorker.kt`  
  Notification creation worker.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaApp.kt`  
  Screen state and high-level Compose app.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaTheme.kt`  
  Compose Material theme.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/res/drawable/chrona_character.png`  
  Copy of the provided blue time-planning character image.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/test/java/com/chrona/ai/parser/RuleBasedTaskParserTest.kt`  
  JVM parser tests.
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/androidTest/java/com/chrona/ai/data/ScheduleTaskDaoTest.kt`  
  Room DAO instrumentation tests.

---

### Task 1: Project-Local Tooling And Android Skeleton

**Files:**
- Create: `C:/Users/suifeng/Desktop/日历/.gitignore`
- Create: `C:/Users/suifeng/Desktop/日历/scripts/setup-android.ps1`
- Create: `C:/Users/suifeng/Desktop/日历/scripts/build-android.ps1`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/settings.gradle.kts`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/build.gradle.kts`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/build.gradle.kts`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/AndroidManifest.xml`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/res/values/styles.xml`

- [ ] **Step 1: Write repository ignore rules**

Create `C:/Users/suifeng/Desktop/日历/.gitignore`:

```gitignore
.tools/
.superpowers/
ChronaAndroid/.gradle/
ChronaAndroid/build/
ChronaAndroid/app/build/
ChronaAndroid/local.properties
*.iml
.idea/
```

- [ ] **Step 2: Write the setup script**

Create `C:/Users/suifeng/Desktop/日历/scripts/setup-android.ps1`:

```powershell
param(
  [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'
$ToolsDir = Join-Path $ProjectRoot '.tools'
$GradleDir = Join-Path $ToolsDir 'gradle-8.7'
$AndroidSdkDir = Join-Path $ToolsDir 'android-sdk'
$CmdlineToolsDir = Join-Path $AndroidSdkDir 'cmdline-tools'
$CmdlineToolsLatestDir = Join-Path $CmdlineToolsDir 'latest'
$JdkDir = Join-Path $ToolsDir 'jdk-17'

New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
New-Item -ItemType Directory -Force -Path $AndroidSdkDir | Out-Null

function Get-CommandPath([string]$Name) {
  $command = Get-Command $Name -ErrorAction SilentlyContinue
  if ($null -eq $command) { return $null }
  return $command.Source
}

function Expand-Zip([string]$ZipPath, [string]$Destination) {
  if (Test-Path -LiteralPath $Destination) {
    Remove-Item -LiteralPath $Destination -Recurse -Force
  }
  New-Item -ItemType Directory -Force -Path $Destination | Out-Null
  Expand-Archive -LiteralPath $ZipPath -DestinationPath $Destination -Force
}

if (-not (Test-Path -LiteralPath $GradleDir)) {
  $zip = Join-Path $ToolsDir 'gradle-8.7-bin.zip'
  Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.7-bin.zip' -OutFile $zip
  Expand-Zip -ZipPath $zip -Destination $ToolsDir
}

if (-not (Test-Path -LiteralPath (Join-Path $JdkDir 'bin/java.exe'))) {
  $javaPath = Get-CommandPath 'java'
  if ($null -eq $javaPath) {
    $zip = Join-Path $ToolsDir 'jdk-17.zip'
    $temp = Join-Path $ToolsDir 'jdk-17-temp'
    Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile $zip
    Expand-Zip -ZipPath $zip -Destination $temp
    $expandedJdk = Get-ChildItem -LiteralPath $temp -Directory | Select-Object -First 1
    if ($null -eq $expandedJdk) { throw 'Downloaded JDK archive did not contain a directory.' }
    Move-Item -LiteralPath $expandedJdk.FullName -Destination $JdkDir -Force
    Remove-Item -LiteralPath $temp -Recurse -Force
  }
}

if (Test-Path -LiteralPath (Join-Path $JdkDir 'bin/java.exe')) {
  $env:JAVA_HOME = $JdkDir
  $env:Path = "$(Join-Path $JdkDir 'bin');$env:Path"
}

if (-not (Test-Path -LiteralPath $CmdlineToolsLatestDir)) {
  $zip = Join-Path $ToolsDir 'commandlinetools-win.zip'
  Invoke-WebRequest -Uri 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' -OutFile $zip
  $temp = Join-Path $ToolsDir 'cmdline-tools-temp'
  Expand-Zip -ZipPath $zip -Destination $temp
  New-Item -ItemType Directory -Force -Path $CmdlineToolsDir | Out-Null
  Move-Item -LiteralPath (Join-Path $temp 'cmdline-tools') -Destination $CmdlineToolsLatestDir -Force
  Remove-Item -LiteralPath $temp -Recurse -Force
}

$sdkManager = Join-Path $CmdlineToolsLatestDir 'bin/sdkmanager.bat'
if (-not (Test-Path -LiteralPath $sdkManager)) {
  throw "Android sdkmanager was not found at $sdkManager"
}

$env:ANDROID_HOME = $AndroidSdkDir
$env:ANDROID_SDK_ROOT = $AndroidSdkDir

$licenseInput = [string]::Join([Environment]::NewLine, (1..40 | ForEach-Object { 'y' })) + [Environment]::NewLine
$licenseInput | & $sdkManager --sdk_root=$AndroidSdkDir --licenses | Out-Host
& $sdkManager --sdk_root=$AndroidSdkDir 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0' | Out-Host

$javaPath = Get-CommandPath 'java'
if ($null -eq $javaPath) {
  Write-Host 'No java command found on PATH. Install JDK 17 manually or place it in .tools/jdk-17, then rerun this script.'
} else {
  Write-Host "Using Java from $javaPath"
}

$localProperties = Join-Path $ProjectRoot 'ChronaAndroid/local.properties'
New-Item -ItemType Directory -Force -Path (Split-Path $localProperties) | Out-Null
"sdk.dir=$($AndroidSdkDir -replace '\\','/')" | Set-Content -LiteralPath $localProperties -Encoding UTF8

Write-Host "Gradle: $GradleDir"
Write-Host "Android SDK: $AndroidSdkDir"
Write-Host "Setup complete."
```

- [ ] **Step 3: Write the build script**

Create `C:/Users/suifeng/Desktop/日历/scripts/build-android.ps1`:

```powershell
param(
  [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'setup-android.ps1') -ProjectRoot $ProjectRoot

$GradleBat = Join-Path $ProjectRoot '.tools/gradle-8.7/bin/gradle.bat'
$AndroidSdkDir = Join-Path $ProjectRoot '.tools/android-sdk'
$JdkDir = Join-Path $ProjectRoot '.tools/jdk-17'
$env:ANDROID_HOME = $AndroidSdkDir
$env:ANDROID_SDK_ROOT = $AndroidSdkDir
if (Test-Path -LiteralPath (Join-Path $JdkDir 'bin/java.exe')) {
  $env:JAVA_HOME = $JdkDir
  $env:Path = "$(Join-Path $JdkDir 'bin');$env:Path"
}

Push-Location (Join-Path $ProjectRoot 'ChronaAndroid')
try {
  & $GradleBat ':app:assembleDebug' ':app:testDebugUnitTest'
} finally {
  Pop-Location
}
```

- [ ] **Step 4: Create Gradle settings**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ChronaAndroid"
include(":app")
```

- [ ] **Step 5: Create top-level Gradle build**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
```

- [ ] **Step 6: Create app Gradle build**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.chrona.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chrona.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

- [ ] **Step 7: Create the Android manifest**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:label="Chrona"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 8: Add app theme style**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/res/values/styles.xml`:

```xml
<resources>
    <style name="AppTheme" parent="android:style/Theme.Material.Light.NoActionBar">
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:statusBarColor">#F8FAFC</item>
        <item name="android:navigationBarColor">#FFFFFF</item>
    </style>
</resources>
```

- [ ] **Step 9: Run a skeleton Gradle check**

Run:

```powershell
& 'C:/Users/suifeng/Desktop/日历/scripts/setup-android.ps1'
& 'C:/Users/suifeng/Desktop/日历/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/ChronaAndroid' tasks
```

Expected: Gradle lists Android tasks. If Java is missing, install a JDK into the project-local `.tools/jdk-17` or use the existing system JDK and rerun.

- [ ] **Step 10: Commit tooling and skeleton**

```powershell
git add .gitignore scripts ChronaAndroid/settings.gradle.kts ChronaAndroid/build.gradle.kts ChronaAndroid/app/build.gradle.kts ChronaAndroid/app/src/main/AndroidManifest.xml ChronaAndroid/app/src/main/res/values/styles.xml
git commit -m "chore: scaffold android project tooling"
```

---

### Task 2: Parser Contract And Rule-Based Parser

**Files:**
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/parser/TaskParser.kt`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/parser/RuleBasedTaskParser.kt`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/test/java/com/chrona/ai/parser/RuleBasedTaskParserTest.kt`

- [ ] **Step 1: Write the failing parser tests**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/test/java/com/chrona/ai/parser/RuleBasedTaskParserTest.kt`:

```kotlin
package com.chrona.ai.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class RuleBasedTaskParserTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val clock = Clock.fixed(Instant.parse("2026-05-14T02:00:00Z"), zone)
    private val parser = RuleBasedTaskParser(clock, zone)

    @Test
    fun parsesThreeChineseTasksWithDefaultTimes() {
        val result = parser.parse("明天提醒我拿快递，晚上健身，周末写报告")

        assertEquals(3, result.size)
        assertEquals("拿快递", result[0].title)
        assertEquals(LocalDateTime.of(2026, 5, 15, 15, 0), result[0].startAt)
        assertEquals("健身", result[1].title)
        assertEquals(LocalDateTime.of(2026, 5, 14, 20, 0), result[1].startAt)
        assertEquals("写报告", result[2].title)
        assertEquals(LocalDateTime.of(2026, 5, 16, 14, 0), result[2].startAt)
    }

    @Test
    fun parsesExplicitAfternoonHour() {
        val result = parser.parse("明天下午3点开会")

        assertEquals(1, result.size)
        assertEquals("开会", result.first().title)
        assertEquals(LocalDateTime.of(2026, 5, 15, 15, 0), result.first().startAt)
    }

    @Test
    fun keepsTaskWithoutTimeAsPendingConfirmation() {
        val result = parser.parse("整理书桌")

        assertEquals(1, result.size)
        assertEquals("整理书桌", result.first().title)
        assertEquals(null, result.first().startAt)
        assertTrue(result.first().needsTimeConfirmation)
        assertNotNull(result.first().confidenceNote)
    }
}
```

- [ ] **Step 2: Run parser tests to verify they fail**

Run:

```powershell
& 'C:/Users/suifeng/Desktop/日历/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/ChronaAndroid' ':app:testDebugUnitTest' --tests 'com.chrona.ai.parser.RuleBasedTaskParserTest'
```

Expected: FAIL because `TaskParser` and `RuleBasedTaskParser` do not exist.

- [ ] **Step 3: Add parser contract**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/parser/TaskParser.kt`:

```kotlin
package com.chrona.ai.parser

import java.time.LocalDateTime

interface TaskParser {
    fun parse(input: String): List<ParsedTask>
}

data class ParsedTask(
    val title: String,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val sourceText: String,
    val confidenceNote: String,
    val needsTimeConfirmation: Boolean
)
```

- [ ] **Step 4: Add rule-based parser implementation**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/parser/RuleBasedTaskParser.kt`:

```kotlin
package com.chrona.ai.parser

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class RuleBasedTaskParser(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : TaskParser {
    override fun parse(input: String): List<ParsedTask> {
        return input
            .split(Regex("[，,、；;\\n]|然后"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { parseSegment(it) }
    }

    private fun parseSegment(segment: String): ParsedTask {
        val today = LocalDate.now(clock.withZone(zoneId))
        val date = detectDate(segment, today)
        val time = detectTime(segment)
        val title = cleanupTitle(segment).ifBlank { segment }
        val defaultTime = time ?: defaultTimeFor(title, segment)
        val startAt = if (date != null && defaultTime != null) LocalDateTime.of(date, defaultTime) else null
        val endAt = startAt?.plusHours(if (title.contains("报告") || title.contains("写")) 2 else 1)

        val confidence = when {
            time != null -> "识别到明确时间"
            startAt != null -> "根据事项类型使用默认时间"
            else -> "未识别日期或时间，加入前需要确认"
        }

        return ParsedTask(
            title = title,
            startAt = startAt,
            endAt = endAt,
            sourceText = segment,
            confidenceNote = confidence,
            needsTimeConfirmation = startAt == null
        )
    }

    private fun detectDate(text: String, today: LocalDate): LocalDate? {
        return when {
            text.contains("后天") -> today.plusDays(2)
            text.contains("明天") -> today.plusDays(1)
            text.contains("今天") || text.contains("今晚") || text.contains("晚上") -> today
            text.contains("周六") || text.contains("星期六") -> nextOrSame(today, DayOfWeek.SATURDAY)
            text.contains("周日") || text.contains("星期日") || text.contains("星期天") -> nextOrSame(today, DayOfWeek.SUNDAY)
            text.contains("周末") -> nextOrSame(today, DayOfWeek.SATURDAY)
            else -> null
        }
    }

    private fun nextOrSame(today: LocalDate, day: DayOfWeek): LocalDate {
        return today.with(TemporalAdjusters.nextOrSame(day))
    }

    private fun detectTime(text: String): LocalTime? {
        Regex("(\\d{1,2})[:：](\\d{2})").find(text)?.let {
            return LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt())
        }

        Regex("(早上|上午|中午|下午|晚上|今晚)?\\s*(\\d{1,2})\\s*点").find(text)?.let {
            val period = it.groupValues[1]
            var hour = it.groupValues[2].toInt()
            if ((period == "下午" || period == "晚上" || period == "今晚") && hour < 12) hour += 12
            if (period == "中午" && hour < 11) hour += 12
            return LocalTime.of(hour.coerceIn(0, 23), 0)
        }

        return null
    }

    private fun defaultTimeFor(title: String, source: String): LocalTime? {
        return when {
            source.contains("早上") || source.contains("上午") -> LocalTime.of(9, 0)
            source.contains("中午") -> LocalTime.of(12, 0)
            source.contains("下午") -> LocalTime.of(15, 0)
            source.contains("晚上") || source.contains("今晚") || title.contains("健身") -> LocalTime.of(20, 0)
            title.contains("快递") -> LocalTime.of(15, 0)
            title.contains("报告") || title.contains("写") -> LocalTime.of(14, 0)
            else -> null
        }
    }

    private fun cleanupTitle(text: String): String {
        return text
            .replace(Regex("提醒我|帮我|记得|今天|明天|后天|周末|周六|周日|星期六|星期日|星期天"), "")
            .replace(Regex("早上|上午|中午|下午|晚上|今晚|\\d{1,2}[:：]\\d{2}|\\d{1,2}\\s*点"), "")
            .trim()
    }
}
```

- [ ] **Step 5: Run parser tests to verify they pass**

Run:

```powershell
& 'C:/Users/suifeng/Desktop/日历/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/ChronaAndroid' ':app:testDebugUnitTest' --tests 'com.chrona.ai.parser.RuleBasedTaskParserTest'
```

Expected: PASS.

- [ ] **Step 6: Commit parser**

```powershell
git add ChronaAndroid/app/src/main/java/com/chrona/ai/parser ChronaAndroid/app/src/test/java/com/chrona/ai/parser
git commit -m "feat: add rule based schedule parser"
```

---

### Task 3: Room Data Layer And Repository

**Files:**
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleTask.kt`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleTaskDao.kt`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/AppDatabase.kt`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleRepository.kt`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/reminder/ReminderScheduler.kt`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/androidTest/java/com/chrona/ai/data/ScheduleTaskDaoTest.kt`

- [ ] **Step 1: Write DAO instrumentation test**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/androidTest/java/com/chrona/ai/data/ScheduleTaskDaoTest.kt`:

```kotlin
package com.chrona.ai.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleTaskDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ScheduleTaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.scheduleTaskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObservePendingTasks() = runTest {
        dao.insert(
            ScheduleTask(
                title = "拿快递",
                note = "根据事项类型使用默认时间",
                startAt = 1_779_433_200_000,
                endAt = 1_779_436_800_000,
                status = TaskStatus.PENDING,
                sourceText = "明天提醒我拿快递",
                createdAt = 1_779_300_000_000,
                updatedAt = 1_779_300_000_000
            )
        )

        val tasks = dao.observeActiveTasks().first()

        assertEquals(1, tasks.size)
        assertEquals("拿快递", tasks.first().title)
        assertEquals(TaskStatus.PENDING, tasks.first().status)
    }
}
```

- [ ] **Step 2: Run DAO test to verify it fails**

Run:

```powershell
& 'C:/Users/suifeng/Desktop/日历/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/ChronaAndroid' ':app:connectedDebugAndroidTest' --tests 'com.chrona.ai.data.ScheduleTaskDaoTest'
```

Expected: FAIL because data classes and database do not exist, or SKIPPED if no Android device/emulator is attached. If skipped, continue and verify with compile in later tasks.

- [ ] **Step 3: Add Room entity**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleTask.kt`:

```kotlin
package com.chrona.ai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_tasks")
data class ScheduleTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String?,
    val startAt: Long?,
    val endAt: Long?,
    val status: TaskStatus,
    val sourceText: String,
    val createdAt: Long,
    val updatedAt: Long
)

enum class TaskStatus {
    PENDING,
    DONE,
    DELETED
}
```

- [ ] **Step 4: Add DAO**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleTaskDao.kt`:

```kotlin
package com.chrona.ai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleTaskDao {
    @Insert
    suspend fun insert(task: ScheduleTask): Long

    @Query("SELECT * FROM schedule_tasks WHERE status != 'DELETED' ORDER BY COALESCE(startAt, createdAt) ASC")
    fun observeActiveTasks(): Flow<List<ScheduleTask>>

    @Query("UPDATE schedule_tasks SET status = :status, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateStatus(taskId: Long, status: TaskStatus, updatedAt: Long)

    @Query("SELECT * FROM schedule_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getById(taskId: Long): ScheduleTask?
}
```

- [ ] **Step 5: Add Room database**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/AppDatabase.kt`:

```kotlin
package com.chrona.ai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScheduleTask::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleTaskDao(): ScheduleTaskDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chrona.db"
                ).build().also { instance = it }
            }
        }
    }
}
```

- [ ] **Step 6: Add repository**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/reminder/ReminderScheduler.kt`:

```kotlin
package com.chrona.ai.reminder

interface ReminderScheduler {
    fun schedule(taskId: Long, title: String, triggerAtMillis: Long)
}
```

- [ ] **Step 7: Add repository**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleRepository.kt`:

```kotlin
package com.chrona.ai.data

import com.chrona.ai.parser.ParsedTask
import com.chrona.ai.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId

class ScheduleRepository(
    private val dao: ScheduleTaskDao,
    private val reminderScheduler: ReminderScheduler,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun observeTasks(): Flow<List<ScheduleTask>> = dao.observeActiveTasks()

    suspend fun addParsedTask(parsedTask: ParsedTask): Long {
        val now = System.currentTimeMillis()
        val startAtMillis = parsedTask.startAt?.atZone(zoneId)?.toInstant()?.toEpochMilli()
        val endAtMillis = parsedTask.endAt?.atZone(zoneId)?.toInstant()?.toEpochMilli()
        val id = dao.insert(
            ScheduleTask(
                title = parsedTask.title,
                note = parsedTask.confidenceNote,
                startAt = startAtMillis,
                endAt = endAtMillis,
                status = TaskStatus.PENDING,
                sourceText = parsedTask.sourceText,
                createdAt = now,
                updatedAt = now
            )
        )
        if (startAtMillis != null) {
            reminderScheduler.schedule(id, parsedTask.title, startAtMillis)
        }
        return id
    }

    suspend fun markDone(taskId: Long) {
        dao.updateStatus(taskId, TaskStatus.DONE, System.currentTimeMillis())
    }

    suspend fun delete(taskId: Long) {
        dao.updateStatus(taskId, TaskStatus.DELETED, System.currentTimeMillis())
    }
}
```

- [ ] **Step 8: Run data compile and available tests**

Run:

```powershell
& 'C:/Users/suifeng/Desktop/日历/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/ChronaAndroid' ':app:compileDebugKotlin' ':app:testDebugUnitTest'
```

Expected: PASS.

- [ ] **Step 9: Commit data layer**

```powershell
git add ChronaAndroid/app/src/main/java/com/chrona/ai/data ChronaAndroid/app/src/main/java/com/chrona/ai/reminder/ReminderScheduler.kt ChronaAndroid/app/src/androidTest/java/com/chrona/ai/data
git commit -m "feat: add local schedule persistence"
```

---

### Task 4: Reminder Scheduling And Notifications

**Files:**
- Modify: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/reminder/ReminderScheduler.kt`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/reminder/ReminderWorker.kt`
- Modify: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add WorkManager reminder scheduler**

Replace `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/reminder/ReminderScheduler.kt` with:

```kotlin
package com.chrona.ai.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.math.max

interface ReminderScheduler {
    fun schedule(taskId: Long, title: String, triggerAtMillis: Long)
}

class WorkManagerReminderScheduler(private val context: Context) : ReminderScheduler {
    override fun schedule(taskId: Long, title: String, triggerAtMillis: Long) {
        val delayMillis = max(0, triggerAtMillis - System.currentTimeMillis())
        val data = Data.Builder()
            .putLong(ReminderWorker.KEY_TASK_ID, taskId)
            .putString(ReminderWorker.KEY_TITLE, title)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder-$taskId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
```

- [ ] **Step 2: Add notification worker**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/reminder/ReminderWorker.kt`:

```kotlin
package com.chrona.ai.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chrona.ai.R

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, 0L)
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        ensureChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Chrona 提醒")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(taskId.toInt(), notification)
        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "日程提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "chrona-reminders"
        const val KEY_TASK_ID = "taskId"
        const val KEY_TITLE = "title"
    }
}
```

- [ ] **Step 3: Add notification icon resource**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/res/drawable/ic_notification.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#2563EB"
        android:pathData="M12,22a2.5,2.5 0,0 0,2.45 -2h-4.9A2.5,2.5 0,0 0,12 22zM18,16v-5a6,6 0,1 0,-12 0v5l-2,2v1h16v-1l-2,-2z" />
</vector>
```

- [ ] **Step 4: Add AndroidX core dependency**

Modify `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/build.gradle.kts` by adding this line inside `dependencies`:

```kotlin
implementation("androidx.core:core-ktx:1.13.1")
```

- [ ] **Step 5: Run compile**

Run:

```powershell
& 'C:/Users/suifeng/Desktop/日历/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/ChronaAndroid' ':app:compileDebugKotlin'
```

Expected: PASS.

- [ ] **Step 6: Commit reminders**

```powershell
git add ChronaAndroid/app/src/main/java/com/chrona/ai/reminder ChronaAndroid/app/src/main/res/drawable/ic_notification.xml ChronaAndroid/app/build.gradle.kts
git commit -m "feat: schedule basic task reminders"
```

---

### Task 5: Compose UI And Brand Asset

**Files:**
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/MainActivity.kt`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaApp.kt`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaTheme.kt`
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/res/drawable/chrona_character.png`

- [ ] **Step 1: Copy the character asset**

Run:

```powershell
New-Item -ItemType Directory -Force -Path 'C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/res/drawable' | Out-Null
Copy-Item -LiteralPath 'C:/Users/suifeng/Desktop/日历/AI智能日程助手_项目包/人物形象_蓝色时间规划少女.png' -Destination 'C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/res/drawable/chrona_character.png' -Force
```

Expected: `chrona_character.png` exists in the Android drawable resource folder.

- [ ] **Step 2: Add Compose theme**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaTheme.kt`:

```kotlin
package com.chrona.ai.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChronaColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondary = Color(0xFF0F766E),
    tertiary = Color(0xFFB45309),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onSurface = Color(0xFF0F172A)
)

@Composable
fun ChronaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChronaColors,
        content = content
    )
}
```

- [ ] **Step 3: Add MainActivity**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/MainActivity.kt`:

```kotlin
package com.chrona.ai

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.chrona.ai.data.AppDatabase
import com.chrona.ai.data.ScheduleRepository
import com.chrona.ai.parser.RuleBasedTaskParser
import com.chrona.ai.reminder.WorkManagerReminderScheduler
import com.chrona.ai.ui.ChronaApp
import com.chrona.ai.ui.ChronaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.get(this)
        val repository = ScheduleRepository(
            dao = database.scheduleTaskDao(),
            reminderScheduler = WorkManagerReminderScheduler(this)
        )
        val parser = RuleBasedTaskParser()

        setContent {
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = {}
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            ChronaTheme {
                ChronaApp(
                    parser = parser,
                    repository = repository
                )
            }
        }
    }
}
```

- [ ] **Step 4: Add Compose app screen**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaApp.kt`:

```kotlin
package com.chrona.ai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chrona.ai.R
import com.chrona.ai.data.ScheduleRepository
import com.chrona.ai.data.ScheduleTask
import com.chrona.ai.parser.ParsedTask
import com.chrona.ai.parser.TaskParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChronaApp(
    parser: TaskParser,
    repository: ScheduleRepository
) {
    val tasks by repository.observeTasks().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("明天提醒我拿快递，晚上健身，周末写报告") }
    var parsedTasks by remember { mutableStateOf<List<ParsedTask>>(emptyList()) }
    var message by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Header()
            }
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = input,
                    minLines = 3,
                    onValueChange = { input = it },
                    label = { Text("一句话安排日程") }
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        parsedTasks = parser.parse(input)
                        message = if (parsedTasks.isEmpty()) "没有识别到事项" else "已识别 ${parsedTasks.size} 个事项"
                    }) {
                        Text("解析")
                    }
                    Button(
                        enabled = parsedTasks.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                parsedTasks.forEach { repository.addParsedTask(it) }
                                message = "已加入日程"
                                parsedTasks = emptyList()
                            }
                        }
                    ) {
                        Text("确认加入")
                    }
                }
            }
            if (message.isNotBlank()) {
                item { Text(message, color = MaterialTheme.colorScheme.secondary) }
            }
            if (parsedTasks.isNotEmpty()) {
                item { SectionTitle("解析结果") }
                items(parsedTasks) { parsed ->
                    ParsedTaskCard(parsed)
                }
            }
            item { SectionTitle("近期日程") }
            if (tasks.isEmpty()) {
                item { EmptyState() }
            } else {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onDone = { scope.launch { repository.markDone(task.id) } },
                        onDelete = { scope.launch { repository.delete(task.id) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.chrona_character),
            contentDescription = "Chrona",
            modifier = Modifier.size(72.dp),
            contentScale = ContentScale.Crop
        )
        Column {
            Text("Chrona", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("让日程逐渐适应你的节奏", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ParsedTaskCard(task: ParsedTask) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(task.title, fontWeight = FontWeight.SemiBold)
            Text(task.startAt?.toString() ?: "时间待确认")
            Text(task.confidenceNote, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TaskCard(
    task: ScheduleTask,
    onDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(checked = false, onCheckedChange = { onDone() })
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.SemiBold)
                Text(formatMillis(task.startAt), style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onDelete) {
                Text("删除")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.chrona_character),
            contentDescription = null,
            modifier = Modifier.size(180.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(8.dp))
        Text("输入一句话，让 Chrona 先帮你整理今天。")
    }
}

private fun formatMillis(value: Long?): String {
    if (value == null) return "时间待确认"
    return SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(value))
}
```

- [ ] **Step 5: Run Compose compile**

Run:

```powershell
& 'C:/Users/suifeng/Desktop/日历/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/ChronaAndroid' ':app:compileDebugKotlin'
```

Expected: PASS.

- [ ] **Step 6: Commit UI**

```powershell
git add ChronaAndroid/app/src/main/java/com/chrona/ai/MainActivity.kt ChronaAndroid/app/src/main/java/com/chrona/ai/ui ChronaAndroid/app/src/main/res
git commit -m "feat: add chrona compose mvp ui"
```

---

### Task 6: Final Build Verification And README

**Files:**
- Create: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/README.md`
- Modify: `C:/Users/suifeng/Desktop/日历/docs/superpowers/plans/2026-05-14-android-mvp.md`

- [ ] **Step 1: Add Android README**

Create `C:/Users/suifeng/Desktop/日历/ChronaAndroid/README.md`:

````markdown
# Chrona Android

Chrona is a native Android MVP for an AI schedule and notification assistant.

## Build

From PowerShell:

```powershell
& '../scripts/build-android.ps1'
```

The script installs Gradle and Android SDK command-line tools under `../.tools` when they are missing.

## Current MVP

- One-line Chinese schedule input.
- Rule-based local parsing.
- User confirmation before saving.
- Room local persistence.
- WorkManager reminder scheduling.
- Compose UI using the provided Chrona character asset.

## Debug APK

After a successful build:

`app/build/outputs/apk/debug/app-debug.apk`
````

- [ ] **Step 2: Run unit tests**

Run:

```powershell
& 'C:/Users/suifeng/Desktop/日历/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/ChronaAndroid' ':app:testDebugUnitTest'
```

Expected: PASS.

- [ ] **Step 3: Build debug APK**

Run:

```powershell
& 'C:/Users/suifeng/Desktop/日历/scripts/build-android.ps1'
```

Expected: `C:/Users/suifeng/Desktop/日历/ChronaAndroid/app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 4: Record instrumentation status**

Run:

```powershell
& 'C:/Users/suifeng/Desktop/日历/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/ChronaAndroid' ':app:connectedDebugAndroidTest'
```

Expected: PASS if an Android device or emulator is connected. If no device is connected, record that instrumentation tests could not run because Gradle reports no connected devices.

- [ ] **Step 5: Commit final verification docs**

```powershell
git add ChronaAndroid/README.md docs/superpowers/plans/2026-05-14-android-mvp.md
git commit -m "docs: add chrona android build instructions"
```

---

## Self-Review Notes

- Spec coverage: toolchain setup, Android native app, rule-based parser, local confirmation, Room storage, WorkManager reminders, brand image, errors, and tests are mapped to tasks.
- Scope: long-term learning, night review, calendar sync, voice input, and cloud sync remain outside the MVP by design.
- Type consistency: `ParsedTask`, `ScheduleTask`, `TaskStatus`, `ReminderScheduler`, and `ScheduleRepository` names are consistent across tasks.
