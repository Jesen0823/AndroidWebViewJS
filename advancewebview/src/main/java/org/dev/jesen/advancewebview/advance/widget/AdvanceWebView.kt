package org.dev.jesen.advancewebview.advance.widget

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import org.dev.jesen.advancewebview.advance.bridge.AdvanceJsBridge
import org.dev.jesen.advancewebview.advance.bridge.AdvanceJsBridgeHelper
import org.dev.jesen.advancewebview.advance.bridge.AdvanceNativeCallJsManager
import org.dev.jesen.advancewebview.advance.client.AdvanceWebChromeClient
import org.dev.jesen.advancewebview.advance.client.AdvanceWebViewClient
import org.dev.jesen.advancewebview.advance.config.AdvanceWebCacheConfig
import org.dev.jesen.advancewebview.advance.config.AdvanceWebSecurityConfig
import org.dev.jesen.advancewebview.advance.config.AdvanceWebViewConfig
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils
import org.dev.jesen.advancewebview.advance.helper.AdvanceThreadHelper
import org.dev.jesen.advancewebview.advance.helper.VersionUtils
import org.dev.jesen.advancewebview.advance.helper.WebViewLifecycleHelper
import org.dev.jesen.advancewebview.advance.inject.AdvanceJsInjectManager

/**
 * WebView 核心控件（高度封装，一站式解决方案，独立无依赖）
 * 整合：版本适配+性能优化+缓存配置+安全防御+JS注入+原生-JS互调
 * 对外暴露简洁 API，隐藏内部实现，具备高度可扩展性和企业级规范性
 */
class AdvanceWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {
    // 桥接相关（对外隐藏，内部管理）
    lateinit var jsBridge: AdvanceJsBridge
    lateinit var nativeCallJsManager: AdvanceNativeCallJsManager
    private var onJsCallNativeListener: AdvanceJsBridge.OnJsCallNativeListener? = null

    // 生命周期管理（性能优化，避免内存泄漏）
    val lifecycleHelper: WebViewLifecycleHelper by lazy {
        WebViewLifecycleHelper(this)
    }

    init {
        // 1. 初始化核心配置（版本+性能+缓存+安全）
        initCoreConfig()
        // 2. 设置自定义客户端（回调+安全+缓存）
        setCustomClients()
        // 3. 启用硬件加速（性能优化，版本适配）
        enableHardwareAcceleration()
    }

    /**
     * 初始化核心配置（一站式配置，对外隐藏细节）
     */
    private fun initCoreConfig() {
        AdvanceWebViewConfig.initCoreConfig(this, context)
        AdvanceWebCacheConfig.initCacheConfig(this, context)
        AdvanceWebSecurityConfig.initSecurityConfig(this, context)
        AdvanceLogUtils.d("AdvanceWebView", "核心配置初始化完成（兼容 Android 5.0+ 到 16.0）")
    }

    /**
     * 设置自定义客户端（回调处理，安全防御）
     */
    private fun setCustomClients() {
        this.webViewClient = AdvanceWebViewClient()
        this.webChromeClient = AdvanceWebChromeClient()
        AdvanceLogUtils.d("AdvanceWebView", "自定义客户端设置完成")
    }

    /**
     * 启用硬件加速（性能优化，版本适配）
     */
    private fun enableHardwareAcceleration() {
        if (VersionUtils.isLollipopOrHigher()) {
            this.setLayerType(LAYER_TYPE_HARDWARE, null)
            AdvanceLogUtils.d("AdvanceWebView", "硬件加速已启用（Android 5.0+）")
        } else {
            this.setLayerType(LAYER_TYPE_SOFTWARE, null)
            AdvanceLogUtils.d("AdvanceWebView", "软件渲染已启用（低版本兼容）")
        }
    }

    /**
     * 初始化 JS 桥接（对外暴露，设置回调监听）
     */
    fun initJsBridge(listener: AdvanceJsBridge.OnJsCallNativeListener) {
        this.onJsCallNativeListener = listener
        val bridgePair = AdvanceJsBridgeHelper.initBridge(this, listener)
        this.jsBridge = bridgePair.first
        this.nativeCallJsManager = bridgePair.second
        AdvanceLogUtils.d("AdvanceWebView", "JS 桥接初始化完成")
    }

    /**
     * 加载页面（统一入口，安全校验，缓存适配）
     */
    fun loadAdvancePage(url: String) {
        // 1. 安全校验：拦截非法 URL
        if (!AdvanceWebSecurityConfig.checkUrlSafety(url)) {
            AdvanceLogUtils.e("AdvanceWebView", "页面加载失败：非法 URL $url")
            return
        }

        // 2. 加载页面（按 URL 类型处理）
        this.loadUrl(url)
        AdvanceLogUtils.d("AdvanceWebView", "页面加载中：$url")
    }

    /**
     * 静态注入 JS（全局工具类，页面开始加载时调用）
     */
    fun injectGlobalToolJs() {
        AdvanceJsInjectManager.injectGlobalToolJs(this)
        AdvanceLogUtils.d("AdvanceWebView", "全局工具类 JS 注入完成")
    }

    /**
     * 动态注入 JS（业务逻辑，页面加载完成时调用）
     */
    fun injectBusinessJs(businessData: String) {
        val safeData = AdvanceWebSecurityConfig.filterXssContent(businessData)
        AdvanceJsInjectManager.injectBusinessJs(this, safeData)
        AdvanceLogUtils.d("AdvanceWebView", "业务逻辑 JS 注入完成")
    }

    /**
     * 清理所有缓存（对外暴露 API，企业级规范）
     */
    fun clearAllAdvanceCache() {
        AdvanceThreadHelper.runOnMainThread(context) {
            AdvanceWebCacheConfig.clearAllCache(this, context)
        }
        AdvanceLogUtils.d("AdvanceWebView", "所有缓存已清理完成")
    }

    /**
     * 切换图片加载状态（对外暴露 API，性能优化）
     */
    fun toggleImageLoading(enable: Boolean) {
        AdvanceWebViewConfig.toggleImageLoading(this, enable)
    }

    /**
     * 销毁 WebView（对外暴露 API，释放所有资源，避免内存泄漏）
     */
    fun destroyAdvanceWebView() {
        // 1. 移除桥接
        AdvanceJsBridgeHelper.removeBridge(this)
        // 2. 生命周期管理销毁
        lifecycleHelper.onDestroy()
        // 3. 移除所有视图
        this.removeAllViews()
        // 4. 销毁 WebView
        this.destroy()
        AdvanceLogUtils.d("AdvanceWebView", "WebView 已销毁，所有资源释放完成")
    }
}