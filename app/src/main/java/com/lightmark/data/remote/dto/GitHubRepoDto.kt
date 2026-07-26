package com.lightmark.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub 仓库信息 DTO
 */
@Serializable
data class GitHubRepoDto(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("private") val isPrivate: Boolean = false,
    val description: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("default_branch") val defaultBranch: String = "main"
)
