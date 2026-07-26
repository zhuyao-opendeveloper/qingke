package com.lightmark.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub 用户信息 DTO
 */
@Serializable
data class GitHubUserDto(
    val login: String,
    val id: Int,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val email: String? = null,
    val bio: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null
)
