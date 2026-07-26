package com.lightmark.domain.model

import kotlinx.serialization.Serializable

/**
 * GitHub 用户信息
 */
@Serializable
data class GitHubUser(
    val login: String,
    val id: Int,
    val name: String? = null,
    val avatarUrl: String? = null,
    val email: String? = null,
    val bio: String? = null
)
