package org.dev.jesen.advancewebview.advance.config

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import org.dev.jesen.advancewebview.advance.constant.AdvanceConstants
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils
import org.dev.jesen.advancewebview.advance.helper.AdvanceReflectUtils
import org.dev.jesen.advancewebview.advance.helper.FileUtils
import org.dev.jesen.advancewebview.advance.helper.VersionUtils

/**
 * AdvanceWebView 缓存配置类（缓存策略，版本适配+缓存优化）
 * 职责：统一管理缓存类型、动态切换缓存策略、清理缓存，适配不同版本存储机制
 */
object AdvanceWebCacheConfig {

    /**
     * 初始化缓存配置（策略：分网络状态、分版本适配）
     */
    fun initCacheConfig(webView: WebView,context: Context){
        val webSettings = webView.settings
        val appCacheDir = FileUtils.getWebViewAppCacheDir(context)
        val cookieDir = FileUtils.getWebViewCookieDir(context)

        // ---------------------- 1. 基础缓存配置（全版本兼容）----------------------
        webSettings.apply {
            // 启用应用缓存（反射调用setAppCacheEnabled，兼容API 21-36）
            AdvanceReflectUtils.invokeVoidMethod(this, "setAppCacheEnabled", true)
            // 设置应用缓存路径（反射调用setAppCachePath，兼容旧版本）
            AdvanceReflectUtils.invokeVoidMethod(this, "setAppCachePath", appCacheDir)

            // 设置最大缓存大小（仅低版本生效，高版本自动管理，反射避免编译错误）
            if (!VersionUtils.isOreoOrHigher()) { // API 26以下才需要设置
                AdvanceReflectUtils.invokeVoidMethod(
                    this,
                    "setAppCacheMaxSize",
                    AdvanceConstants.WEBVIEW_MAX_CACHE_SIZE
                )
            }
            // 启用 DOM 存储缓存（H5 本地存储）
            domStorageEnabled = true
            // 启用数据库缓存
            databaseEnabled = true
            databasePath = cookieDir.absolutePath
            // 启用缓存模式（默认按网络状态切换）
            cacheMode = if (isNetworkAvailable(context)) {
                WebSettings.LOAD_DEFAULT // 有网络：缓存新数据，加载旧缓存（优化加载速度）
            } else {
                WebSettings.LOAD_CACHE_ELSE_NETWORK // 无网络：优先加载缓存，无缓存则失败（离线可用）
            }
        }

        // ---------------------- 2. Cookie 缓存配置（版本适配，持久化存储）----------------------
        val cookieManager = CookieManager.getInstance()
        cookieManager.apply {
            // 启用 Cookie
            setAcceptCookie(true)
            // Android 5.0+ 启用 Cookie 持久化
            if (VersionUtils.isLollipopOrHigher()) {
                setAcceptThirdPartyCookies(webView, false) // 禁止第三方 Cookie（安全+隐私）
                flush() // 强制写入 Cookie 到磁盘
            } else {
                @Suppress("DEPRECATION")
                setAcceptCookie(true)
            }
        }

        // ---------------------- 3. 版本适配缓存配置（针对性优化）----------------------
        // Android 10+ 适配分区存储，清理过期缓存（避免缓存溢出）
        if (VersionUtils.isQOrHigher()) {
            FileUtils.clearExpireFiles(
                FileUtils.getWebViewCacheDir(context),
                AdvanceConstants.WEBVIEW_CACHE_EXPIRE_TIME
            )
            AdvanceLogUtils.d("AdvanceWebCacheConfig", "已清理 7 天前过期缓存（Android 10.0+）")
        }

        // Android 14+ 适配缓存大小限制，避免超出系统限制
        if (VersionUtils.isUOrHigher()) {
            val cacheSize = FileUtils.getDirSize(FileUtils.getWebViewCacheDir(context))
            if (cacheSize > AdvanceConstants.WEBVIEW_MAX_CACHE_SIZE) {
                clearAppCache(context)
                AdvanceLogUtils.d("AdvanceWebCacheConfig", "缓存大小超出 100MB，已自动清理（Android 14.0+）")
            }
        }

        AdvanceLogUtils.d("AdvanceWebCacheConfig", "WebView 缓存配置初始化完成，当前缓存目录：$appCacheDir")
    }

    /**
     * 动态切换缓存策略（按网络状态）
     */
    fun switchCacheStrategy(webView: WebView, context: Context) {
        val webSettings = webView.settings
        webSettings.cacheMode = if (isNetworkAvailable(context)) {
            WebSettings.LOAD_DEFAULT
        } else {
            WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
        AdvanceLogUtils.d("AdvanceWebCacheConfig", "WebView 缓存策略已切换：${if (isNetworkAvailable(context)) "有网络模式" else "无网络离线模式"}")
    }

    /**
     * 清理所有缓存（规范：完整清理，无残留）
     */
    fun clearAllCache(webView: WebView, context: Context) {
        // 1. 清理 WebView 页面缓存
        webView.clearCache(true)
        // 2. 清理 WebView 历史记录
        webView.clearHistory()
        // 3. 清理 WebView 表单数据
        webView.clearFormData()
        // 4. 清理 Cookie 缓存
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies { success ->
            if (success) {
                cookieManager.flush()
                AdvanceLogUtils.d("AdvanceWebCacheConfig", "Cookie 缓存清理成功")
            }
        }
        // 5. 清理应用缓存目录
        clearAppCache(context)
        // 6. 清理 DOM 存储缓存
        webView.settings.domStorageEnabled = false
        webView.settings.domStorageEnabled = true

        AdvanceLogUtils.d("AdvanceWebCacheConfig", "WebView 所有缓存已清理完成")
    }

    /**
     * 清理应用缓存目录
     */
    private fun clearAppCache(context: Context) {
        val cacheDir = FileUtils.getWebViewCacheDir(context)
        val appCacheDir = FileUtils.getWebViewAppCacheDir(context)
        val cookieDir = FileUtils.getWebViewCookieDir(context)

        FileUtils.deleteDir(appCacheDir)
        FileUtils.deleteDir(cookieDir)
        // 清理过期文件，保留最新缓存（可选）
        FileUtils.clearExpireFiles(cacheDir, AdvanceConstants.WEBVIEW_CACHE_EXPIRE_TIME)

        AdvanceLogUtils.d("AdvanceWebCacheConfig", "应用缓存目录清理完成，缓存目录大小：${FileUtils.getDirSize(cacheDir) / 1024 / 1024}MB")
    }

    /**
     * 检查网络是否可用（适配 Android 10+ 网络状态判断）
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (VersionUtils.isQOrHigher()) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities != null && (
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
}