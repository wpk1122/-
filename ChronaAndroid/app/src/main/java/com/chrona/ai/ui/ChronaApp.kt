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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chrona.ai.R
import com.chrona.ai.data.ScheduleRepository
import com.chrona.ai.data.ScheduleTask
import com.chrona.ai.parser.ParsedTask
import com.chrona.ai.parser.TaskParser
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
    parser: TaskParser,
    repository: ScheduleRepository,
    modifier: Modifier = Modifier
) {
    val tasks by repository.observeTasks().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var input by remember { mutableStateOf(DefaultPrompt) }
    var parsedTasks by remember { mutableStateOf<List<ParsedTask>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

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
                    onParse = {
                        parsedTasks = parser.parse(input)
                        statusMessage = if (parsedTasks.isEmpty()) {
                            "还没有识别到日程，换一种说法试试。"
                        } else {
                            "已解析 ${parsedTasks.size} 条候选日程。"
                        }
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
                                parsedTasks.forEach { repository.addParsedTask(it) }
                                val savedCount = parsedTasks.size
                                parsedTasks = emptyList()
                                statusMessage = "已加入 $savedCount 条日程。"
                            }
                        },
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
                                repository.markDone(task.id)
                                statusMessage = "已完成：${task.title}"
                            }
                        },
                        onDelete = {
                            coroutineScope.launch {
                                repository.delete(task.id)
                                statusMessage = "已删除：${task.title}"
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
            painter = painterResource(id = R.drawable.chrona_character),
            contentDescription = "Chrona",
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
    onParse: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            label = { Text("想安排什么？") },
            placeholder = { Text(DefaultPrompt) },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(8.dp)
        )
        ElevatedButton(
            onClick = onParse,
            enabled = input.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("解析")
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
            painter = painterResource(id = R.drawable.chrona_character),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
        )
        Text(
            text = "先输入一句话，让 Chrona 帮你拆成日程。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
