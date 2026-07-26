package com.lightmark.data.remote

import com.lightmark.data.remote.dto.GitHubContentDto
import com.lightmark.data.remote.dto.GitHubRepoDto
import com.lightmark.data.remote.dto.GitHubUserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * GitHub API 服务接口
 * 使用 GitHub REST API v3
 *
 * 主要功能：
 * - 验证 Token 有效性
 * - 读写用户私有仓库中的数据文件
 */
interface GitHubApiService {

    /**
     * 获取当前认证用户信息（用于验证 Token 有效性）
     */
    @GET("user")
    suspend fun getCurrentUser(): GitHubUserDto

    /**
     * 获取指定仓库信息
     */
    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRepoDto

    /**
     * 检查仓库是否存在（HEAD 请求）
     */
    @HEAD("repos/{owner}/{repo}")
    suspend fun checkRepositoryExists(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): retrofit2.Response<Unit>

    /**
     * 创建或更新文件（GitHub Contents API）
     *
     * @param owner 仓库所有者
     * @param repo 仓库名
     * @param path 文件路径
     * @param body 请求体（包含 message、content 等）
     */
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String = "main",
        @Body body: GitHubContentRequestDto
    ): GitHubContentResponseDto

    /**
     * 获取文件内容
     */
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String = "main"
    ): GitHubContentDto
}

/**
 * 创建/更新文件的请求体
 */
data class GitHubContentRequestDto(
    val message: String,           // commit message
    val content: String,           // Base64 编码的内容
    val sha: String? = null,       // 文件当前的 sha（更新时需要）
    val branch: String = "main"
)

/**
 * 文件操作响应
 */
data class GitHubContentResponseDto(
    val content: GitHubContentDto?,
    val commit: GitHubCommitDto?
)

data class GitHubCommitDto(
    val sha: String,
    val message: String,
    val date: String?
)
