package org.dev.jesen.androidwebviewjs.core.helpers

import android.webkit.WebView
import org.dev.jesen.androidwebviewjs.core.utils.LogUtils

/**
 * WebView 生命周期管理工具类
 * 职责：统一同步 WebView 与 Activity/Fragment 的生命周期，释放相关资源，避免内存泄漏
 * 解耦 WebView 生命周期管理与页面业务，提供简洁可复用的 API
 */
class WebViewLifecycleHelper(private val webView: WebView) {
    // 标记是否已销毁，避免重复释放资源
    private var isDestroyed = false

    /**
     * 获取当前销毁状态（供上层页面判断，避免非法操作）
     */
    fun isWebViewDestroyed(): Boolean = isDestroyed

    /**
     * 对应 Activity/Fragment 的 onResume()
     * 恢复 WebView 的正常运行状态，继续执行定时器、JS 等逻辑
     */
    fun onResume() {
        if (isDestroyed || webView == null) {
            LogUtils.w("WebViewLifecycleHelper", "WebView 已销毁或为空，无法执行 onResume")
            return
        }
        try {
            // 恢复当前 WebView 的运行（Android 4.4+ 支持）
            webView.onResume()
            // 恢复 WebView 全局定时器（影响所有 WebView 实例，优化后台性能）
            webView.onResume()
            LogUtils.d("WebViewLifecycleHelper", "WebView 生命周期：onResume 执行完成")
        } catch (e: Exception) {
            LogUtils.e("WebViewLifecycleHelper", "WebView onResume 异常：${e.message}", e)
        }
    }

    /**
     * 对应 Activity/Fragment 的 onPause()
     * 暂停 WebView 的运行，减少后台资源消耗，避免不必要的逻辑执行
     */
    fun onPause() {
        if (isDestroyed || webView == null) {
            LogUtils.w("WebViewLifecycleHelper", "WebView 已销毁或为空，无法执行 onPause")
            return
        }
        try {
            // 暂停当前 WebView 的运行，停止 JS 执行、页面渲染等
            webView.onPause()
            // 暂停 WebView 全局定时器，优化后台内存与性能（全局生效，需与 resumeTimers 配对）
            webView.pauseTimers()
            // 停止页面加载（若有正在进行的网络请求或资源加载，立即终止）
            webView.stopLoading()
            LogUtils.d("WebViewLifecycleHelper", "WebView 生命周期：onPause 执行完成")
        } catch (e: Exception) {
            LogUtils.e("WebViewLifecycleHelper", "WebView onPause 异常：${e.message}", e)
        }
    }

    /**
     * 对应 Activity/Fragment 的 onDestroy()
     * 彻底释放 WebView 相关资源，杜绝内存泄漏
     */
    fun onDestroy() {
        if (isDestroyed) {
            LogUtils.w("WebViewLifecycleHelper", "WebView 已执行过销毁逻辑，无需重复执行")
            return
        }
        try {
            // 1. 若 WebView 不为空，执行深层资源释放
            webView.run {
                // 停止所有加载与定时器
                stopLoading()
                webView.stopLoading()
                // 移除所有 JS 接口，避免悬空引用导致内存泄漏
                removeJavascriptInterface("AndroidJsBridge") // 与桥接名称保持一致，可扩展为参数
                // 清空 WebView 内容，解除页面引用
                loadUrl("about:blank")
                // 移除所有子视图，释放布局相关资源
                removeAllViews()
                // 关闭 WebView 内部的绘图线程
                destroyDrawingCache()
            }
            // 2. 暂停全局定时器（确保无残留资源消耗）
            webView.pauseTimers()
            // 3. 标记已销毁，防止重复调用
            isDestroyed = true
            LogUtils.d(
                "WebViewLifecycleHelper",
                "WebView 生命周期：onDestroy 执行完成，资源已彻底释放"
            )
        } catch (e: Exception) {
            LogUtils.e("WebViewLifecycleHelper", "WebView onDestroy 异常：${e.message}", e)
        }
    }
}