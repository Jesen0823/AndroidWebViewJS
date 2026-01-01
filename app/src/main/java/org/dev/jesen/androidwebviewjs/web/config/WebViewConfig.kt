package org.dev.jesen.androidwebviewjs.web.config

import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import org.dev.jesen.androidwebviewjs.BuildConfig
import org.dev.jesen.androidwebviewjs.core.constants.WebConstants
import org.dev.jesen.androidwebviewjs.core.utils.LogUtils

/**
 * WebView 核心属性配置类
 * 职责：统一配置 WebView 属性，解耦视图与配置逻辑
 */
object WebViewConfig {

    /**
     * 初始化 WebView 基础配置
     */
    fun initBasicConfig(webView: WebView) {
        val webSettings = webView.settings
        webSettings.apply {
            // 1. 基础配置
            allowFileAccess = false // 禁用文件访问（安全考虑，默认false）
            allowFileAccessFromFileURLs = false // 禁用文件URL访问其他文件
            allowUniversalAccessFromFileURLs = false // 禁用文件URL访问任意资源
            setSupportZoom(true) // 支持缩放
            builtInZoomControls = true // 启用内置缩放控件
            displayZoomControls = false // 隐藏缩放控件，出于美观考虑
            defaultTextEncodingName = "UTF-8" // 设置默认编码格式
            loadWithOverviewMode = true // 支持宽视图
            layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN // 单列布局（适配手机）

            // 2. JS 配置（基础：启用 JS，支持与原生互调）
            javaScriptEnabled = true // 启用 JavaScript（必要时开启，默认关闭）
            javaScriptCanOpenWindowsAutomatically = true // 允许 JS 打开新窗口
            // 验证：打印JS权限配置日志
            LogUtils.d("WebViewConfig", "JS执行权限是否开启：${webSettings.javaScriptEnabled}")
            LogUtils.d("WebViewConfig", "JS是否允许打开新窗口：${webSettings.javaScriptCanOpenWindowsAutomatically}")

            // 3. 缓存配置（基础：启用应用缓存，兼容高低版本）
            cacheMode = WebSettings.LOAD_DEFAULT
            configureAppCache(webSettings, webView.context)

            // 4. 其他配置（优化）
            loadsImagesAutomatically = true // 自动加载图片（默认开启，关闭可提升首屏加载速度）
            blockNetworkImage = false // 不阻塞网络图片加载
            mediaPlaybackRequiresUserGesture = false // 允许自动播放媒体（按需配置）
        }

        // 5. WebView 调试（仅调试版开启）
        if (BuildConfig.ENABLE_WEBVIEW_DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
            LogUtils.d("WebViewConfig", "WebView 调试模式已开启")
        }

        // 6. 适配 Android 版本（基础）
        adaptAndroidVersion(webSettings)
    }

    /**
     * 单独抽离应用缓存配置（解决 Unresolved reference 问题，兼容高低版本）
     */
    @Suppress(
        "DEPRECATION",
        "UNRESOLVED_REFERENCE",
        "UNUSED_VARIABLE"
    ) // 抑制废弃API警告（注废弃API使用）
    private fun configureAppCache(
        webSettings: WebSettings,
        context: Context
    ) {
        // ------------- 旧 API 强制调用（仅用于兼容 Android 8.0 以下，高版本自动失效）-------------
        // 1. 启用应用缓存（正确：带参成员方法，系统原生 WebSettings 才支持）
        try {
            // 反射调用（终极兜底：避免 IDE 编译报错，兼容所有版本）
            val setAppCacheEnabledMethod = WebSettings::class.java.getMethod(
                "setAppCacheEnabled",
                Boolean::class.javaPrimitiveType
            )
            setAppCacheEnabledMethod.invoke(webSettings, true)

            // 2. 配置缓存目录（appCachePath 属性）
            // 缓存目录使用 context.cacheDir（应用内部缓存目录，无需动态权限，兼容 Android 10+ 分区存储），避免使用外部存储导致权限问题
            val cacheDir = context.cacheDir.absolutePath + "/" + WebConstants.WEBVIEW_CACHE_DIR
            val appCachePathField = WebSettings::class.java.getField("appCachePath")
            appCachePathField.set(webSettings, cacheDir)

            // 3. 配置最大缓存大小（仅 Android 4.3 以下有效，高版本忽略）
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                val appCacheMaxSizeField = WebSettings::class.java.getField("appCacheMaxSize")
                appCacheMaxSizeField.set(webSettings, WebConstants.WEBVIEW_MAX_CACHE_SIZE)
            }

            LogUtils.d("WebViewConfig", "应用缓存配置完成（反射兜底），缓存目录：$cacheDir")
        } catch (e: Exception) {
            // 捕获反射异常（高版本 SDK 无此方法/属性），不影响核心功能
            LogUtils.w("WebViewConfig", "应用缓存配置失败（高版本 SDK 不支持旧 API）：${e.message}")
            LogUtils.d(
                "WebViewConfig",
                "WebView 高版本中，应用缓存的功能被整合到默认缓存策略中，缓存目录由系统自动分配，开发者仅需关注 cacheMode 配置即可"
            )
        }
    }

    /**
     * Android 版本适配（基础）
     * 处理不同 Android 版本 WebView 的差异
     */
    private fun adaptAndroidVersion(webSettings: WebSettings) {
        // Android 7.0+ 禁用混合内容（仅允许 HTTPS，安全考虑）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        // Android 9.0+ 启用安全浏览
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            webSettings.safeBrowsingEnabled = true
        }
    }
}

