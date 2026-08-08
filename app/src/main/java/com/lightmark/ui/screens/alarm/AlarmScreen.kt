package com.lightmark.ui.screens.alarm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.domain.model.Alarm
import com.lightmark.domain.model.AlarmSounds
import com.lightmark.ui.components.LightMarkCard
import com.lightmark.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    onNavigateBack: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val alarms by viewModel.alarms.collectAsState()
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("闹钟", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = "添加闹钟")
            }
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("还没有闹钟，点右下角添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Dimens.md),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        subtitle = viewModel.countdownText(alarm),
                        onToggle = { viewModel.toggle(alarm) },
                        onDelete = { viewModel.remove(alarm) },
                        onClick = { editing = alarm; showEditor = true }
                    )
                }
            }
        }
    }

    if (showEditor) {
        AlarmEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onConfirm = { result ->
                showEditor = false
                viewModel.save(result)
            }
        )
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    subtitle: String,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    LightMarkCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alarm.timeText,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        if (alarm.label.isNotBlank()) append("${alarm.label} · ")
                        append(alarm.repeatText)
                        append(" · ")
                        append(AlarmSounds.nameOf(alarm.soundId))
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            Switch(checked = alarm.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditorDialog(
    initial: Alarm?,
    onDismiss: () -> Unit,
    onConfirm: (Alarm) -> Unit
) {
    var hour by remember { mutableStateOf(initial?.hour ?: 7) }
    var minute by remember { mutableStateOf(initial?.minute ?: 0) }
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var repeat by remember { mutableStateOf(initial?.repeatDays ?: emptySet()) }
    var soundId by remember { mutableStateOf(initial?.soundId ?: AlarmSounds.DEFAULT_ID) }
    var vibrate by remember { mutableStateOf(initial?.vibrate ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建闹钟" else "编辑闹钟") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                // 时间选择
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NumberStepper(value = hour, range = 0..23, label = "时") { hour = it }
                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = Dimens.sm))
                    NumberStepper(value = minute, range = 0..59, label = "分") { minute = it }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("标签（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("重复", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Alarm.WEEK_LABELS.forEachIndexed { idx, name ->
                        val day = idx + 1
                        FilterChip(
                            selected = day in repeat,
                            onClick = {
                                repeat = if (day in repeat) repeat - day else repeat + day
                            },
                            label = { Text(name, fontSize = 12.sp) }
                        )
                    }
                }

                Text("铃声", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AlarmSounds.ALL.forEach { (id, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(selected = soundId == id, onClick = { soundId = id })
                            Text(name, fontSize = 14.sp)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vibrate, onCheckedChange = { vibrate = it })
                    Text("响铃时震动", fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    (initial ?: Alarm(id = java.util.UUID.randomUUID().toString())).copy(
                        label = label.trim(),
                        hour = hour,
                        minute = minute,
                        enabled = true,
                        repeatDays = repeat,
                        soundId = soundId,
                        vibrate = vibrate
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun NumberStepper(
    value: Int,
    range: IntRange,
    label: String,
    onChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = {
            onChange(if (value >= range.last) range.first else value + 1)
        }) { Text("▲") }
        Text("%02d".format(value), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = {
            onChange(if (value <= range.first) range.last else value - 1)
        }) { Text("▼") }
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
