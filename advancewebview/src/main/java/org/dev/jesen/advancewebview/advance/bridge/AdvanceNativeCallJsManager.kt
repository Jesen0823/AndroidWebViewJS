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
        val params = AdvanceJsBridgeHelper.toJson(data)
        AdvanceThreadHelper.runOnMainThread(webView.context) {
            jsBridge.callAdvanceJs(webView, AdvanceConstants.NATIVE_METHOD_UPDATE_UI, params)
        }
        AdvanceLogUtils.d("NativeCallJsManager", "原生调用 JS 更新 UI 完成")
    }

    /**
     * 原生调用 JS 通知缓存状态
     */
    fun notifyCacheState(cacheState: String) {
        val params = AdvanceJsBridgeHelper.toJson(cacheState)
        AdvanceThreadHelper.runOnMainThread(webView.context) {
            jsBridge.callAdvanceJs(webView, AdvanceConstants.NATIVE_METHOD_NOTIFY_CACHE, params)
        }
        AdvanceLogUtils.d("NativeCallJsManager", "原生调用 JS 通知缓存状态：$cacheState")
    }

    /**
     * 原生调用 JS 传递设备信息
     */
    fun sendDeviceInfo(deviceInfo: Map<String, String>) {
        val params = AdvanceJsBridgeHelper.toJson(deviceInfo.takeIf { it.isNotEmpty() }
            ?: mapOf("msg" to "无设备信息"))
        AdvanceThreadHelper.runOnMainThread(webView.context) {
            jsBridge.callAdvanceJs(webView, AdvanceConstants.JS_METHOD_GET_DEVICE_INFO, params)
            AdvanceLogUtils.d("NativeCallJsManager", "原生调用 JS 传递设备信息完成")
        }
    }
}