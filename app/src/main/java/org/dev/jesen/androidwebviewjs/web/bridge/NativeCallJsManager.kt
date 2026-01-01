package org.dev.jesen.androidwebviewjs.web.bridge

import android.webkit.WebView
import com.google.gson.Gson
import org.dev.jesen.androidwebviewjs.core.constants.WebConstants

/**
 * 原生调用 JS 管理类
 * 职责：封装原生调用 JS 的具体业务逻辑，提供简洁 API 给上层
 */
class NativeCallJsManager(private val webView: WebView, private val jsBridge: JsBridge) {

    private val gson = Gson()

    /**
     * 原生调用 JS 显示 Toast
     */
    fun showToast(message: String){
        val params = gson.toJson(message)
        jsBridge.callJs(webView, WebConstants.NATIVE_METHOD_SHOW_TOAST,params)
    }

    /**
     * 原生调用 JS 更新 UI
     */
    fun updateUi(data: Map<String, String>) {
        val params = gson.toJson(data)
        jsBridge.callJs(webView, WebConstants.NATIVE_METHOD_UPDATE_UI, params)
    }

    /**
     * 原生调用 JS 返回用户信息
     */
    fun returnUserInfo(userInfo: Map<String, String>) {
        val params = gson.toJson(userInfo)
        jsBridge.callJs(webView, WebConstants.JS_METHOD_GET_USER_INFO, params)
    }
}