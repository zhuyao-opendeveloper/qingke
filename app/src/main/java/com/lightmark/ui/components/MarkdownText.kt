package com.lightmark.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.text.BasicText

/**
 * 轻量 Markdown 渲染（#4/#5）：支持 **加粗**、*斜体*、`代码`、[链接](url)。
 * 仅做行内样式，不处理标题/列表等块级语法，足够任务描述使用。
 */
@Composable
fun MarkdownText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val annotated = remember(text, color, primary) { parseMarkdown(text, color, primary) }
    BasicText(text = annotated, modifier = modifier)
}

private fun parseMarkdown(text: String, baseColor: Color, primaryColor: Color): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    val n = text.length
    while (i < n) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    builder.append(text[i]); i++
                }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    builder.withStyle(
                        SpanStyle(fontFamily = FontFamily.Monospace, color = primaryColor)
                    ) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    builder.append(text[i]); i++
                }
            }
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1 && end > i + 1) {
                    builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    builder.append(text[i]); i++
                }
            }
            text.startsWith("[", i) -> {
                val close = text.indexOf("]", i)
                val paren = text.indexOf("(", close + 1)
                val endp = text.indexOf(")", paren + 1)
                if (close != -1 && paren == close + 1 && endp != -1) {
                    val label = text.substring(i + 1, close)
                    builder.withStyle(
                        SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline)
                    ) { append(label) }
                    i = endp + 1
                } else {
                    builder.append(text[i]); i++
                }
            }
            else -> {
                builder.append(text[i]); i++
            }
        }
    }
    return builder.toAnnotatedString()
}
