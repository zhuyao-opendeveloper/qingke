package com.lightmark.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightmark.icons.IconProvider
import com.lightmark.icons.LightMarkIcon
import com.lightmark.ui.theme.Dimens

/**
 * 轻刻风格的搜索栏
 *
 * Material 3 搜索栏样式，圆角设计
 * 支持输入文本搜索待办事项
 */
@Composable
fun LightMarkSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    iconProvider: IconProvider,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索待办事项..."
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.lg)
            .height(56.dp),
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            LightMarkIcon(
                provider = iconProvider,
                icon = { search },
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    LightMarkIcon(
                        provider = iconProvider,
                        icon = { close },
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(Dimens.buttonCornerRadius),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}
