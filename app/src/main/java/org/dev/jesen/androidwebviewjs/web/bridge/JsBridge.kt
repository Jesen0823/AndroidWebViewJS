package org.dev.jesen.androidwebviewjs.web.bridge

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import org.dev.jesen.androidwebviewjs.core.constants.WebConstants
import org.dev.jesen.androidwebviewjs.core.utils.LogUtils

/**
 * 原生-JS 通信桥
 * 职责：暴露原生方法给 JS，处理 JS 调用逻辑，解耦通信与业务
 *
 * 优化：符合Java兼容性的JS桥接类
 *  * 1. 类声明明确为open（允许继承，避免混淆问题）
 *  * 2. @JavascriptInterface方法确保public、非内联、参数为Java基础类型
 *  * 3. 添加参数非空判断，避免空指针异常
 */
open class JsBridge(private val onJsCallNativeListener:OnJsCallNativeListener) {
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * JS 调用原生的回调接口
     */
    interface OnJsCallNativeListener{
        fun onGetUserInfo() // JS 请求获取用户信息
        fun onOpenNativePage(pageName: String) // JS 请求打开原生页面
        fun onUnknownMethod(methodName: String, params: String) // 未知方法回调
    }

    /**
     * 暴露给 JS 的核心方法（JS 调用原生的入口）
     * 注解 @JavascriptInterface 必须添加（Android 4.2+ 安全要求）
     *
     * 1. 明确标注@JavascriptInterface（必须保留，无替代）
     * 2. 方法为public（Kotlin默认public，明确声明更规范）
     * 3. 参数添加非空判断，避免空指针导致方法执行中断
     * 4. 打印详细日志，便于排查调用链路
     */
    @JavascriptInterface
    public fun callAndroid(methodName: String?,params: String?){
        LogUtils.d("JsBridge", "JS 调用原生：methodName=$methodName, params=$params")
        // 步骤1：参数非空校验（JS可能传递null，避免空指针）
        val finalMethodName = methodName ?: "unknownMethod"
        val finalParams = params ?: ""
        LogUtils.d("JsBridge", "===== JS 调用原生开始 =====")
        LogUtils.d("JsBridge", "方法名：$finalMethodName")
        LogUtils.d("JsBridge", "参数：$finalParams")
        LogUtils.d("JsBridge", "===== JS 调用原生结束 =====")

        // 步骤2：解析方法名，分发到对应回调
        when (finalMethodName) {
            WebConstants.JS_METHOD_GET_USER_INFO -> onJsCallNativeListener.onGetUserInfo()
            WebConstants.JS_METHOD_OPEN_NATIVE_PAGE -> {
                val pageName = try {
                    gson.fromJson(finalParams, String::class.java)
                } catch (e: Exception) {
                    LogUtils.e("JsBridge", "解析页面参数失败：${e.message}")
                    "defaultPage"
                }
                onJsCallNativeListener.onOpenNativePage(pageName)
            }
            else -> onJsCallNativeListener.onUnknownMethod(finalMethodName, finalParams)
        }
    }

    /**
     * 原生调用 JS 方法（统一入口）
     * @param webView WebView 实例
     * @param methodName JS 方法名
     * @param params 传递给 JS 的参数（JSON 格式）
     */
    fun callJs(webView: WebView,methodName: String,params: String){
        // 检查当前线程是否为主线程
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // 已在主线程，直接执行
            executeJs(webView, methodName, params)
        } else {
            // 不在主线程，通过 Handler 切换到主线程
            mainHandler.post {
                executeJs(webView, methodName, params)
            }
        }
    }

    /**
     * 抽取独立方法：执行 JS 调用
     */
    private fun executeJs(webView: android.webkit.WebView, methodName: String, params: String) {
        try {
            // 虽然没必要判断，因为最低版本API21>API19
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Android 4.4+ 推荐使用 evaluateJavascript（异步，无弹窗，返回结果）
                webView.evaluateJavascript("javascript:${WebConstants.NATIVE_METHOD_CALL_JS}('$methodName', '$params')") { result ->
                    LogUtils.d("JsBridge", "原生调用 JS 结果：$result")
                }
            } else {
                // 低版本兼容API19：使用 loadUrl（同步，有弹窗，无返回结果）
                webView.loadUrl("javascript:${WebConstants.NATIVE_METHOD_CALL_JS}('$methodName', '$params')")
            }
        } catch (e: Exception) {
            LogUtils.e("JsBridge", "执行 JS 失败：${e.message}")
        }
    }
}
















