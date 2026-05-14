# Chrona AI 智能日程助手

Chrona 是一个 Android 原生 MVP，用来把自然语言转换成可保存、可提醒的日程任务。

## 项目思路

- 用户用一句话输入多个事项，例如“明天下午三点提醒我拿快递，晚上健身”。
- 应用优先调用用户自己配置的 OpenAI-compatible API，解析标题、开始时间、结束时间和确认状态。
- API 请求会把手机当前系统时间和时区传给模型，避免“明天、今晚、周末”等相对时间解析错误。
- 未配置 API、API 失败、返回空结果时，自动回退到本地规则解析器。
- 解析结果先给用户确认，再写入本地 Room 数据库。
- 有明确时间的任务会通过 WorkManager 安排系统通知提醒。
- UI 使用项目人物形象拆分出的头像、空状态图和局部装饰图，并提供本地 API 设置入口。

## 代码结构

- `ChronaAndroid/`：Android 应用源码。
- `ChronaAndroid/app/src/main/java/com/chrona/ai/api/`：用户 API 设置、OpenAI-compatible 请求、AI/本地回退解析服务。
- `ChronaAndroid/app/src/main/java/com/chrona/ai/parser/`：本地规则解析器。
- `ChronaAndroid/app/src/main/java/com/chrona/ai/data/`：Room 数据库、DAO、仓库层。
- `ChronaAndroid/app/src/main/java/com/chrona/ai/reminder/`：WorkManager 通知提醒。
- `ChronaAndroid/app/src/main/java/com/chrona/ai/ui/`：Jetpack Compose 界面。
- `scripts/`：项目本地 Android 构建环境安装与打包脚本。
- `release/Chrona-debug.apk`：当前调试安装包。

## 安装包

当前 APK：

```text
release/Chrona-debug.apk
```

这是 debug 包，适合测试安装，不是正式商店签名包。

## 构建方式

在 PowerShell 中运行：

```powershell
powershell -ExecutionPolicy Bypass -File 'scripts/build-android.ps1'
```

脚本会使用项目目录下的 `.tools` 本地 JDK、Gradle 和 Android SDK，并在打包前执行 `clean`，避免旧 dex 缓存导致安装包缺类。

## API 配置示例

以 DeepSeek 为例：

```text
Base URL: https://api.deepseek.com
API Key: 你的 DeepSeek API Key
Model: deepseek-v4-flash
```

保存后输入一句自然语言日程，成功时界面会显示使用 API 解析；失败时会自动使用本地规则。

## 验证记录

- 已在 vivo S12 Pro 上通过 ADB 安装并启动验证。
- 已修复启动闪退问题。
- 已验证 debug 单元测试通过。
