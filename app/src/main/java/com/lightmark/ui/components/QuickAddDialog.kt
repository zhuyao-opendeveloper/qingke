package com.lightmark.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import com.lightmark.util.NaturalLanguageParser

/**
 * 闪电添加对话框（#1 闪电添加 / #2 自然语言识别 / #21 语音添加 / #22 剪贴板创建）
 *
 * 输入一句话即可，例如：
 * `明天下午3点交报告 @工作 #紧急 每周`
 */
@Composable
fun QuickAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                text = if (text.isBlank()) spoken else "$text $spoken"
            }
        }
    }

    val parsed = remember(text) { NaturalLanguageParser.parse(text.ifBlank { " " }) }
    val summary = if (text.isBlank()) "" else NaturalLanguageParser.summary(parsed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("闪电添加", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("明天下午3点交报告 @工作 #紧急") },
                    supportingText = {
                        Text(
                            "支持：日期时间 · @清单 · #标签 · 紧急/重要 · 每天/每周",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            runCatching {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "说出要做的事")
                                }
                                voiceLauncher.launch(intent)
                            }
                        },
                        label = { Text("语音") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = null,
                                modifier = Modifier.height(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                    AssistChip(
                        onClick = {
                            val clip = clipboard.getText()?.text.orEmpty()
                            if (clip.isNotBlank()) {
                                text = if (text.isBlank()) clip.take(200) else "$text ${clip.take(200)}"
                            }
                        },
                        label = { Text("粘贴") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.height(18.dp)
                            )
                        }
                    )
                }

                if (summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "识别结果：${parsed.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) onConfirm(text)
                    onDismiss()
                },
                enabled = text.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
