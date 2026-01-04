package org.dev.jesen.advancewebview.advance.client

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.dev.jesen.advancewebview.advance.config.AdvanceWebCacheConfig
import org.dev.jesen.advancewebview.advance.config.AdvanceWebSecurityConfig
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils

/**
 * 自定义 WebViewClient（独立封装，版本适配+缓存+安全）
 * 职责：处理页面加载状态、URL 拦截、资源加载、安全校验，适配不同版本回调差异
 */
class AdvanceWebViewClient: WebViewClient() {

    /**
     * 页面开始加载时回调（缓存策略切换+安全校验）
     */
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        AdvanceLogUtils.d("AdvanceWebViewClient", "页面开始加载：$url")

        // 1. 安全校验：拦截非法 URL
        if (url !=null && !AdvanceWebSecurityConfig.checkUrlSafety(url)){
            view?.stopLoading()
            AdvanceLogUtils.w("AdvanceWebViewClient", "页面加载终止：非法 URL $url")
            return
        }
        // 2. 切换缓存策略（按网络状态）
        view?.context?.let { ctx->
            AdvanceWebCacheConfig.switchCacheStrategy(webView = view, context = ctx)
        }
    }

    /**
     * 页面加载完成时回调（后续操作+性能优化）
     */
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        AdvanceLogUtils.d("AdvanceWebViewClient", "页面加载完成：$url")

        // 可在此处执行后续操作（如 JS 动态注入、UI 更新）
    }

    /**
     * URL 拦截（安全防御：仅允许信任的 URL，拦截非法跳转）
     */
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return super.shouldOverrideUrlLoading(view, request)
        AdvanceLogUtils.d("AdvanceWebViewClient", "拦截 URL：$url")

        // 1. 安全校验：拦截非法 URL
        if (!AdvanceWebSecurityConfig.checkUrlSafety(url)) {
            AdvanceLogUtils.w("AdvanceWebViewClient", "拦截非法 URL 跳转：$url")
            return true
        }

        // 2. 本地文件 URL 直接加载（信任的 assets 目录）
        if (url.startsWith("file:///android_asset/")) {
            return super.shouldOverrideUrlLoading(view, request)
        }

        // 3. 网络 URL 按需处理（企业级可添加自定义逻辑）
        return super.shouldOverrideUrlLoading(view, request)
    }

    /**
     * 页面加载失败时回调（错误处理+缓存回退）
     */
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        val errorMsg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            error?.description ?: "未知错误"
        } else {
            error?.toString()
        }
        val url = request?.url?.toString() ?: "空 URL"
        AdvanceLogUtils.e("AdvanceWebViewClient", "页面加载失败：$url，错误：$errorMsg")

        // 无网络时切换到离线缓存模式，尝试加载缓存页面
        view?.context?.let { context ->
            AdvanceWebCacheConfig.switchCacheStrategy(view, context)
        }
    }

    /**
     * SSL 证书校验（防止中间人攻击，适配自签名证书）
     */
    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        super.onReceivedSslError(view, handler, error)
        AdvanceLogUtils.e("AdvanceWebViewClient", "SSL 证书校验失败：${error?.toString()}")
        // 企业级规范：默认拒绝加载，避免中间人攻击（自签名证书需单独配置白名单）
        handler?.cancel()
    }
}