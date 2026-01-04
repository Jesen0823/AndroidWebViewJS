package org.dev.jesen.advancewebview.advance.bridge

import android.webkit.WebView
import com.google.gson.Gson
import org.dev.jesen.advancewebview.advance.config.AdvanceWebSecurityConfig
import org.dev.jesen.advancewebview.advance.constant.AdvanceConstants
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils

/**
 * JS 桥接工具类（独立封装，统一管理桥接生命周期）
 * 职责：封装桥接初始化、销毁、数据序列化，避免重复代码，提升可扩展性
 */
object AdvanceJsBridgeHelper {
    val gson = Gson()

    /**
     * 初始化桥接（全局统一配置，安全高效）
     */
    fun initBridge(
        webView: WebView,
        listener: AdvanceJsBridge.OnJsCallNativeListener
    ): Pair<AdvanceJsBridge, AdvanceNativeCallJsManager>{
        // 1. 初始化桥接实例（唯一入口，名称固定为 AdvanceConstants.JS_BRIDGE_NAME）
        val jsBridge = AdvanceJsBridge(listener)
        webView.addJavascriptInterface(jsBridge, AdvanceConstants.JS_BRIDGE_NAME)

        // 2. 初始化原生调用 JS 管理类
        val nativeCallJsManager = AdvanceNativeCallJsManager(webView, jsBridge)
        AdvanceLogUtils.d("JsBridgeHelper", "桥接初始化完成，桥接名称：${AdvanceConstants.JS_BRIDGE_NAME}")
        return Pair(jsBridge, nativeCallJsManager)
    }

    /**
     * 移除桥接（销毁时释放资源，避免内存泄漏+安全漏洞）
     */
    fun removeBridge(webView: WebView){
        webView.removeJavascriptInterface(AdvanceConstants.JS_BRIDGE_NAME)
        AdvanceLogUtils.d("JsBridgeHelper", "桥接已移除，资源释放完成")
    }

    /**
     * 数据序列化（统一 JSON 格式，避免序列化差异）
     */
    fun toJson(obj: Any): String {
        return try {
            gson.toJson(obj)
        } catch (e: Exception) {
            AdvanceLogUtils.e("JsBridgeHelper", "JSON 序列化失败：${e.message}", e)
            "{}"
        }
    }

    /**
     * 数据反序列化（统一 JSON 格式，安全校验）
     */
    inline fun <reified T> fromJson(json: String): T? {
        return try {
            val safeJson = AdvanceWebSecurityConfig.filterXssContent(json)
            gson.fromJson(safeJson, T::class.java)
        } catch (e: Exception) {
            AdvanceLogUtils.e("JsBridgeHelper", "JSON 反序列化失败：${e.message}", e)
            null
        }
    }
}