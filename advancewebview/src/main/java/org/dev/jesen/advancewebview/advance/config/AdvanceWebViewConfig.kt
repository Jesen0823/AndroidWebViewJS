package org.dev.jesen.advancewebview.advance.config

import android.content.Context
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import org.dev.jesen.advancewebview.BuildConfig
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils
import org.dev.jesen.advancewebview.advance.helper.AdvanceReflectUtils
import org.dev.jesen.advancewebview.advance.helper.FileUtils
import org.dev.jesen.advancewebview.advance.helper.VersionUtils

/**
 * AdvanceWebView 基础配置类（版本适配+性能优化核心）
 * 职责：统一配置 WebView 基础属性，处理不同版本差异，优化渲染/加载性能
 */
object AdvanceWebViewConfig {

    /**
     * 初始化核心配置（版本适配+性能优化）
     */
    fun initCoreConfig(webView: WebView, context: Context){
        val webSettings: WebSettings = webView.settings
        val appCacheDir: String = FileUtils.getWebViewAppCacheDir(context).absolutePath

        // ---------------------- 1. 基础配置（全版本兼容）----------------------
        webSettings.apply {
            // 编码格式
            defaultTextEncodingName = "UTF-8"
            // 屏幕适配（性能优化：减少性能开销）
            loadWithOverviewMode = true
            useWideViewPort = true
            // 单列布局，避免多列渲染卡顿
            layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            // 缩放配置（隐藏缩放控件，优化视觉+性能）
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            // 资源加载（性能优化：延迟加载图片，提升首屏速度）
            loadsImagesAutomatically = true
            // 有网络时不阻塞图片，无网络时手动关闭（缓存策略中处理）
            blockNetworkImage = false
        }

        // ---------------------- 2. JS 配置（支持注入+互调，版本适配）----------------------
        webSettings.apply {
            // 启用 JS（按需开启，安全防御中做权限控制）
            javaScriptEnabled = true
            // 禁止 JS 自动开窗（安全+性能）
            javaScriptCanOpenWindowsAutomatically = false
            // Android 7.0+ 禁止 JS 访问本地资源（安全防御）
            if(VersionUtils.isNougatOrHigher()){
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs =false
            }
            // Android 5.0+ 启用 JS 调试（仅调试版）
            if(VersionUtils.isLollipopOrHigher() && BuildConfig.ENABLE_ADVANCE_DEBUG){
                WebView.setWebContentsDebuggingEnabled(true)
                AdvanceLogUtils.d("AdvanceWebViewConfig", "WebView JS 调试已启用（Android 5.0+）")
            }
        }

        // ---------------------- 3. 版本适配配置（针对性处理不同Android版本）----------------------
        webSettings.apply {
            // Android 5.0+ 启用硬件加速（性能优化：提升渲染速度）
            if(VersionUtils.isLollipopOrHigher()){
                webView.setLayerType(View.LAYER_TYPE_HARDWARE,null)
                // 启用混合内容临时兼容（Android 5.0-6.0，高版本在安全配置中禁止）
                if (!VersionUtils.isNougatOrHigher()) {
                    @Suppress("DEPRECATION")
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
            }else {
                // Android 4.4- 启用软件渲染（避免硬件加速兼容问题）
                webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            }

            // Android 8.0+ 启用安全浏览（安全+版本适配）
            if (VersionUtils.isOreoOrHigher()) {
                safeBrowsingEnabled = true
                AdvanceLogUtils.d("AdvanceWebViewConfig", "WebView 安全浏览已启用（Android 8.0+）")
            }

            // Android 9.0+ 禁止明文流量（HTTPS 优先，安全+版本适配）
            if (VersionUtils.isPieOrHigher()) {
                @Suppress("DEPRECATION")
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                AdvanceLogUtils.d("AdvanceWebViewConfig", "WebView 禁止明文流量（Android 9.0+，HTTPS 优先）")
            }

            // API 21-29 反射配置 AppCache（Android 5.0-10.0）
            if (VersionUtils.isLollipopOrHigher() && !VersionUtils.isQOrHigher()) {
                initLowVersionAppCache(webSettings, appCacheDir)
            }

            // API 30+（Android 11+）：无需配置AppCache，采用现代缓存方案
            if (VersionUtils.isQOrHigher()) {
                cacheMode = WebSettings.LOAD_DEFAULT
                AdvanceLogUtils.d("AdvanceWebViewConfig", "WebView 采用系统默认缓存（Android 11+，废弃AppCache）")
            }
            // Android 14.0+ 适配缓存大小限制（targetSdk 36，版本适配）
            if (VersionUtils.isUOrHigher()) {
                // API 33+ 推荐使用WebSettings的缓存管理，而非手动设置大小
                // 若需限制缓存，可通过WebView的缓存目录手动清理（替代废弃的setAppCacheMaxSize）
                AdvanceLogUtils.d("AdvanceWebViewConfig", "WebView 缓存由系统自动管理（Android 14.0+）")
            }
        }

        // ---------------------- 4. 性能优化进阶配置----------------------
        // 禁用不必要的功能，减少资源消耗
        webSettings.apply {
            // 禁用地理定位（无需时关闭，减少权限请求+资源消耗）
            setGeolocationEnabled(false)
            // 禁用媒体自动播放（减少内存+流量消耗）
            mediaPlaybackRequiresUserGesture = true
            // 启用快速加载（跳过无关资源，提升首屏速度）
            if (VersionUtils.isLollipopOrHigher()) {
                // 启用硬件加速已在上方配置，补充其他性能优化
                setRenderPriority(WebSettings.RenderPriority.HIGH)
            }
        }

        AdvanceLogUtils.d("AdvanceWebViewConfig", "WebView 核心配置初始化完成（适配 Android 5.0+ 到 16.0）")
    }

    /**
     * 反射初始化低版本（API 21-29）AppCache配置（兼容核心）
     * 仅在Android 5.0-10.0中调用，反射失败则跳过，不影响核心流程
     */
    private fun initLowVersionAppCache(
        webSettings: WebSettings,
        appCacheDir: String
    ) {
        AdvanceLogUtils.d("AdvanceWebViewConfig", "开始反射配置低版本AppCache，缓存目录：$appCacheDir")

        // 1. 反射调用 setAppCacheEnabled(true)
        val enableAppCacheSuccess = AdvanceReflectUtils.invokeVoidMethod(
            obj = webSettings,
            methodName = "setAppCacheEnabled",
            params = arrayOf(true)
        )

        // 2. 反射调用 setAppCachePath(appCacheDir)
        val setAppCachePathSuccess = AdvanceReflectUtils.invokeVoidMethod(
            obj = webSettings,
            methodName = "setAppCachePath",
            params = arrayOf(appCacheDir)
        )

        // 3. 兼容结果日志（需记录，便于后续排查兼容问题）
        if (enableAppCacheSuccess && setAppCachePathSuccess) {
            AdvanceLogUtils.d("AdvanceWebViewConfig", "低版本AppCache配置成功（反射调用）")
        } else {
            AdvanceLogUtils.w("AdvanceWebViewConfig", "低版本AppCache配置失败，将使用默认缓存策略")
        }
    }

    /**
     * 切换图片加载状态（性能优化：无网络时关闭图片加载）
     */
    fun toggleImageLoading(webView: WebView, enable: Boolean) {
        webView.settings.apply {
            loadsImagesAutomatically = enable
            blockNetworkImage = !enable
        }
        AdvanceLogUtils.d("AdvanceWebViewConfig", "WebView 图片加载状态：${if (enable) "启用" else "禁用"}")
    }

    /**
     * 启用/禁用 JS（安全防御：按需开启）
     */
    fun toggleJavascript(webView: WebView, enable: Boolean) {
        webView.settings.javaScriptEnabled = enable
        AdvanceLogUtils.d("AdvanceWebViewConfig", "WebView JS 状态：${if (enable) "启用" else "禁用"}")
    }

    /**
     * 启用 WebView 焦点管理（确保输入元素能获取焦点和软键盘输入）
     * @param enable 是否启用焦点管理
     */
    fun enableFocusManagement(webView: WebView, enable: Boolean) {
        if (enable) {
            webView.apply {
                // 设置WebView可以获取焦点，但不强制拦截触摸事件
                isFocusable = true
                isFocusableInTouchMode = true
                
                // 移除之前的触摸事件监听器，让WebView默认处理焦点
                // WebView会自动在点击输入元素时获取焦点和弹出软键盘
                setOnTouchListener(null)
                
                // 键盘事件处理：让WebView优先处理
                setOnKeyListener { v, keyCode, event -> false }
            }
            AdvanceLogUtils.d("AdvanceWebViewConfig", "WebView 焦点管理已启用")
        } else {
            webView.apply {
                isFocusable = false
                isFocusableInTouchMode = false
            }
            AdvanceLogUtils.d("AdvanceWebViewConfig", "WebView 焦点管理已禁用")
        }
    }
}