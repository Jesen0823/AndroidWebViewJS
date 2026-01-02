package org.dev.jesen.androidwebviewjs.web.client

import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import org.dev.jesen.androidwebviewjs.core.utils.LogUtils

/**
 * 自定义 WebViewClient（基础版）
 * 职责：处理页面加载状态、URL 拦截、资源加载等逻辑
 */
open class CustomWebViewClient : WebViewClient() {
    /**
     * 页面开始加载时回调
     */
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        LogUtils.d("CustomWebViewClient", "onPageStarted, url:$url")
        // 可在此处显示加载动画
    }

    /**
     * 页面加载完成时回调
     */
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        LogUtils.d("CustomWebViewClient", "onPageFinished：$url")
        // 可在此处隐藏加载动画、执行 JS 注入
    }

    /**
     * 拦截 URL 加载（拦截自定义 URL 协议，实现原生-JS互调（旧方案））
     */
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return super.shouldOverrideUrlLoading(view, request)
        LogUtils.d("CustomWebViewClient", "shouldOverrideUrlLoading, 拦截 URL：$url")
        // 拦截自定义协议（如：android://xxx）
        if (url.startsWith("android://")) {
            // 处理自定义协议逻辑（后续阶段扩展）
            return true
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    /**
     * 页面加载失败时回调
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        val errorMsg = error?.description ?: "未知错误"
        LogUtils.e("CustomWebViewClient", "页面加载失败：$errorMsg")
        // 可在此处显示错误页面
    }
}