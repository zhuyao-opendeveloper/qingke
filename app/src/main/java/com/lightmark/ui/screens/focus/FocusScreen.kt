package com.lightmark.ui.screens.focus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.ui.components.LightMarkCard
import com.lightmark.ui.components.MarkdownText
import com.lightmark.ui.theme.Dimens
import kotlinx.coroutines.delay

/**
 * 专注 / 禅模式界面（#58 / #59）
 *
 * 沉浸式呈现单个任务，配合番茄钟计时，隐藏其余干扰信息。
 * 纯本地实现，不依赖任何网络能力。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    todoId: String,
    onNavigateBack: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val task by viewModel.task.collectAsState()
    LaunchedEffect(todoId) { viewModel.load(todoId) }

    // 番茄钟计时（专注段）
    var workMin by remember { mutableStateOf(25) }
    var running by remember { mutableStateOf(false) }
    var remainingMs by remember { mutableStateOf(workMin * 60_000L) }

    LaunchedEffect(running, workMin) {
        if (!running) return@LaunchedEffect
        while (remainingMs > 0) {
            delay(1000)
            remainingMs -= 1000
        }
        running = false
    }

    val mm = (remainingMs / 60_000).toInt()
    val ss = ((remainingMs % 60_000) / 1000).toInt()
    val timeText = "%02d:%02d".format(mm, ss)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("专注模式", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
            Spacer(modifier = Modifier.height(Dimens.md))

            // 任务标题（沉浸大字）
            Text(
                text = task?.title ?: "加载中…",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 任务描述（轻量 Markdown）
            if (!task?.description.isNullOrBlank()) {
                LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                    MarkdownText(
                        text = task?.description ?: "",
                        modifier = Modifier.padding(Dimens.md)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lg))

            Text(
                text = "专注计时",
                fontSize = 14.sp,
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
                    onClick = {
                        if (remainingMs <= 0) remainingMs = workMin * 60_000L
                        running = !running
                    },
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
                        remainingMs = workMin * 60_000L
                    },
                    modifier = Modifier.size(width = 110.dp, height = 52.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(Dimens.sm))
                    Text("重置")
                }
            }

            LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("专注时长（分钟）", fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { workMin = (workMin - 1).coerceAtLeast(1) },
                            enabled = !running
                        ) { Text("－", fontSize = 22.sp) }
                        Text(
                            text = "$workMin",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        IconButton(
                            onClick = { workMin = (workMin + 1).coerceAtMost(120) },
                            enabled = !running
                        ) { Text("＋", fontSize = 22.sp) }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.markComplete(todoId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = task != null && !(task?.isCompleted ?: false)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(Dimens.sm))
                Text("完成此任务")
            }
        }
    }
}
