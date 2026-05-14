# Chrona API 配置与视觉刷新设计

## 背景

当前 MVP 已经实现 Android 原生界面、本地规则解析、Room 保存和 WorkManager 提醒。下一步需要让用户自己提供 AI API，同时让界面更贴近“蓝色时间规划少女”的人物形象，并避免整张人物图在多个位置重复使用。

## 目标

- 新增用户可配置的 AI 接口信息：API Base URL、API Key、Model。
- API 按 OpenAI-compatible Chat Completions 风格设计，便于用户使用自己的兼容服务。
- 未配置 API、网络失败、解析失败时，自动回退到现有本地规则解析器。
- UI 配色从人物形象提取：深钴蓝、亮蓝紫、冰白、铃铛金、少量粉色点缀。
- 人物图拆分为局部资产：头像、空状态插图、装饰图，不再直接整张复用。
- 更新通知图标为更符合 Chrona 的“时间 + 铃铛”简洁符号。

## 非目标

- 不把 API Key 上传到任何第三方服务；只保存在本机应用内。
- 不实现多服务账号管理、云同步、计费、复杂提示词编辑器。
- 不替换已有本地规则解析器；它仍是离线兜底能力。

## 方案

采用“用户配置 API + 本地兜底”的混合解析方案。

新增配置入口放在首屏输入区下方，以紧凑的设置面板呈现。默认折叠，展示当前模式：`本地规则` 或 `AI 已配置`。展开后用户可填写 Base URL、API Key、Model，并保存到本地 SharedPreferences。API Key 输入框使用密码样式，不在普通 UI 中明文展示。

解析流程如下：

1. 用户点击“解析”。
2. 如果 API 配置完整，先调用远程 AI 解析。
3. 远程调用返回结构化 JSON 后转为 `ParsedTask`。
4. 如果配置缺失、请求失败、JSON 无效或结果为空，回退到 `RuleBasedTaskParser`。
5. UI 显示解析来源，例如“AI 解析”或“本地规则解析”。

## API 解析接口

新增 `AiTaskParser`，实现现有 `TaskParser` 接口，内部组合：

- `ApiSettingsStore`：读取和保存用户配置。
- `OpenAiCompatibleScheduleParser`：负责 HTTP 请求和 JSON 转换。
- `RuleBasedTaskParser`：兜底解析器。

请求使用 Android/Java 标准网络能力，避免为 MVP 引入大体量网络库。接口目标为 OpenAI-compatible `/chat/completions`：

- `Authorization: Bearer <API Key>`
- `Content-Type: application/json`
- `model`: 用户填写的模型名
- `messages`: system + user

系统提示要求模型只返回 JSON 数组，每项包含：

- `title`
- `startAt`
- `endAt`
- `confidenceNote`
- `needsTimeConfirmation`

时间使用设备当前时区和当前日期作为上下文。远程解析失败时不能阻断用户操作。

## 本地存储

API 配置使用 SharedPreferences：

- `api_base_url`
- `api_key`
- `api_model`

MVP 阶段不引入加密存储库，但 UI 不明文展示 key。后续如进入正式发布阶段，再升级到 AndroidX Security Crypto。

## UI 刷新

颜色更新为：

- 主色：人物衣饰深钴蓝 `#1D4ED8`
- 强调蓝紫：流动丝带亮蓝紫 `#4F46E5`
- 背景：冰白 `#F7FBFF`
- 表面：白色带浅蓝边框
- 成功/辅助：偏青蓝 `#0891B2`
- 提醒/警示：铃铛金 `#D99A18`
- 少量点缀：星光粉 `#EC5AA6`

首屏结构保持“可用工具”而非营销页：

- 顶部使用头像裁切图。
- 输入卡片加入轻微蓝紫边界和小的装饰裁切图。
- API 设置面板使用铃铛金/青蓝状态点，不做大面积渐变。
- 空状态使用单独裁切的坐姿/祈愿局部图。

## 图片拆分

从原图生成以下资源：

- `chrona_avatar.png`：人物头肩部裁切，用于顶部头像。
- `chrona_empty.png`：右下坐姿人物局部，用于空状态。
- `chrona_accent.png`：蓝色丝带/时钟局部，用于输入区或设置面板装饰。

原 `chrona_character.png` 可保留作为源资产，但 UI 不再直接引用整图。

## 图标更新

替换 `ic_notification.xml` 为简洁矢量图标：

- 圆形时钟轮廓。
- 小铃铛提示点。
- 一颗星点作为 Chrona 识别元素。

保持单色或双色矢量，适合 Android 通知小图标约束。

## 错误处理

- API 未配置：使用本地规则解析，并提示“正在使用本地规则解析”。
- API 请求失败：回退本地规则解析，并提示“AI 连接失败，已使用本地规则”。
- API 返回非 JSON：回退本地规则解析。
- 保存 API 配置失败：保留当前输入并显示错误。
- API Key 空白或 Base URL 无效：不发起网络请求。

## 测试与验证

- 为 API 配置存储添加 JVM 测试或可运行的轻量测试。
- 为 AI 解析响应 JSON 转换添加 JVM 测试。
- 保持现有解析器、Repository、Reminder 测试通过。
- 运行 `:app:testDebugUnitTest` 和 `:app:assembleDebug`。
- 更新右侧 HTML 预览，展示新配色、局部图片和 API 设置面板。

## 后续扩展

- 支持 Responses API 或更多兼容提供商模板。
- 使用 AndroidX Security Crypto 加密保存 API Key。
- 允许用户测试连接并查看模型返回错误。
- 增加真实 App 图标和自适应图标。
