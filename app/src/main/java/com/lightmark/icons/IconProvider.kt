package com.lightmark.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lightmark.domain.model.IconPack

interface IconProvider {
    val add: ImageVector
    val check: ImageVector
    val close: ImageVector
    val delete: ImageVector
    val edit: ImageVector
    val home: ImageVector
    val info: ImageVector
    val menu: ImageVector
    val notifications: ImageVector
    val person: ImageVector
    val search: ImageVector
    val settings: ImageVector
    val star: ImageVector
    val circleOutline: ImageVector
    val checkCircle: ImageVector
    val flagOutline: ImageVector
}

object MaterialIconProvider : IconProvider {
    override val add: ImageVector get() = Icons.Filled.Add
    override val check: ImageVector get() = Icons.Filled.Check
    override val close: ImageVector get() = Icons.Filled.Clear
    override val delete: ImageVector get() = Icons.Filled.Delete
    override val edit: ImageVector get() = Icons.Filled.Edit
    override val home: ImageVector get() = Icons.Filled.Home
    override val info: ImageVector get() = Icons.Filled.Info
    override val menu: ImageVector get() = Icons.Filled.Menu
    override val notifications: ImageVector get() = Icons.Filled.Notifications
    override val person: ImageVector get() = Icons.Filled.Person
    override val search: ImageVector get() = Icons.Filled.Search
    override val settings: ImageVector get() = Icons.Filled.Settings
    override val star: ImageVector get() = Icons.Filled.Star
    override val circleOutline: ImageVector get() = Icons.Outlined.Circle
    override val checkCircle: ImageVector get() = Icons.Outlined.CheckCircleOutline
    override val flagOutline: ImageVector get() = Icons.Outlined.Flag
}

object FluentIconProvider : IconProvider {
    override val add: ImageVector get() = Icons.Filled.Add
    override val check: ImageVector get() = Icons.Filled.Check
    override val close: ImageVector get() = Icons.Filled.Clear
    override val delete: ImageVector get() = Icons.Filled.Delete
    override val edit: ImageVector get() = Icons.Filled.Edit
    override val home: ImageVector get() = Icons.Filled.Home
    override val info: ImageVector get() = Icons.Filled.Info
    override val menu: ImageVector get() = Icons.Filled.Menu
    override val notifications: ImageVector get() = Icons.Filled.Notifications
    override val person: ImageVector get() = Icons.Filled.Person
    override val search: ImageVector get() = Icons.Filled.Search
    override val settings: ImageVector get() = Icons.Filled.Settings
    override val star: ImageVector get() = Icons.Filled.StarBorder
    override val circleOutline: ImageVector get() = Icons.Outlined.Circle
    override val checkCircle: ImageVector get() = Icons.Outlined.CheckCircleOutline
    override val flagOutline: ImageVector get() = Icons.Outlined.Flag
}

object LucideIconProvider : IconProvider {
    override val add: ImageVector get() = Icons.Filled.Add
    override val check: ImageVector get() = Icons.Filled.Check
    override val close: ImageVector get() = Icons.Filled.Clear
    override val delete: ImageVector get() = Icons.Filled.Delete
    override val edit: ImageVector get() = Icons.Filled.Edit
    override val home: ImageVector get() = Icons.Filled.Home
    override val info: ImageVector get() = Icons.Filled.Info
    override val menu: ImageVector get() = Icons.Filled.Menu
    override val notifications: ImageVector get() = Icons.Filled.Notifications
    override val person: ImageVector get() = Icons.Filled.Person
    override val search: ImageVector get() = Icons.Filled.Search
    override val settings: ImageVector get() = Icons.Filled.Settings
    override val star: ImageVector get() = Icons.Outlined.Star
    override val circleOutline: ImageVector get() = Icons.Outlined.Circle
    override val checkCircle: ImageVector get() = Icons.Outlined.CheckCircleOutline
    override val flagOutline: ImageVector get() = Icons.Outlined.Flag
}

fun getIconProvider(pack: IconPack): IconProvider = when (pack) {
    IconPack.MATERIAL -> MaterialIconProvider
    IconPack.FLUENT -> FluentIconProvider
    IconPack.LUCIDE -> LucideIconProvider
}

@Composable
fun LightMarkIcon(
    provider: IconProvider,
    icon: IconProvider.() -> ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    Icon(
        imageVector = icon(provider),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
