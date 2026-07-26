package com.lightmark.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lightmark.ui.screens.home.HomeScreen
import com.lightmark.ui.screens.home.HomeViewModel
import com.lightmark.ui.screens.settings.SettingsScreen
import com.lightmark.ui.screens.todo.AddEditTodoScreen

/**
 * 轻刻导航路由定义
 */
object Routes {
    const val HOME = "home"
    const val ADD_TODO = "add_todo"
    const val EDIT_TODO = "edit_todo/{todoId}"
    const val SETTINGS = "settings"

    fun editTodo(todoId: String) = "edit_todo/$todoId"
}

/**
 * 轻刻主导航组件
 *
 * 使用 Jetpack Navigation Compose
 * 支持页面间动画过渡
 * 底部导航栏（主页/设置）
 *
 * @param initialRoute 初始路由
 * @param userId 当前用户 ID
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

    // 显示底部导航栏的页面
    val showBottomBar = currentRoute in listOf(Routes.HOME)

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
                    onNavigateToAdd = { navController.navigate(Routes.ADD_TODO) },
                    onNavigateToEdit = { todoId ->
                        navController.navigate(Routes.editTodo(todoId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Routes.SETTINGS)
                    }
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
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * 轻刻底部导航栏
 *
 * Material 3 NavigationBar
 * 圆角设计，悬浮在屏幕底部
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
        // 首页
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = { onNavigate(Routes.HOME) },
            icon = {
                Icon(
                    imageVector = if (currentRoute == Routes.HOME)
                        com.lightmark.icons.MaterialIconProvider.home
                    else
                        com.lightmark.icons.MaterialIconProvider.home,
                    contentDescription = "首页"
                )
            },
            label = { Text("待办") }
        )

        // 设置
        NavigationBarItem(
            selected = currentRoute == Routes.SETTINGS,
            onClick = { onNavigate(Routes.SETTINGS) },
            icon = {
                Icon(
                    imageVector = com.lightmark.icons.MaterialIconProvider.settings,
                    contentDescription = "设置"
                )
            },
            label = { Text("设置") }
        )
    }
}
