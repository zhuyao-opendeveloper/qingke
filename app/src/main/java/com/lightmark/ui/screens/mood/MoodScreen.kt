package com.lightmark.ui.screens.mood

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightmark.ui.components.LightMarkCard
import com.lightmark.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.*

/**
 * 心情记录页（#122）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(
    onNavigateBack: () -> Unit,
    viewModel: MoodViewModel = hiltViewModel()
) {
    val moods by viewModel.moods.collectAsState()
    var scoreInput by remember { mutableStateOf(3) }
    var noteInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("心情记录", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            Text("记录此刻心情", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                (1..5).forEach { s ->
                    FilterChip(
                        selected = scoreInput == s,
                        onClick = { scoreInput = s },
                        label = { Text("$s 分", fontSize = 14.sp) }
                    )
                }
            }
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注（可选）") },
                singleLine = true
            )
            Button(onClick = {
                viewModel.addMood(scoreInput, noteInput)
                noteInput = ""
            }) { Text("保存记录") }

            Spacer(modifier = Modifier.height(Dimens.md))
            Text("历史记录", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (moods.isEmpty()) {
                Text("还没有记录，记录第一条吧～", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    items(moods, key = { it.id }) { m ->
                        LightMarkCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(Dimens.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${m.score} 分", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(Dimens.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (m.note.isNotBlank()) {
                                        Text(m.note, fontSize = 14.sp)
                                    }
                                    Text(
                                        formatMoodTime(m.createdAt), fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteMood(m) }) {
                                    Icon(
                                        Icons.Filled.Delete, contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMoodTime(ts: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(ts))
}
