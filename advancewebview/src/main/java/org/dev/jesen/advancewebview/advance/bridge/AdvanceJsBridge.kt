package org.dev.jesen.advancewebview.advance.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import org.dev.jesen.advancewebview.advance.config.AdvanceWebSecurityConfig
import org.dev.jesen.advancewebview.advance.constant.AdvanceConstants
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils
import org.dev.jesen.advancewebview.advance.helper.VersionUtils

/**
 * 原生-JS 通信桥（独立封装，安全高效）
 * 职责：暴露原生方法给 JS，处理 JS 调用逻辑，参数安全校验，解耦通信与业务
 */
class AdvanceJsBridge(private val onJsCallNativeListener: OnJsCallNativeListener) {
    private val gson = Gson()

    /**
     * JS 调用原生的回调接口（业务解耦，上层页面按需实现）
     */
    interface OnJsCallNativeListener {
        fun onShowToast(message: String) // 显示 Toast
        fun onGetDeviceInfo() // 获取设备信息
        fun onClearCache() // 清理缓存
        fun onUnknownMethod(methodName: String, params: String) // 未知方法回调
    }

    /**
     * 暴露给 JS 的核心方法（唯一入口，@JavascriptInterface 注解必备，安全要求）
     * 所有 JS 调用原生都通过此方法分发，便于统一校验和管理
     */
    @JavascriptInterface
    fun callAdvanceAndroid(methodName: String, params: String){
        // 安全校验：过滤 XSS 关键字，防止恶意调用
        val safeMethodName = AdvanceWebSecurityConfig.filterXssContent(methodName)
        val safeParams = AdvanceWebSecurityConfig.filterXssContent(params)

        AdvanceLogUtils.d("AdvanceJsBridge", "JS 调用原生：methodName=$safeMethodName, params=$safeParams")

        // 方法分发（白名单机制，仅允许信任的方法，安全防御）
        when (safeMethodName) {
            AdvanceConstants.JS_METHOD_SHOW_TOAST -> handleShowToast(safeParams)
            AdvanceConstants.JS_METHOD_GET_DEVICE_INFO -> onJsCallNativeListener.onGetDeviceInfo()
            AdvanceConstants.JS_METHOD_CLEAR_CACHE -> onJsCallNativeListener.onClearCache()
            else -> onJsCallNativeListener.onUnknownMethod(safeMethodName, safeParams)
        }
    }

    /**
     * 处理显示 Toast 逻辑（参数解析+安全校验）
     */
    private fun handleShowToast(params: String) {
        try {
            val message = gson.fromJson(params, String::class.java)
            onJsCallNativeListener.onShowToast(message ?: "空消息")
        } catch (e: Exception) {
            AdvanceLogUtils.e("AdvanceJsBridge", "解析 Toast 参数失败：${e.message}", e)
            onJsCallNativeListener.onUnknownMethod(AdvanceConstants.JS_METHOD_SHOW_TOAST, params)
        }
    }

    /**
     * 原生调用 JS 统一入口（兼容 Android 4.4+，安全高效）
     */
    fun callAdvanceJs(webView: WebView, methodName: String, params: String) {
        // 安全校验：过滤 XSS 关键字
        val safeMethodName = AdvanceWebSecurityConfig.filterXssContent(methodName)
        val safeParams = AdvanceWebSecurityConfig.filterXssContent(params)

        // 兼容 Android 4.4+（minSdk 21，此处简化为 4.4+ 适配）
        if (VersionUtils.isKitKatOrHigher()) {
            // Android 4.4+ 推荐使用 evaluateJavascript（异步，无弹窗，返回结果）
            webView.evaluateJavascript("javascript:${AdvanceConstants.NATIVE_METHOD_CALL_JS}('$safeMethodName', '$safeParams')") { result ->
                AdvanceLogUtils.d("AdvanceJsBridge", "原生调用 JS 结果：${result ?: "注入成功（无返回值）"}")
            }
        } else {
            // 低版本兼容（minSdk 21，此处仅作占位，实际无需处理）
            webView.loadUrl("javascript:${AdvanceConstants.NATIVE_METHOD_CALL_JS}('$safeMethodName', '$safeParams')")
            AdvanceLogUtils.d("AdvanceJsBridge", "原生调用 JS 完成（低版本兼容）")
        }
    }
}