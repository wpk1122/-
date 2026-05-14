# Chrona API Settings And Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add user-provided OpenAI-compatible API parsing with local fallback, refresh Chrona's UI colors from the character image, split the character image into focused assets, update the reminder icon, and refresh the browser preview.

**Architecture:** Keep the existing synchronous `TaskParser` for local rules. Add a suspend `ScheduleParseService` that reads user API settings, tries an OpenAI-compatible parser on an IO dispatcher, and falls back to `RuleBasedTaskParser` with a visible source message. Store API settings in SharedPreferences through a small store class; keep JSON conversion in testable pure Kotlin helpers.

**Tech Stack:** Kotlin, Jetpack Compose, SharedPreferences, Java `HttpURLConnection`, org.json, Android `INTERNET` permission, PowerShell/.NET image cropping, Android vector drawable, existing Gradle/JUnit test setup.

---

## File Structure

- Create: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/main/java/com/chrona/ai/api/ApiSettings.kt`  
  Immutable API settings model and completeness/base URL helpers.
- Create: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/main/java/com/chrona/ai/api/ApiSettingsStore.kt`  
  SharedPreferences-backed local storage for base URL, API key, and model.
- Create: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/main/java/com/chrona/ai/api/OpenAiCompatibleScheduleParser.kt`  
  HTTP request and JSON response parsing for OpenAI-compatible chat completions.
- Create: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/main/java/com/chrona/ai/api/ScheduleParseService.kt`  
  Suspend parser facade with AI-first and local-rule fallback behavior.
- Create: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/test/java/com/chrona/ai/api/OpenAiCompatibleScheduleParserTest.kt`  
  JVM tests for settings completeness and JSON conversion.
- Modify: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/build.gradle.kts`  
  Add a JVM test dependency for real `org.json` behavior under local unit tests.
- Modify: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/main/AndroidManifest.xml`  
  Add `INTERNET` permission for user-provided API calls.
- Modify: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/main/java/com/chrona/ai/MainActivity.kt`  
  Wire `ApiSettingsStore` and `ScheduleParseService` into `ChronaApp`.
- Modify: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaApp.kt`  
  Replace direct parser call with `ScheduleParseService`, add API settings panel, and use split image assets.
- Modify: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaTheme.kt`  
  Update color palette from character image.
- Create image assets:
  - `ChronaAndroid/app/src/main/res/drawable/chrona_avatar.png`
  - `ChronaAndroid/app/src/main/res/drawable/chrona_empty.png`
  - `ChronaAndroid/app/src/main/res/drawable/chrona_accent.png`
- Modify: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/main/res/drawable/ic_notification.xml`
- Modify: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.superpowers/chrona-ui-preview.html`

---

### Task 1: API Settings And Parse Service

**Files:**
- Create: `ChronaAndroid/app/src/main/java/com/chrona/ai/api/ApiSettings.kt`
- Create: `ChronaAndroid/app/src/main/java/com/chrona/ai/api/ApiSettingsStore.kt`
- Create: `ChronaAndroid/app/src/main/java/com/chrona/ai/api/OpenAiCompatibleScheduleParser.kt`
- Create: `ChronaAndroid/app/src/main/java/com/chrona/ai/api/ScheduleParseService.kt`
- Create: `ChronaAndroid/app/src/test/java/com/chrona/ai/api/OpenAiCompatibleScheduleParserTest.kt`
- Modify: `ChronaAndroid/app/build.gradle.kts`
- Modify: `ChronaAndroid/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write the failing API parser tests**

Create `ChronaAndroid/app/src/test/java/com/chrona/ai/api/OpenAiCompatibleScheduleParserTest.kt`:

```kotlin
package com.chrona.ai.api

import com.chrona.ai.parser.RuleBasedTaskParser
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiCompatibleScheduleParserTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val clock = Clock.fixed(Instant.parse("2026-05-14T02:00:00Z"), zone)

    @Test
    fun settingsAreCompleteOnlyWhenAllFieldsExist() {
        assertFalse(ApiSettings("", "key", "model").isComplete)
        assertFalse(ApiSettings("https://api.example.com", "", "model").isComplete)
        assertFalse(ApiSettings("https://api.example.com", "key", "").isComplete)
        assertTrue(ApiSettings("https://api.example.com", "key", "model").isComplete)
    }

    @Test
    fun appendsChatCompletionsPathWhenBaseUrlDoesNotContainIt() {
        val settings = ApiSettings("https://api.example.com/v1", "key", "model")

        assertEquals(
            "https://api.example.com/v1/chat/completions",
            settings.chatCompletionsUrl
        )
    }

    @Test
    fun keepsExplicitChatCompletionsUrl() {
        val settings = ApiSettings("https://api.example.com/v1/chat/completions", "key", "model")

        assertEquals(
            "https://api.example.com/v1/chat/completions",
            settings.chatCompletionsUrl
        )
    }

    @Test
    fun parsesOpenAiCompatibleJsonContentIntoParsedTasks() {
        val content = """
            [
              {
                "title": "拿快递",
                "startAt": "2026-05-15T15:00:00",
                "endAt": "2026-05-15T16:00:00",
                "confidenceNote": "AI 解析",
                "needsTimeConfirmation": false
              }
            ]
        """.trimIndent()

        val result = OpenAiCompatibleScheduleParser.parseTasksFromModelContent(
            content = content,
            sourceText = "明天提醒我拿快递"
        )

        assertEquals(1, result.size)
        assertEquals("拿快递", result.first().title)
        assertEquals(LocalDateTime.of(2026, 5, 15, 15, 0), result.first().startAt)
        assertEquals(LocalDateTime.of(2026, 5, 15, 16, 0), result.first().endAt)
        assertFalse(result.first().needsTimeConfirmation)
    }

    @Test
    fun fallbackServiceUsesLocalParserWhenSettingsMissing() = kotlinx.coroutines.test.runTest {
        val service = ScheduleParseService(
            settingsProvider = { ApiSettings("", "", "") },
            remoteParser = object : RemoteScheduleParser {
                override suspend fun parse(input: String, settings: ApiSettings): List<com.chrona.ai.parser.ParsedTask> {
                    error("Remote parser should not be called")
                }
            },
            fallbackParser = RuleBasedTaskParser(clock, zone)
        )

        val result = service.parse("明天提醒我拿快递")

        assertEquals(ParseSource.LOCAL_RULES, result.source)
        assertEquals("拿快递", result.tasks.first().title)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
$env:JAVA_HOME='C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/jdk-17'
$env:ANDROID_HOME='C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
& 'C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid' ':app:testDebugUnitTest' --tests 'com.chrona.ai.api.OpenAiCompatibleScheduleParserTest'
```

Expected: FAIL because API classes do not exist.

- [ ] **Step 3: Add platform and JVM test support**

In `ChronaAndroid/app/src/main/AndroidManifest.xml`, add this permission outside the `<application>` tag:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

In `ChronaAndroid/app/build.gradle.kts`, add the real JSON implementation to unit tests:

```kotlin
testImplementation("org.json:json:20240303")
```

- [ ] **Step 4: Add API settings model**

Create `ChronaAndroid/app/src/main/java/com/chrona/ai/api/ApiSettings.kt`:

```kotlin
package com.chrona.ai.api

data class ApiSettings(
    val baseUrl: String,
    val apiKey: String,
    val model: String
) {
    val isComplete: Boolean
        get() = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()

    val chatCompletionsUrl: String
        get() {
            val trimmed = baseUrl.trim().trimEnd('/')
            return if (trimmed.endsWith("/chat/completions")) trimmed else "$trimmed/chat/completions"
        }
}
```

- [ ] **Step 5: Add settings store**

Create `ChronaAndroid/app/src/main/java/com/chrona/ai/api/ApiSettingsStore.kt`:

```kotlin
package com.chrona.ai.api

import android.content.Context

class ApiSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "chrona_api_settings",
        Context.MODE_PRIVATE
    )

    fun load(): ApiSettings {
        return ApiSettings(
            baseUrl = preferences.getString(KEY_BASE_URL, "") ?: "",
            apiKey = preferences.getString(KEY_API_KEY, "") ?: "",
            model = preferences.getString(KEY_MODEL, "") ?: ""
        )
    }

    fun save(settings: ApiSettings): Boolean {
        return preferences.edit()
            .putString(KEY_BASE_URL, settings.baseUrl.trim())
            .putString(KEY_API_KEY, settings.apiKey.trim())
            .putString(KEY_MODEL, settings.model.trim())
            .commit()
    }

    companion object {
        private const val KEY_BASE_URL = "api_base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "api_model"
    }
}
```

- [ ] **Step 6: Add OpenAI-compatible parser**

Create `ChronaAndroid/app/src/main/java/com/chrona/ai/api/OpenAiCompatibleScheduleParser.kt` with:

```kotlin
package com.chrona.ai.api

import com.chrona.ai.parser.ParsedTask
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

interface RemoteScheduleParser {
    suspend fun parse(input: String, settings: ApiSettings): List<ParsedTask>
}

class OpenAiCompatibleScheduleParser(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : RemoteScheduleParser {
    override suspend fun parse(input: String, settings: ApiSettings): List<ParsedTask> {
        val connection = (URL(settings.chatCompletionsUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(buildRequestBody(input, settings.model, zoneId))
            }
            val code = connection.responseCode
            if (code !in 200..299) return emptyList()
            val body = BufferedReader(connection.inputStream.reader(Charsets.UTF_8)).use { it.readText() }
            val content = JSONObject(body)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            parseTasksFromModelContent(content, input)
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        fun buildRequestBody(input: String, model: String, zoneId: ZoneId): String {
            val systemPrompt = """
                你是 Chrona 的日程解析器。只返回 JSON 数组，不要解释。
                每项字段：title, startAt, endAt, confidenceNote, needsTimeConfirmation。
                startAt/endAt 使用 ISO_LOCAL_DATE_TIME；不能判断时间时使用 null。
                今天日期：${LocalDate.now(zoneId)}，时区：$zoneId。
            """.trimIndent()
            return JSONObject()
                .put("model", model)
                .put(
                    "messages",
                    JSONArray()
                        .put(JSONObject().put("role", "system").put("content", systemPrompt))
                        .put(JSONObject().put("role", "user").put("content", input))
                )
                .put("temperature", 0.1)
                .toString()
        }

        fun parseTasksFromModelContent(content: String, sourceText: String): List<ParsedTask> {
            val array = JSONArray(content.trim())
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val title = item.optString("title").trim()
                    if (title.isBlank()) continue
                    add(
                        ParsedTask(
                            title = title,
                            startAt = item.optNullableDateTime("startAt"),
                            endAt = item.optNullableDateTime("endAt"),
                            sourceText = sourceText,
                            confidenceNote = item.optString("confidenceNote", "AI 解析"),
                            needsTimeConfirmation = item.optBoolean("needsTimeConfirmation", item.isNull("startAt"))
                        )
                    )
                }
            }
        }

        private fun JSONObject.optNullableDateTime(name: String): LocalDateTime? {
            if (isNull(name)) return null
            val raw = optString(name).trim()
            if (raw.isBlank() || raw == "null") return null
            return runCatching { LocalDateTime.parse(raw) }.getOrNull()
        }
    }
}
```

- [ ] **Step 7: Add parse service**

Create `ChronaAndroid/app/src/main/java/com/chrona/ai/api/ScheduleParseService.kt`:

```kotlin
package com.chrona.ai.api

import com.chrona.ai.parser.ParsedTask
import com.chrona.ai.parser.RuleBasedTaskParser
import com.chrona.ai.parser.TaskParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ParseSource {
    AI,
    LOCAL_RULES,
    FALLBACK
}

data class ParseResult(
    val tasks: List<ParsedTask>,
    val source: ParseSource,
    val message: String
)

class ScheduleParseService(
    private val settingsProvider: () -> ApiSettings,
    private val remoteParser: RemoteScheduleParser,
    private val fallbackParser: TaskParser = RuleBasedTaskParser()
) {
    suspend fun parse(input: String): ParseResult = withContext(Dispatchers.IO) {
        val settings = settingsProvider()
        if (!settings.isComplete) {
            return@withContext ParseResult(
                tasks = fallbackParser.parse(input),
                source = ParseSource.LOCAL_RULES,
                message = "正在使用本地规则解析"
            )
        }
        val remoteTasks = remoteParser.parse(input, settings)
        if (remoteTasks.isNotEmpty()) {
            ParseResult(remoteTasks, ParseSource.AI, "AI 解析完成")
        } else {
            ParseResult(
                tasks = fallbackParser.parse(input),
                source = ParseSource.FALLBACK,
                message = "AI 连接失败，已使用本地规则"
            )
        }
    }
}
```

- [ ] **Step 8: Run API tests**

Run the same targeted Gradle command. Expected: PASS.

- [ ] **Step 9: Commit API parsing foundation**

```powershell
git add ChronaAndroid/app/src/main/java/com/chrona/ai/api ChronaAndroid/app/src/test/java/com/chrona/ai/api
git commit -m "feat: add user api parsing fallback"
```

---

### Task 2: Split Character Assets And Notification Icon

**Files:**
- Create: `ChronaAndroid/app/src/main/res/drawable/chrona_avatar.png`
- Create: `ChronaAndroid/app/src/main/res/drawable/chrona_empty.png`
- Create: `ChronaAndroid/app/src/main/res/drawable/chrona_accent.png`
- Modify: `ChronaAndroid/app/src/main/res/drawable/ic_notification.xml`

- [ ] **Step 1: Generate cropped image assets**

Run this PowerShell script from the worktree root:

```powershell
Add-Type -AssemblyName System.Drawing
$source = 'C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/main/res/drawable/chrona_character.png'
$outDir = 'C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/app/src/main/res/drawable'
$image = [System.Drawing.Image]::FromFile($source)

function Save-Crop($name, $x, $y, $w, $h, $size) {
  $bitmap = New-Object System.Drawing.Bitmap $size, $size
  $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
  $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $graphics.DrawImage($image, (New-Object System.Drawing.Rectangle 0,0,$size,$size), (New-Object System.Drawing.Rectangle $x,$y,$w,$h), [System.Drawing.GraphicsUnit]::Pixel)
  $bitmap.Save((Join-Path $outDir $name), [System.Drawing.Imaging.ImageFormat]::Png)
  $graphics.Dispose()
  $bitmap.Dispose()
}

Save-Crop 'chrona_avatar.png' 360 80 420 420 512
Save-Crop 'chrona_empty.png' 730 610 380 380 512
Save-Crop 'chrona_accent.png' 30 170 380 380 512
$image.Dispose()
```

Expected: the three PNG files exist. If crop coordinates look poor after preview, adjust once and note the final crop.

- [ ] **Step 2: Replace notification icon**

Replace `ChronaAndroid/app/src/main/res/drawable/ic_notification.xml` with:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#1D4ED8"
        android:pathData="M12,3a7,7 0,0 0,-7 7v3.4l-1.8,2.4A1,1 0,0 0,4 17.4h16a1,1 0,0 0,0.8 -1.6L19,13.4V10a7,7 0,0 0,-7 -7zM12,5a5,5 0,0 1,5 5v4l1,1.4H6L7,14v-4a5,5 0,0 1,5 -5z" />
    <path
        android:fillColor="#D99A18"
        android:pathData="M9.7,19a2.4,2.4 0,0 0,4.6 0h-4.6z" />
    <path
        android:fillColor="#EC5AA6"
        android:pathData="M18.8,2.2l0.5,1.2 1.2,0.5 -1.2,0.5 -0.5,1.2 -0.5,-1.2 -1.2,-0.5 1.2,-0.5 0.5,-1.2z" />
</vector>
```

- [ ] **Step 3: Run resource compile**

```powershell
$env:JAVA_HOME='C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/jdk-17'
$env:ANDROID_HOME='C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
& 'C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid' ':app:assembleDebug'
```

Expected: PASS.

- [ ] **Step 4: Commit assets**

```powershell
git add ChronaAndroid/app/src/main/res/drawable/chrona_avatar.png ChronaAndroid/app/src/main/res/drawable/chrona_empty.png ChronaAndroid/app/src/main/res/drawable/chrona_accent.png ChronaAndroid/app/src/main/res/drawable/ic_notification.xml
git commit -m "feat: split chrona visual assets"
```

---

### Task 3: UI API Settings Panel And Palette Refresh

**Files:**
- Modify: `ChronaAndroid/app/src/main/java/com/chrona/ai/MainActivity.kt`
- Modify: `ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaApp.kt`
- Modify: `ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaTheme.kt`

- [ ] **Step 1: Update theme palette**

In `ChronaTheme.kt`, replace colors with:

```kotlin
private val ChronaLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0ECFF),
    onPrimaryContainer = Color(0xFF0B2A6F),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF0E3B4A),
    tertiary = Color(0xFFD99A18),
    onTertiary = Color(0xFF2A1D00),
    tertiaryContainer = Color(0xFFFFEDBF),
    onTertiaryContainer = Color(0xFF4C3300),
    background = Color(0xFFF7FBFF),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE8F1FF),
    onSurfaceVariant = Color(0xFF4B5D75),
    outline = Color(0xFF9DB4D0),
    error = Color(0xFFB91C1C),
    onError = Color.White
)
```

- [ ] **Step 2: Wire API service in MainActivity**

Change `MainActivity` to instantiate:

```kotlin
val settingsStore = remember { ApiSettingsStore(this) }
val parseService = remember {
    ScheduleParseService(
        settingsProvider = { settingsStore.load() },
        remoteParser = OpenAiCompatibleScheduleParser(),
        fallbackParser = RuleBasedTaskParser()
    )
}
```

Pass `parseService` and `settingsStore` to `ChronaApp`. Keep notification permission callback.

- [ ] **Step 3: Update ChronaApp signature and state**

Change `ChronaApp` parameters to:

```kotlin
fun ChronaApp(
    parseService: ScheduleParseService,
    apiSettingsStore: ApiSettingsStore,
    repository: ScheduleRepository,
    onRequestNotificationPermission: () -> Unit = {},
    modifier: Modifier = Modifier
)
```

Add state:

```kotlin
var apiSettings by remember { mutableStateOf(apiSettingsStore.load()) }
var apiPanelExpanded by remember { mutableStateOf(false) }
var baseUrlInput by remember { mutableStateOf(apiSettings.baseUrl) }
var apiKeyInput by remember { mutableStateOf(apiSettings.apiKey) }
var modelInput by remember { mutableStateOf(apiSettings.model) }
var isParsing by remember { mutableStateOf(false) }
```

- [ ] **Step 4: Replace parse action**

`onParse` should launch a coroutine, set `isParsing = true`, call `parseService.parse(input)`, assign `parsedTasks = result.tasks`, and set `statusMessage = result.message` or a no-result message. On exception, use `"解析失败，已保留输入"`. Disable parse while parsing or saving.

- [ ] **Step 5: Add API settings panel**

Add a composable `ApiSettingsPanel(...)` below the input section. It must:

- show collapsed status: `AI 已配置` when settings complete, else `本地规则`
- expand/collapse via a `TextButton`
- fields: Base URL, API Key password-style, Model
- save button calls `apiSettingsStore.save(ApiSettings(...))`, updates local state, collapses, and shows `"AI 配置已保存"` or failure
- use compact cards/rows, no nested cards

- [ ] **Step 6: Swap image resources**

Use:

- Header image: `R.drawable.chrona_avatar`
- Empty state image: `R.drawable.chrona_empty`
- Optional small accent image near API panel/input: `R.drawable.chrona_accent`

Do not directly reference `R.drawable.chrona_character` in UI.

- [ ] **Step 7: Run compile/tests**

```powershell
$env:JAVA_HOME='C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/jdk-17'
$env:ANDROID_HOME='C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
& 'C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid' ':app:compileDebugKotlin' ':app:testDebugUnitTest'
```

Expected: PASS.

- [ ] **Step 8: Commit UI/API panel**

```powershell
git add ChronaAndroid/app/src/main/java/com/chrona/ai/MainActivity.kt ChronaAndroid/app/src/main/java/com/chrona/ai/ui
git commit -m "feat: add api settings panel and chrona palette"
```

---

### Task 4: Refresh Browser Preview And Final Verification

**Files:**
- Modify: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.superpowers/chrona-ui-preview.html`
- Modify: `C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid/README.md` if API settings need a short note.

- [ ] **Step 1: Update preview HTML**

Refresh `.superpowers/chrona-ui-preview.html` so it shows:

- avatar crop in header
- API settings panel with Base URL / API Key / Model fields
- new blue-purple/ice/gold/pink palette
- empty-state crop and accent crop
- no whole-character image usage

- [ ] **Step 2: Add README note**

If not already present, add under `Current MVP`:

```markdown
- Optional user-provided OpenAI-compatible API settings with local parser fallback.
```

Add a short note:

```markdown
API settings are stored locally on the device. If no API is configured, Chrona uses the local rule-based parser.
```

- [ ] **Step 3: Final verification**

Run:

```powershell
$env:JAVA_HOME='C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/jdk-17'
$env:ANDROID_HOME='C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
& 'C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/.tools/gradle-8.7/bin/gradle.bat' -p 'C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/ChronaAndroid' ':app:testDebugUnitTest'
powershell -ExecutionPolicy Bypass -File 'C:/Users/suifeng/Desktop/日历/.worktrees/chrona-android-mvp/scripts/build-android.ps1'
```

Expected: both PASS and debug APK exists.

- [ ] **Step 4: Commit preview/docs**

```powershell
git add .superpowers/chrona-ui-preview.html ChronaAndroid/README.md
git commit -m "docs: refresh chrona api visual preview"
```

---

## Self-Review Notes

- Spec coverage: API settings, OpenAI-compatible parser, fallback, SharedPreferences, palette, split assets, icon, preview, README, tests, and final build are covered.
- Scope: real LLM integration is limited to user-provided compatible API; cloud sync, account management, and encrypted key storage remain out of scope.
- Type consistency: UI uses `ScheduleParseService`; existing `TaskParser` remains for local parser and tests; `ParsedTask` remains unchanged.
