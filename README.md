# Chrona AI 智能日程助手

Chrona 是一个 Android 原生 AI 日程助手，用来把自然语言转换成可保存、可提醒、可复盘的日程任务。

## 项目思路

- 用户用一句话输入多个事项，例如“明天下午三点提醒我拿快递，晚上健身”。
- 应用优先调用用户自己配置的 OpenAI-compatible API，解析标题、开始时间、结束时间和确认状态。
- API 请求会把手机当前系统时间和时区传给模型，避免“明天、今晚、周末”等相对时间解析错误。
- 未配置 API、API 失败、返回空结果时，自动回退到本地规则解析器。
- 解析结果先给用户确认，再写入本地 Room 数据库。
- 有明确时间的任务会通过 WorkManager 安排系统通知提醒。
- UI 已整理为无人物的组件化界面：首页、对话、执行、总结四个入口使用抽象时间管理视觉；人物头像只保留在应用图标中。
- 完成、删除、创建等行为会写入手机本地行为日志，用于生成真实使用总结。
- 总结页会根据本地历史计算今日完成率、高效时段、过期待办和优化建议。
- 长文本超过约 900 字时会自动分段请求 API，并限制输出 token，降低超时和上下文超限风险。

## 代码结构

- `ChronaAndroid/`：Android 应用源码。
- `ChronaAndroid/app/src/main/java/com/chrona/ai/api/`：用户 API 设置、OpenAI-compatible 请求、AI/本地回退解析服务。
- `ChronaAndroid/app/src/main/java/com/chrona/ai/parser/`：本地规则解析器。
- `ChronaAndroid/app/src/main/java/com/chrona/ai/data/`：Room 数据库、DAO、仓库层。
- `ChronaAndroid/app/src/main/java/com/chrona/ai/insights/`：本地行为洞察和总结建议。
- `ChronaAndroid/app/src/main/java/com/chrona/ai/reminder/`：WorkManager 通知提醒。
- `ChronaAndroid/app/src/main/java/com/chrona/ai/ui/`：Jetpack Compose 界面。
- `scripts/`：项目本地 Android 构建环境安装与打包脚本。
- `release/Chrona-debug.apk`：当前可安装测试包。

## 安装包

当前 APK：

```text
release/Chrona-debug.apk
```

这是 debug 签名包，适合直接安装测试；正式商店发布仍需要使用你的正式 keystore 生成签名包或上传 AAB。

## 构建方式

在 PowerShell 中运行：

```powershell
powershell -ExecutionPolicy Bypass -File 'scripts/build-android.ps1'
```

脚本会使用项目目录下的 `.tools` 本地 JDK、Gradle 和 Android SDK，并在打包前执行 `clean`，避免旧 dex 缓存导致安装包缺类。
构建成功后会自动把 debug APK 复制到 `release/Chrona-debug.apk`。

## API 配置示例

以 DeepSeek 为例：

```text
Base URL: https://api.deepseek.com
API Key: 你的 DeepSeek API Key
Model: deepseek-v4-flash
```

保存后输入一句自然语言日程，成功时界面会显示使用 API 解析；失败时会自动使用本地规则。
复杂长文本会先分段，再逐段调用 API；未配置 API 时仍可离线使用本地规则。

## 验证记录

- 已在 vivo S12 Pro 上通过 ADB 安装并启动验证。
- 已修复启动闪退问题。
- 已验证 debug 单元测试通过。
- 已加入真实行为记录、本地总结洞察、长文本 API 稳定处理和无人物组件化 UI；人物头像仅作为应用图标使用。
