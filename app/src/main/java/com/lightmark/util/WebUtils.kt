package com.lightmark.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 轻刻网页版地址（GitHub Pages 托管，含云端同步与 AI 对话能力）。
 *
 * 轻刻 App 本身为纯单机应用（不联网、不声明 INTERNET 权限），
 * 需要跨设备同步或 AI 对话时跳转到网页版，由系统浏览器打开。
 */
const val LIGHTMARK_WEB_URL = "https://zhuyao-opendeveloper.github.io/lightmark-web/"

/**
 * 使用系统浏览器打开轻刻网页版。
 * App 自身不发起任何网络请求，仅把 URL 交给系统浏览器处理。
 */
fun openLightMarkWeb(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LIGHTMARK_WEB_URL))
    context.startActivity(intent)
}
