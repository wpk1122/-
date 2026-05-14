package com.chrona.ai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chrona.ai.R
import com.chrona.ai.api.ApiSettings
import com.chrona.ai.api.ApiSettingsStore
import com.chrona.ai.api.ParseSource
import com.chrona.ai.api.ScheduleParseService
import com.chrona.ai.data.ScheduleRepository
import com.chrona.ai.data.ScheduleTask
import com.chrona.ai.parser.ParsedTask
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private const val DefaultPrompt = "明天提醒我拿快递，晚上健身，周末写报告"

@Composable
fun ChronaApp(
    parseService: ScheduleParseService,
    apiSettingsStore: ApiSettingsStore,
    repository: ScheduleRepository,
    onRequestNotificationPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tasks by repository.observeTasks().collectAsStateWithLifecycle(initialValue = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var input by remember { mutableStateOf(DefaultPrompt) }
    var parsedTasks by remember { mutableStateOf<List<ParsedTask>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isParsing by remember { mutableStateOf(false) }
    var apiSettings by remember { mutableStateOf(apiSettingsStore.load()) }
    var apiPanelExpanded by remember { mutableStateOf(false) }
    var baseUrlInput by remember { mutableStateOf(apiSettings.baseUrl) }
    var apiKeyInput by remember { mutableStateOf(apiSettings.apiKey) }
    var modelInput by remember { mutableStateOf(apiSettings.model) }

    fun saveApiSettings(settings: ApiSettings) {
        if (apiSettingsStore.save(settings)) {
            apiSettings = settings
            baseUrlInput = settings.baseUrl
            apiKeyInput = settings.apiKey
            modelInput = settings.model
            apiPanelExpanded = false
            statusMessage = if (settings.isComplete) {
                "AI 配置已保存"
            } else {
                "已切换为本地规则"
            }
        } else {
            statusMessage = "配置保存失败，请重试"
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ChronaHeader()
            }

            item {
                InputSection(
                    input = input,
                    onInputChange = {
                        input = it
                        statusMessage = null
                    },
                    enabled = !isSaving && !isParsing,
                    isParsing = isParsing,
                    onParse = {
                        coroutineScope.launch {
                            isParsing = true
                            try {
                                val result = parseService.parse(input.trim())
                                parsedTasks = result.tasks
                                statusMessage = if (result.tasks.isEmpty()) {
                                    "还没有识别到日程，换一种说法试试。"
                                } else {
                                    val sourceText = when (result.source) {
                                        ParseSource.USER_API -> "已使用你的 API 解析"
                                        ParseSource.LOCAL_RULES -> if (apiSettings.isComplete) {
                                            "API 暂不可用，已使用本地规则解析"
                                        } else {
                                            "已使用本地规则解析"
                                        }
                                    }
                                    "$sourceText ${result.tasks.size} 条候选日程。"
                                }
                            } catch (_: Exception) {
                                statusMessage = "解析失败，已保留输入"
                            } finally {
                                isParsing = false
                            }
                        }
                    }
                )
            }

            item {
                ApiSettingsPanel(
                    apiSettings = apiSettings,
                    expanded = apiPanelExpanded,
                    baseUrlInput = baseUrlInput,
                    apiKeyInput = apiKeyInput,
                    modelInput = modelInput,
                    onExpandedChange = { apiPanelExpanded = it },
                    onBaseUrlChange = { baseUrlInput = it },
                    onApiKeyChange = { apiKeyInput = it },
                    onModelChange = { modelInput = it },
                    onSave = {
                        saveApiSettings(
                            ApiSettings(
                                baseUrl = baseUrlInput,
                                apiKey = apiKeyInput,
                                model = modelInput
                            )
                        )
                    },
                    onClear = {
                        saveApiSettings(ApiSettings("", "", ""))
                    }
                )
            }

            statusMessage?.let { message ->
                item {
                    StatusMessage(message = message)
                }
            }

            if (parsedTasks.isNotEmpty()) {
                item {
                    SectionTitle(title = "解析结果")
                }
                items(parsedTasks, key = { "${it.sourceText}-${it.title}-${it.startAt}" }) { parsedTask ->
                    ParsedTaskCard(parsedTask = parsedTask)
                }
                item {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val tasksToSave = parsedTasks
                                if (tasksToSave.isEmpty()) return@launch

                                isSaving = true
                                try {
                                    tasksToSave.forEach { repository.addParsedTask(it) }
                                    val hasTimedTask = tasksToSave.any { it.startAt != null }
                                    parsedTasks = emptyList()
                                    statusMessage = "已加入 ${tasksToSave.size} 条日程。"
                                    if (hasTimedTask) {
                                        onRequestNotificationPermission()
                                    }
                                } catch (_: Exception) {
                                    statusMessage = "保存失败，请检查后重试"
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("确认加入")
                    }
                }
            }

            item {
                SectionTitle(title = "近期日程")
            }

            if (tasks.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onDone = {
                            coroutineScope.launch {
                                try {
                                    repository.markDone(task.id)
                                    statusMessage = "已完成：${task.title}"
                                } catch (_: Exception) {
                                    statusMessage = "完成失败，请稍后重试"
                                }
                            }
                        },
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    repository.delete(task.id)
                                    statusMessage = "已删除：${task.title}"
                                } catch (_: Exception) {
                                    statusMessage = "删除失败，请稍后重试"
                                }
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ChronaHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.chrona_avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Chrona",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "把自然语言变成清晰日程",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun InputSection(
    input: String,
    onInputChange: (String) -> Unit,
    enabled: Boolean,
    isParsing: Boolean,
    onParse: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.chrona_accent),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(132.dp),
                label = { Text("想安排什么？") },
                placeholder = { Text(DefaultPrompt) },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(8.dp)
            )
        }
        ElevatedButton(
            onClick = onParse,
            enabled = enabled && input.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(if (isParsing) "解析中..." else "解析")
        }
    }
}

@Composable
private fun ApiSettingsPanel(
    apiSettings: ApiSettings,
    expanded: Boolean,
    baseUrlInput: String,
    apiKeyInput: String,
    modelInput: String,
    onExpandedChange: (Boolean) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "API 设置",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (apiSettings.isComplete) {
                            "${apiSettings.model} · ${apiSettings.apiKey.maskApiKey()}"
                        } else {
                            "未配置时自动使用本地规则"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (apiSettings.isComplete) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (apiSettings.isComplete) "AI 已配置" else "本地规则",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (apiSettings.isComplete) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        }
                    )
                }
                TextButton(onClick = { onExpandedChange(!expanded) }) {
                    Text(if (expanded) "收起" else "编辑")
                }
            }

            if (expanded) {
                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = onBaseUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    placeholder = { Text("https://api.example.com/v1") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Uri
                    )
                )
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Password
                    )
                )
                OutlinedTextField(
                    value = modelInput,
                    onValueChange = onModelChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Model") },
                    placeholder = { Text("deepseek-chat") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("清空")
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ParsedTaskCard(parsedTask: ParsedTask) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = parsedTask.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = parsedTask.startAt?.formatForDisplay() ?: "时间待确认",
                style = MaterialTheme.typography.bodyMedium,
                color = if (parsedTask.startAt == null) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
            Text(
                text = parsedTask.confidenceNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: ScheduleTask,
    onDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { checked ->
                    if (checked) onDone()
                }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = task.startAt?.formatMillisForDisplay() ?: "时间待确认",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onDelete) {
                Text("删除")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.chrona_empty),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
        )
        Text(
            text = "先输入一句话，让 Chrona 帮你拆成日程。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun String.maskApiKey(): String {
    if (isBlank()) return "未填写"
    return "•••• ${takeLast(4)}"
}

private fun LocalDateTime.formatForDisplay(): String {
    return format(DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA))
}

private fun Long.formatMillisForDisplay(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .formatForDisplay()
}
