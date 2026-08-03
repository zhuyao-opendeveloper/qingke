package com.lightmark.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.lightmark.ui.screens.ai.AiChatScreen
import com.lightmark.ui.screens.categories.CategoriesScreen
import com.lightmark.ui.screens.home.HomeScreen
import com.lightmark.ui.screens.home.HomeViewModel
import com.lightmark.ui.screens.settings.SettingsScreen
import com.lightmark.ui.screens.stats.StatsScreen
import com.lightmark.ui.screens.todo.AddEditTodoScreen
import com.lightmark.icons.IconProvider
import com.lightmark.icons.LightMarkIcon

/**
 * 轻刻导航路由定义
 */
object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val AI = "ai"
    const val CATEGORIES = "categories"
    const val ADD_TODO = "add_todo"
    const val EDIT_TODO = "edit_todo/{todoId}"
    const val SETTINGS = "settings"

    fun editTodo(todoId: String) = "edit_todo/$todoId"
}

/**
 * 轻刻主导航容器
 *
 * 使用 Jetpack Navigation Compose，页面间切换带滑动 + 淡入淡出动画。
 * 底部导航栏：待办 / 统计 / AI / 设置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightMarkNavHost(
    initialRoute: String,
    userId: String
) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // 底部导航栏展示的页面
    val showBottomBar = currentRoute in listOf(Routes.HOME, Routes.STATS, Routes.AI)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                LightMarkBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.HOME) {
                LargeFloatingActionButton(
                    onClick = {
                        navController.navigate(Routes.ADD_TODO)
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    com.lightmark.icons.LightMarkIcon(
                        provider = homeViewModel.currentIconProvider,
                        icon = { add },
                        contentDescription = "添加待办"
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it / 3 } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = homeViewModel,
                    userName = userId,
                    onNavigateToAdd = { navController.navigate(Routes.ADD_TODO) },
                    onNavigateToEdit = { todoId ->
                        navController.navigate(Routes.editTodo(todoId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Routes.SETTINGS)
                    },
                    onNavigateToAi = {
                        navController.navigate(Routes.AI)
                    }
                )
            }

            composable(Routes.STATS) {
                StatsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.AI) {
                AiChatScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ADD_TODO) {
                AddEditTodoScreen(
                    todoId = null,
                    iconProvider = homeViewModel.currentIconProvider,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.EDIT_TODO,
                arguments = listOf(navArgument("todoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val todoId = backStackEntry.arguments?.getString("todoId") ?: return@composable
                AddEditTodoScreen(
                    todoId = todoId,
                    iconProvider = homeViewModel.currentIconProvider,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCategories = { navController.navigate(Routes.CATEGORIES) }
                )
            }
        }
    }
}

/**
 * 轻刻底部导航栏
 *
 * Material 3 NavigationBar，圆角设计，悬浮在屏幕底部。
 */
@Composable
fun LightMarkBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        BottomItem(
            selected = currentRoute == Routes.HOME,
            onClick = { onNavigate(Routes.HOME) },
            icon = { LightMarkIcon(provider = com.lightmark.icons.MaterialIconProvider, icon = { home }, contentDescription = "待办") },
            label = "待办"
        )
        BottomItem(
            selected = currentRoute == Routes.STATS,
            onClick = { onNavigate(Routes.STATS) },
            icon = { Icon(Icons.Filled.BarChart, contentDescription = "统计") },
            label = "统计"
        )
        BottomItem(
            selected = currentRoute == Routes.AI,
            onClick = { onNavigate(Routes.AI) },
            icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary) },
            label = "AI"
        )
        BottomItem(
            selected = currentRoute == Routes.SETTINGS,
            onClick = { onNavigate(Routes.SETTINGS) },
            icon = { LightMarkIcon(provider = com.lightmark.icons.MaterialIconProvider, icon = { settings }, contentDescription = "设置") },
            label = "设置"
        )
    }
}

@Composable
private fun RowScope.BottomItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = { Text(label) }
    )
}
