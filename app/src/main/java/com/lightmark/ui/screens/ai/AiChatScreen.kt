package com.lightmark.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * AI 对话页
 *
 * - 与 AI 自由对话
 * - 一句话生成待办并写入数据库（「生成待办」按钮）
 * - 内置几个快捷指令
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val lastAddedCount by viewModel.lastAddedCount.collectAsState()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        scope.launch { listState.animateScrollToItem(messages.size.coerceAtLeast(0)) }
    }

    LaunchedEffect(lastAddedCount) {
        if (lastAddedCount != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearAddedCount()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.sm))
                        Text("AI 助手", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
        ) {
            // 消息列表
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.lg),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                items(messages, key = { it.role + it.content.take(12) + it.hashCode() }) { msg ->
                    ChatBubble(msg)
                }
                if (isThinking) {
                    item { ThinkingBubble() }
                }
            }

            // 快捷指令
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.lg),
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                AssistChip(
                    onClick = { input = "帮我规划今天的待办清单"; },
                    label = { Text("规划今天", fontSize = 12.sp) }
                )
                AssistChip(
                    onClick = { viewModel.generateAndAdd(input.ifBlank { "买菜、写周报、健身 30 分钟" }) },
                    label = { Text("生成待办", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                    }
                )
            }

            // 输入区
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("说点什么，或描述要做的待办…") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(Dimens.sm))
                    FloatingActionButton(
                        onClick = {
                            viewModel.send(input)
                            input = ""
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                    }
                }
            }

            AnimatedVisibility(
                visible = lastAddedCount != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "已添加 $lastAddedCount 条待办到列表",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.lg, vertical = Dimens.xs)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Text(
                text = msg.content,
                fontSize = 14.sp,
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(Dimens.md)
            )
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(Dimens.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text("AI 思考中…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
