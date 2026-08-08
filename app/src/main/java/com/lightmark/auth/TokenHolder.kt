package com.lightmark.auth

import javax.inject.Singleton

/**
 * 进程内 Token 持有者，用来在 OkHttp 拦截器与 AuthManager 之间解耦。
 *
 * 问题背景：之前 AppModule 把拦截器的 tokenProvider 写死成 { null }，
 * 导致所有请求都不带 Authorization 头，GitHub 的 /user 接口直接返回 401。
 *
 * 这里用一个轻量的内存单例保存当前 token：
 * - AuthManager 在登录/恢复/登出时同步更新它；
 * - GitHubAuthInterceptor 在每次请求时读取它，把 token 放进 Authorization 头。
 */
@Singleton
class TokenHolder {
    @Volatile
    var token: String? = null
}
