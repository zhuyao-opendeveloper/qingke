package com.lightmark.ui.screens.pomodoro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightmark.ui.components.LightMarkCard
import com.lightmark.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(onNavigateBack: () -> Unit) {
    var workMin by remember { mutableStateOf(25) }
    var breakMin by remember { mutableStateOf(5) }
    var isWork by remember { mutableStateOf(true) }
    var running by remember { mutableStateOf(false) }
    var remainingMs by remember { mutableStateOf(workMin * 60_000L) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(running, isWork, workMin, breakMin) {
        if (!running) return@LaunchedEffect
        while (remainingMs > 0) {
            delay(1000)
            remainingMs -= 1000
        }
        // 阶段结束：切换工作/休息并继续
        isWork = !isWork
        remainingMs = (if (isWork) workMin else breakMin) * 60_000L
    }

    val mm = (remainingMs / 60_000).toInt()
    val ss = ((remainingMs % 60_000) / 1000).toInt()
    val timeText = "%02d:%02d".format(mm, ss)

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("番茄钟", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimens.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.lg)
        ) {
            Spacer(modifier = Modifier.height(Dimens.lg))
            Text(
                text = if (isWork) "专注时间" else "休息时间",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = timeText,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                Button(
                    onClick = { running = !running },
                    modifier = Modifier.size(width = 130.dp, height = 52.dp)
                ) {
                    Icon(
                        imageVector = if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(Dimens.sm))
                    Text(if (running) "暂停" else "开始")
                }
                OutlinedButton(
                    onClick = {
                        running = false
                        isWork = true
                        remainingMs = workMin * 60_000L
                    },
                    modifier = Modifier.size(width = 110.dp, height = 52.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(Dimens.sm))
                    Text("重置")
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.md)) {
                    DurationRow(
                        label = "专注时长（分钟）",
                        value = workMin,
                        enabled = !running,
                        onMinus = { workMin = (workMin - 1).coerceAtLeast(1) },
                        onPlus = { workMin = (workMin + 1).coerceAtMost(120) }
                    ) { if (!running && isWork) remainingMs = workMin * 60_000L }
                    Spacer(modifier = Modifier.height(Dimens.sm))
                    DurationRow(
                        label = "休息时长（分钟）",
                        value = breakMin,
                        enabled = !running,
                        onMinus = { breakMin = (breakMin - 1).coerceAtLeast(1) },
                        onPlus = { breakMin = (breakMin + 1).coerceAtMost(60) }
                    ) { if (!running && !isWork) remainingMs = breakMin * 60_000L }
                }
            }
        }
    }
}

@Composable
private fun DurationRow(
    label: String,
    value: Int,
    enabled: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onChanged: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMinus(); onChanged() }, enabled = enabled) {
                Text("－", fontSize = 22.sp)
            }
            Text(
                text = "$value",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { onPlus(); onChanged() }, enabled = enabled) {
                Text("＋", fontSize = 22.sp)
            }
        }
    }
}
