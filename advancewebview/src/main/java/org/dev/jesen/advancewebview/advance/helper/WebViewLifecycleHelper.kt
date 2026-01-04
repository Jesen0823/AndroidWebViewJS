package org.dev.jesen.advancewebview.advance.helper

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import org.dev.jesen.advancewebview.advance.constant.AdvanceConstants

/**
 * WebView 生命周期管理工具
 */
class WebViewLifecycleHelper(private val webView: WebView) {

    /**
     * 与 Activity/Fragment onResume 同步
     */
    fun onResume() {
        try {
            webView.onResume()
            webView.resumeTimers()
            AdvanceLogUtils.d("AdvanceLifecycle", "WebView 已恢复运行")
        } catch (e: Exception) {
            AdvanceLogUtils.e("AdvanceLifecycle", "WebView onResume 异常：${e.message}", e)
        }
    }

    /**
     * 与 Activity/Fragment onPause 同步
     */
    fun onPause() {
        try {
            webView.onPause()
            webView.pauseTimers()
            AdvanceLogUtils.d("AdvanceLifecycle", "WebView 已暂停运行")
        } catch (e: Exception) {
            AdvanceLogUtils.e("AdvanceLifecycle", "WebView onPause 异常：${e.message}", e)
        }
    }

    /**
     * 与 Activity/Fragment onDestroy 同步（释放资源核心）
     */
    fun onDestroy() {
        try {
            // 1. 停止加载
            webView.stopLoading()
            // 2. 移除所有回调
            webView.webViewClient = AdvanceEmptyWebViewClient()
            webView.webChromeClient = AdvanceEmptyWebChromeClient()
            // 3. 移除 JS 桥接
            webView.removeJavascriptInterface(AdvanceConstants.JS_BRIDGE_NAME)
            // 4. 清空历史记录
            webView.clearHistory()
            // 5. 停止所有定时器和线程
            webView.pauseTimers()
            // 6. 移除所有视图
            (webView.parent as? ViewGroup)?.removeView(webView)
            AdvanceLogUtils.d("AdvanceLifecycle", "WebView 已释放核心资源")
        } catch (e: Exception) {
            AdvanceLogUtils.e("AdvanceLifecycle", "WebView onDestroy 异常：${e.message}", e)
        }
    }

    /**
     * 空实现 WebViewClient（避免内存泄漏）
     */
    private class AdvanceEmptyWebViewClient : WebViewClient()

    /**
     * 空实现 WebChromeClient（避免内存泄漏）
     */
    private class AdvanceEmptyWebChromeClient : WebChromeClient()
}