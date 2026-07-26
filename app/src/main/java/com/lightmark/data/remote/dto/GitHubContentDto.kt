package com.lightmark.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Contents API 文件内容 DTO
 */
@Serializable
data class GitHubContentDto(
    val name: String,
    val path: String,
    val sha: String,
    val size: Int,
    val url: String,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("git_url") val gitUrl: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    val type: String = "file",
    val content: String? = null,    // Base64 编码的文件内容
    val encoding: String? = null    // 编码方式，通常为 "base64"
)
