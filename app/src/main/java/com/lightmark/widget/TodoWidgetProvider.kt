package com.lightmark.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lightmark.MainActivity
import com.lightmark.R
import com.lightmark.data.local.LightMarkDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 轻刻桌面小部件（#63，纯离线）
 *
 * 仅展示「未完成的活跃待办数量」并提供一个「新建待办」按钮，
 * 不依赖任何网络；数据直接读取本机 Room 数据库（与 App 共用同一实例）。
 *
 * 注意：RemoteViews 不支持 Jetpack Compose，小部件使用传统 View 布局。
 */
class TodoWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onEnabled(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, TodoWidgetProvider::class.java))
        onUpdate(context, mgr, ids)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_todo)

        // 点击标题 / 数量 → 打开应用首页
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                action = "com.lightmark.action.OPEN"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, openApp)
        views.setOnClickPendingIntent(R.id.widget_count, openApp)

        // 点击「新建待办」→ 直接打开新建待办页
        val addIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.lightmark.action.NEW_TODO"
        }
        val addPi = PendingIntent.getActivity(
            context,
            1,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_add, addPi)

        // 先放占位，再异步拉取真实数量
        views.setTextViewText(R.id.widget_count, "…")
        appWidgetManager.updateAppWidget(appWidgetId, views)

        scope.launch {
            try {
                val db = LightMarkDatabase.create(context)
                val all = db.todoDao().getAllTodosList()
                val pending = all.count { !it.isCompleted && !it.isDeleted && !it.isArchived }
                val text = if (pending == 0) "暂无待办 ✓" else "待办 $pending 条"
                views.setTextViewText(R.id.widget_count, text)
            } catch (e: Exception) {
                views.setTextViewText(R.id.widget_count, "—")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
