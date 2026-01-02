package org.dev.jesen.androidwebviewjs.core.helpers

import android.webkit.WebView
import com.google.gson.Gson
import org.dev.jesen.androidwebviewjs.core.constants.WebConstants
import org.dev.jesen.androidwebviewjs.core.utils.LogUtils
import org.dev.jesen.androidwebviewjs.web.bridge.JsBridge
import org.dev.jesen.androidwebviewjs.web.bridge.NativeCallJsManager

/**
 * 通用JS桥接工具类（阶段2/3/后续阶段共用，统一配置）
 * 职责：封装桥接初始化、原生-JS通信，避免重复代码和配置冲突
 */
object JsBridgeHelper {
    private val gson = Gson()

    /**
     * 初始化桥接（全局统一配置）
     * @param webView WebView实例
     * @param listener JS调用原生的回调（各页面按需实现）
     * @return Pair<JsBridge, NativeCallJsManager> 桥接实例+原生调用JS管理类
     */
    fun initBridge(
        webView: WebView,
        listener: JsBridge.OnJsCallNativeListener
    ): Pair<JsBridge, NativeCallJsManager> {
        // 1. 初始化桥接（唯一入口，名称固定为WebConstants.JS_BRIDGE_NAME）
        val jsBridge = JsBridge(listener)
        webView.addJavascriptInterface(jsBridge, WebConstants.JS_BRIDGE_NAME)
        LogUtils.d("JsBridgeHelper", "桥接初始化完成，名称：${WebConstants.JS_BRIDGE_NAME}")

        // 2. 初始化原生调用JS管理类（全局统一方法名）
        val nativeCallJsManager = NativeCallJsManager(webView, jsBridge)
        return Pair(jsBridge, nativeCallJsManager)
    }

    /**
     * 移除桥接（统一销毁逻辑）
     */
    fun removeBridge(webView: WebView) {
        webView.removeJavascriptInterface(WebConstants.JS_BRIDGE_NAME)
        LogUtils.d("JsBridgeHelper", "桥接已移除")
    }

    /**
     * 辅助方法：将对象转为JSON字符串（全局统一序列化方式）
     */
    fun toJson(obj: Any): String {
        return try {
            gson.toJson(obj)
        } catch (e: Exception) {
            LogUtils.e("JsBridgeHelper", "JSON序列化失败：${e.message}")
            "{}"
        }
    }
}