package org.dev.jesen.advancewebview.advance.client

import android.webkit.WebChromeClient
import android.webkit.WebView
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils

/**
 * 自定义 WebChromeClient（独立封装，性能优化+用户交互）
 * 职责：处理页面加载进度、弹窗、标题更新，适配不同版本交互差异
 */
open class AdvanceWebChromeClient: WebChromeClient() {
    // 页面加载进度回调（性能优化：更新进度条，提升用户体验）
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        AdvanceLogUtils.d("WebChromeClient", "页面加载进度：$newProgress%")

        // 可在此处更新 UI 进度条（可添加回调接口）
    }

    // 页面标题更新回调（适配不同版本，更新标题栏）
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        AdvanceLogUtils.d("WebChromeClient", "页面标题更新：$title")

        // 可在此处更新 Activity 标题栏
    }

    // 弹窗处理（安全防御：拦截不必要的弹窗，仅允许信任的弹窗）
    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: android.webkit.JsResult?
    ): Boolean {
        AdvanceLogUtils.w("WebChromeClient", "拦截 JS 弹窗：$message，URL：$url")

        // 默认拦截，避免恶意弹窗（必要时可添加白名单）
        result?.cancel()
        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: android.webkit.JsResult?
    ): Boolean {
        AdvanceLogUtils.w("WebChromeClient", "拦截 JS 确认弹窗：$message，URL：$url")
        result?.cancel()
        return true
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: android.webkit.JsPromptResult?
    ): Boolean {
        AdvanceLogUtils.w("WebChromeClient", "拦截 JS 输入弹窗：$message，URL：$url")
        result?.cancel()
        return true
    }
}