package org.dev.jesen.advancewebview.advance.bridge

import android.webkit.WebView
import org.dev.jesen.advancewebview.advance.constant.AdvanceConstants
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils
import org.dev.jesen.advancewebview.advance.helper.AdvanceThreadHelper

/**
 * 原生调用 JS 管理类（独立封装，业务解耦）
 * 职责：封装原生调用 JS 的具体业务逻辑，提供简洁 API 给上层，避免直接操作桥接
 */
class AdvanceNativeCallJsManager(
    private val webView: WebView,
    private val jsBridge: AdvanceJsBridge
) {
    /**
     * 原生调用 JS 更新 UI
     */
    fun updateUi(data: Map<String, String>) {
        callJs(AdvanceConstants.NATIVE_METHOD_UPDATE_UI, data, "更新 UI 完成")
    }

    /**
     * 原生调用 JS 通知缓存状态
     */
    fun notifyCacheState(cacheState: String) {
        callJs(AdvanceConstants.NATIVE_METHOD_NOTIFY_CACHE, cacheState, "通知缓存状态完成")
    }

    /**
     * 原生调用 JS 传递设备信息
     */
    fun sendDeviceInfo(deviceInfo: Map<String, String>) {
        val safeDeviceInfo = deviceInfo.takeIf { it.isNotEmpty() } ?: mapOf("msg" to "无设备信息")
        callJs(AdvanceConstants.JS_METHOD_GET_DEVICE_INFO, safeDeviceInfo, "传递设备信息完成")
    }

    /**
     * 原生调用 JS 更新安全配置
     */
    fun updateSecurityConfig(config: Map<String, String>) {
        callJs(AdvanceConstants.NATIVE_METHOD_UPDATE_SECURITY_CONFIG, config, "更新安全配置完成")
    }

    /**
     * 原生调用 JS 通知 URL 校验结果
     */
    fun notifyUrlCheckResult(result: Map<String, Any>) {
        callJs(AdvanceConstants.NATIVE_METHOD_NOTIFY_URL_CHECK, result, "通知 URL 校验结果完成")
    }

    /**
     * 原生调用 JS 通知 XSS 过滤结果
     */
    fun notifyXssFilterResult(result: Map<String, String>) {
        callJs(AdvanceConstants.NATIVE_METHOD_NOTIFY_XSS_FILTER, result, "通知 XSS 过滤结果完成")
    }

    /**
     * 抽象通用的 JS 调用逻辑
     */
    private fun callJs(methodName: String, data: Any, actionDesc: String) {
        val params = AdvanceJsBridgeHelper.toJson(data)
        AdvanceThreadHelper.runOnMainThread(webView.context) {
            jsBridge.callAdvanceJs(webView, methodName, params)
        }
        AdvanceLogUtils.d("NativeCallJsManager", "原生调用 JS $actionDesc，方法：$methodName，参数：$params")
    }
}