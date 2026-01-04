package org.dev.jesen.advancewebview.advance.inject

import android.webkit.WebView
import org.dev.jesen.advancewebview.advance.config.AdvanceWebSecurityConfig
import org.dev.jesen.advancewebview.advance.constant.AdvanceConstants
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils
import org.dev.jesen.advancewebview.advance.helper.VersionUtils

/**
 * JS 注入管理类（独立封装，静态+动态注入，版本适配+安全校验）
 * 职责：统一管理 JS 注入逻辑，支持提前注入（静态）、动态注入（业务），兼容不同 Android 版本
 */

object AdvanceJsInjectManager {

    /**
     * 静态注入：全局工具类 JS（页面开始加载时注入，全页面可用）
     * 功能：提供 Toast、日志、存储等全局工具方法，依赖统一桥接与原生通信
     */
    fun injectGlobalToolJs(webView: WebView) {
        val bridgeName = AdvanceConstants.JS_BRIDGE_NAME
        val globalToolName = AdvanceConstants.INJECT_MODULE_GLOBAL_TOOL

        // 构建全局工具类 JS 代码（安全净化，避免恶意脚本）
        val globalJs = """
            // 全局工具类 JS（由 AdvanceEnterpriseWebView 注入，依赖桥接：$bridgeName）
            window.$globalToolName = window.$globalToolName || {};
            (function(bridgeName) {
                const bridge = window[bridgeName]; // 统一获取桥接实例

                // 1. 全局 Toast 方法（通过统一桥接调用原生）
                this.showToast = function(message) {
                    console.log("$globalToolName Toast：" + message);
                    if (bridge) {
                        try {
                            const method = "${AdvanceConstants.JS_METHOD_SHOW_TOAST}";
                            bridge.${AdvanceConstants.JS_METHOD_CALL_NATIVE}(method, JSON.stringify(message));
                        } catch (e) {
                            console.error("$globalToolName showToast 错误：" + e.message);
                        }
                    } else {
                        console.error("$globalToolName showToast 错误：未找到桥接 $bridgeName");
                    }
                };

                // 2. 全局日志方法（本地打印+更新 H5 页面）
                this.log = function(content) {
                    console.log("$globalToolName Log：" + content);
                    try {
                        const resultDom = document.getElementById("advanceInjectResult");
                        if (resultDom) {
                            resultDom.innerHTML += "$globalToolName Log：" + content + "<br/>";
                        }
                    } catch (e) {
                        console.error("$globalToolName log 错误：" + e.message);
                    }
                };

                // 3. 全局本地存储方法（H5 本地存储，安全高效）
                this.setStorage = function(key, value) {
                    try {
                        localStorage.setItem(key, value);
                        this.log("存储成功：" + key + "=" + value);
                    } catch (e) {
                        this.log("存储失败：" + e.message);
                    }
                };

                this.getStorage = function(key) {
                    try {
                        const value = localStorage.getItem(key);
                        this.log("获取存储：" + key + "=" + (value || "null"));
                        return value;
                    } catch (e) {
                        this.log("获取存储失败：" + e.message);
                        return null;
                    }
                };

                // 4. 全局清理缓存方法（调用原生清理缓存）
                this.clearCache = function() {
                    console.log("$globalToolName 清理缓存");
                    if (bridge) {
                        try {
                            const method = "${AdvanceConstants.JS_METHOD_CLEAR_CACHE}";
                            bridge.${AdvanceConstants.JS_METHOD_CALL_NATIVE}(method, JSON.stringify(""));
                            this.log("请求原生清理缓存");
                        } catch (e) {
                            console.error("$globalToolName clearCache 错误：" + e.message);
                        }
                    }
                };
            }).call(window.$globalToolName, "$bridgeName");

            // 注入完成回调
            console.log("$globalToolName 注入完成，依赖桥接：$bridgeName");
            try {
                document.getElementById("advanceInjectResult").innerHTML += "$globalToolName 注入完成<br/>";
            } catch (e) {}
        """.trimIndent()

        // 安全校验+执行注入
        injectJs(webView, globalJs, globalToolName)
    }

    /**
     * 动态注入：业务逻辑 JS（页面加载完成时注入，按需执行业务）
     * 功能：提供业务相关方法，依赖全局工具类+统一桥接，支持动态传参
     */
    fun injectBusinessJs(webView: WebView, businessData: String) {
        val bridgeName = AdvanceConstants.JS_BRIDGE_NAME
        val businessLogicName = AdvanceConstants.INJECT_MODULE_BUSINESS_LOGIC
        val globalToolName = AdvanceConstants.INJECT_MODULE_GLOBAL_TOOL

        // 构建业务逻辑 JS 代码（安全净化，避免恶意脚本）
        val businessJs = """
            // 业务逻辑 JS（由 AdvanceEnterpriseWebView 注入，依赖桥接：$bridgeName + 工具类：$globalToolName）
            window.$businessLogicName = window.$businessLogicName || {};
            (function(bridgeName, toolName) {
                const bridge = window[bridgeName];
                const tool = window[toolName];

                // 1. 业务数据（由原生传递，已完成 JSON 序列化）
                this.businessData = $businessData || {};

                // 2. 处理业务数据方法（依赖全局工具类日志）
                this.handleBusinessData = function() {
                    const data = this.businessData;
                    let dataStr = "业务数据：<br/>";
                    for (const key in data) {
                        if (data.hasOwnProperty(key)) {
                            dataStr += key + "：" + data[key] + "<br/>";
                        }
                    }
                    try {
                        const resultDom = document.getElementById("advanceInjectResult");
                        if (resultDom) {
                            resultDom.innerHTML += dataStr;
                        }
                        tool?.log("业务数据处理完成");
                    } catch (e) {
                        console.error("$businessLogicName handleBusinessData 错误：" + e.message);
                    }
                };

                // 3. 业务触发设备信息查询（调用原生获取设备信息）
                this.getDeviceInfo = function() {
                    if (bridge) {
                        try {
                            const method = "${AdvanceConstants.JS_METHOD_GET_DEVICE_INFO}";
                            bridge.${AdvanceConstants.JS_METHOD_CALL_NATIVE}(method, JSON.stringify(""));
                            tool?.log("请求原生获取设备信息");
                        } catch (e) {
                            console.error("$businessLogicName getDeviceInfo 错误：" + e.message);
                        }
                    }
                };
            }).call(window.$businessLogicName, "$bridgeName", "$globalToolName");

            // 注入完成后执行业务逻辑
            try {
                window.$businessLogicName.handleBusinessData();
                console.log("$businessLogicName 注入完成");
                document.getElementById("advanceInjectResult").innerHTML += "$businessLogicName 注入完成<br/>";
            } catch (e) {
                console.error("$businessLogicName 执行错误：" + e.message);
                document.getElementById("advanceInjectResult").innerHTML += "$businessLogicName 执行错误：" + e.message + "<br/>";
            }
        """.trimIndent()

        // 安全校验+执行注入
        injectJs(webView, businessJs, businessLogicName)
    }

    /**
     * 统一 JS 注入方法（兼容 Android 版本，安全校验，异常处理）
     */
    private fun injectJs(webView: WebView, jsCode: String, jsName: String) {
        // 1. 前置校验
        if (webView.url.isNullOrEmpty()) {
            AdvanceLogUtils.w("AdvanceJsInjectManager", "$jsName 注入失败：WebView 未加载页面")
            return
        }
        if (!AdvanceWebSecurityConfig.checkJsInjectSafety(jsCode)) {
            AdvanceLogUtils.w("AdvanceJsInjectManager", "$jsName 注入失败：JS 内容不安全")
            return
        }

        // 2. 版本兼容注入
        try {
            if (VersionUtils.isKitKatOrHigher()) {
                // Android 4.4+ 推荐使用 evaluateJavascript（异步，无弹窗，返回结果）
                webView.evaluateJavascript(jsCode) { result ->
                    AdvanceLogUtils.d("AdvanceJsInjectManager", "$jsName 注入结果：${result ?: "注入成功（无返回值）"}")
                }
            } else {
                // 低版本兼容（minSdk 21，此处仅作占位）
                webView.loadUrl("javascript:$jsCode")
                AdvanceLogUtils.d("AdvanceJsInjectManager", "$jsName 注入完成（低版本兼容）")
            }
        } catch (e: Exception) {
            AdvanceLogUtils.e("AdvanceJsInjectManager", "$jsName 注入异常：${e.message}", e)
            // 注入失败时回显到 H5 页面，提升用户体验
            webView.evaluateJavascript("javascript:document.getElementById('advanceInjectResult').innerHTML += '$jsName 注入异常：${e.message}<br/>'") {}
        }
    }
}