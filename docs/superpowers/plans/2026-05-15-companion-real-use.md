# Chrona C-2 Real Use Companion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first real-use companion slice: behavior logging, local insights, stable long-text API parsing, four scene UI, updated APK.

**Architecture:** Keep the app端侧优先. Room stores tasks and behavior events; pure Kotlin services compute insights and split long API inputs; Compose reads repository flows and renders four scenes.

**Tech Stack:** Kotlin, Jetpack Compose, Room, WorkManager, kotlinx-coroutines-test, JUnit 4.

---

## File Structure

- Create `ChronaAndroid/app/src/main/java/com/chrona/ai/data/TaskBehaviorEvent.kt`: Room entity and event type constants.
- Modify `ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleTaskDao.kt`: insert and observe behavior events.
- Modify `ChronaAndroid/app/src/main/java/com/chrona/ai/data/AppDatabase.kt`: include the new entity and migration 1 to 2.
- Modify `ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleRepository.kt`: log creation, completion, deletion events and expose behavior event flow.
- Create `ChronaAndroid/app/src/main/java/com/chrona/ai/insights/ScheduleInsights.kt`: pure local analytics model and calculator.
- Create `ChronaAndroid/app/src/main/java/com/chrona/ai/api/ScheduleInputPreprocessor.kt`: split long input and expose segment threshold.
- Modify `ChronaAndroid/app/src/main/java/com/chrona/ai/api/OpenAiCompatibleScheduleParser.kt`: parse long input by segments and add `max_tokens`.
- Modify `ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaApp.kt`: add four scene navigation and summary/execute panels.
- Modify `ChronaAndroid/app/src/main/java/com/chrona/ai/MainActivity.kt`: pass insight data through existing repository.
- Add drawable PNG resources for four role scenes.
- Modify `README.md`, `ChronaAndroid/README.md`, and `.superpowers/chrona-ui-preview.html`.

## Task 1: Baseline and Spec Commit

**Files:**
- Create: `docs/superpowers/specs/2026-05-15-companion-real-use-design.md`
- Create: `docs/superpowers/plans/2026-05-15-companion-real-use.md`

- [ ] **Step 1: Run baseline tests**

Run: `powershell -ExecutionPolicy Bypass -File scripts/build-android.ps1`
Expected: exit 0.

- [ ] **Step 2: Commit spec and plan**

Run:
```powershell
git add docs/superpowers/specs/2026-05-15-companion-real-use-design.md docs/superpowers/plans/2026-05-15-companion-real-use.md
git commit -m "docs: design real-use companion upgrade"
```

## Task 2: Behavior Event Logging

**Files:**
- Create: `ChronaAndroid/app/src/main/java/com/chrona/ai/data/TaskBehaviorEvent.kt`
- Modify: `ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleTaskDao.kt`
- Modify: `ChronaAndroid/app/src/main/java/com/chrona/ai/data/ScheduleRepository.kt`
- Modify: `ChronaAndroid/app/src/main/java/com/chrona/ai/data/AppDatabase.kt`
- Test: `ChronaAndroid/app/src/test/java/com/chrona/ai/data/ScheduleRepositoryTest.kt`

- [ ] **Step 1: Write failing tests**

Add repository tests proving `addParsedTask` logs `CREATED`, `markDone` logs `COMPLETED`, and `delete` logs `DELETED`.

- [ ] **Step 2: Verify RED**

Run: `cd ChronaAndroid; .\gradlew.bat :app:testDebugUnitTest --tests com.chrona.ai.data.ScheduleRepositoryTest`
Expected: fail because behavior event APIs do not exist.

- [ ] **Step 3: Implement event entity, DAO methods, repository writes, and migration**

Add `TaskBehaviorEvent`, `BehaviorEventType`, DAO insert/observe methods, repository event writes after successful state changes, and Room migration 1 to 2.

- [ ] **Step 4: Verify GREEN**

Run the same repository test command.
Expected: pass.

- [ ] **Step 5: Commit**

Run:
```powershell
git add ChronaAndroid/app/src/main/java/com/chrona/ai/data ChronaAndroid/app/src/test/java/com/chrona/ai/data/ScheduleRepositoryTest.kt
git commit -m "feat: log schedule behavior events"
```

## Task 3: Local Insight Engine

**Files:**
- Create: `ChronaAndroid/app/src/main/java/com/chrona/ai/insights/ScheduleInsights.kt`
- Test: `ChronaAndroid/app/src/test/java/com/chrona/ai/insights/ScheduleInsightsTest.kt`

- [ ] **Step 1: Write failing tests**

Tests cover: no history returns gentle defaults; completed events produce completion rate and productive hour.

- [ ] **Step 2: Verify RED**

Run: `cd ChronaAndroid; .\gradlew.bat :app:testDebugUnitTest --tests com.chrona.ai.insights.ScheduleInsightsTest`
Expected: fail because insight classes do not exist.

- [ ] **Step 3: Implement insight calculator**

Create `ScheduleInsight` and `ScheduleInsightCalculator.calculate(tasks, events, now, zoneId)`.

- [ ] **Step 4: Verify GREEN**

Run the same insight test command.
Expected: pass.

- [ ] **Step 5: Commit**

Run:
```powershell
git add ChronaAndroid/app/src/main/java/com/chrona/ai/insights ChronaAndroid/app/src/test/java/com/chrona/ai/insights
git commit -m "feat: add local schedule insights"
```

## Task 4: Long Text API Stability

**Files:**
- Create: `ChronaAndroid/app/src/main/java/com/chrona/ai/api/ScheduleInputPreprocessor.kt`
- Modify: `ChronaAndroid/app/src/main/java/com/chrona/ai/api/OpenAiCompatibleScheduleParser.kt`
- Test: `ChronaAndroid/app/src/test/java/com/chrona/ai/api/OpenAiCompatibleScheduleParserTest.kt`

- [ ] **Step 1: Write failing tests**

Tests cover: long input splits into multiple segments; request body contains `max_tokens`; parser calls request per segment through a protected request hook.

- [ ] **Step 2: Verify RED**

Run: `cd ChronaAndroid; .\gradlew.bat :app:testDebugUnitTest --tests com.chrona.ai.api.OpenAiCompatibleScheduleParserTest`
Expected: fail because the preprocessor and max token behavior do not exist.

- [ ] **Step 3: Implement preprocessor and segmented parse**

Add `ScheduleInputPreprocessor.splitForApi(input, maxChars = 900)` and update parser to iterate segments. Add `max_tokens: 900` to request JSON.

- [ ] **Step 4: Verify GREEN**

Run the same API test command.
Expected: pass.

- [ ] **Step 5: Commit**

Run:
```powershell
git add ChronaAndroid/app/src/main/java/com/chrona/ai/api ChronaAndroid/app/src/test/java/com/chrona/ai/api/OpenAiCompatibleScheduleParserTest.kt
git commit -m "feat: stabilize long api schedule parsing"
```

## Task 5: Four Scene Companion UI

**Files:**
- Modify: `ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaApp.kt`
- Modify: `ChronaAndroid/app/src/main/java/com/chrona/ai/MainActivity.kt`
- Add: `ChronaAndroid/app/src/main/res/drawable/chrona_scene_home.png`
- Add: `ChronaAndroid/app/src/main/res/drawable/chrona_scene_chat.png`
- Add: `ChronaAndroid/app/src/main/res/drawable/chrona_scene_execute.png`
- Add: `ChronaAndroid/app/src/main/res/drawable/chrona_scene_summary.png`

- [ ] **Step 1: Copy role scene resources**

Copy the four PNGs from `.superpowers/final-package-20260515/角色融合交互终版素材` into Android drawable resources with ASCII names.

- [ ] **Step 2: Add scene navigation**

Update `ChronaApp` with `ChronaScene` enum and four top-level scene buttons: 首页, 对话, 执行, 总结.

- [ ] **Step 3: Add summary and execution panels**

Render local insight values in 总结. Render pending tasks with finish/delete buttons in 执行. Keep existing input and API settings available.

- [ ] **Step 4: Verify build**

Run: `powershell -ExecutionPolicy Bypass -File scripts/build-android.ps1`
Expected: exit 0.

- [ ] **Step 5: Commit**

Run:
```powershell
git add ChronaAndroid/app/src/main/java/com/chrona/ai/ui/ChronaApp.kt ChronaAndroid/app/src/main/java/com/chrona/ai/MainActivity.kt ChronaAndroid/app/src/main/res/drawable/chrona_scene_*.png
git commit -m "feat: add companion scene navigation"
```

## Task 6: Docs, Preview, APK

**Files:**
- Modify: `README.md`
- Modify: `ChronaAndroid/README.md`
- Modify: `.superpowers/chrona-ui-preview.html`
- Modify: `release/Chrona-debug.apk`

- [ ] **Step 1: Update documentation and progress preview**

Document C-2 behavior logging, local insights, optional API summaries, and long text split behavior.

- [ ] **Step 2: Build final APK**

Run: `powershell -ExecutionPolicy Bypass -File scripts/build-android.ps1`
Expected: exit 0 and updated debug APK copied to `release/Chrona-debug.apk`.

- [ ] **Step 3: Final verification**

Run: `git status -sb` and `git log --oneline -6`.
Expected: only intended docs/APK changes pending before final commit.

- [ ] **Step 4: Commit**

Run:
```powershell
git add README.md ChronaAndroid/README.md .superpowers/chrona-ui-preview.html release/Chrona-debug.apk
git commit -m "docs: update companion progress and apk"
```

## Self-Review

- Spec coverage: tasks cover behavior logging, local insights, long text API stability, UI scenes, docs and APK.
- Placeholder scan: no implementation step depends on unspecified files.
- Type consistency: behavior event names are `CREATED`, `COMPLETED`, `DELETED`; insight class is `ScheduleInsight`; preprocessor is `ScheduleInputPreprocessor`.
