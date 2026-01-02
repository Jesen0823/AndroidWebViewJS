package org.dev.jesen.androidwebviewjs.web

import android.webkit.WebView
import org.dev.jesen.androidwebviewjs.core.constants.WebConstants
import org.dev.jesen.androidwebviewjs.core.utils.LogUtils

/**
 * JS 注入管理类
 * 职责：统一管理 JS 注入逻辑，支持提前注入、动态注入
 */
object JsInjectManager {
    /**
     * 提前注入：全局工具类 JS（页面开始加载时调用）
     * 功能：提供 H5 全局工具方法，如 Toast、日志、存储等
     */
    fun injectGlobalToolJs(webView: WebView) {
        // 核心：使用WebConstants.JS_BRIDGE_NAME，与阶段2保持一致
        val bridgeName = WebConstants.JS_BRIDGE_NAME
        val globalToolName = WebConstants.INJECT_MODULE_GLOBAL_TOOL

        val globalJs = """
            // 全局工具类 JS（由 Android 原生注入）
            window.$globalToolName = window.$globalToolName || {};
            (function(bridgeName) {
                const bridge = window[bridgeName]; // 统一获取桥接实例
                
                // 1. 全局 Toast 方法（增加桥接存在性判断）
                this.showToast = function(message) {
                    console.log("$globalToolName Toast：" + message);
                    if (bridge) {
                        try {
                            const method = "${WebConstants.JS_METHOD_SHOW_TOAST}";
                            bridge.${WebConstants.JS_METHOD_CALL_NATIVE}(method, JSON.stringify(message));
                        } catch (e) {
                            console.error("$globalToolName showToast 错误：" + e.message);
                        }
                    } else {
                        console.error("$globalToolName showToast 错误：未找到 AndroidJsBridge");
                    }
                };

                // 2. 全局日志方法
                this.log = function(content) {
                    console.log("$globalToolName Log：" + content);
                    try {
                        const resultDom = document.getElementById("injectResult");
                        if (resultDom) {
                            resultDom.innerHTML += "$globalToolName Log：" + content + "<br/>";
                        }
                    } catch (e) {
                        console.error("$globalToolName log 错误：" + e.message);
                    }
                };

                // 3. 本地存储方法
                this.setStorage = function(key, value) {
                    try {
                        // 判断localStorage是否存在
                        if (!window.localStorage) {
                            this.log("存储失败：浏览器不支持localStorage");
                            return;
                        }
                        localStorage.setItem(key, value);
                        this.log("存储成功：" + key + "=" + value);
                    } catch (e) {
                        this.log("存储失败：" + e.message);
                    }
                };

                this.getStorage = function(key) {
                    try {
                        if (!window.localStorage) {
                            this.log("获取存储失败：浏览器不支持localStorage");
                            return null;
                        }
                        const value = localStorage.getItem(key);
                        this.log("获取存储：" + key + "=" + (value || "null"));
                        return value;
                    } catch (e) {
                        this.log("获取存储失败：" + e.message);
                        return null;
                    }
                };
            }).call(window.$globalToolName, "$bridgeName");

            // 注入完成回调
            console.log("$globalToolName 注入完成，依赖桥接：$bridgeName");
            try {
                document.getElementById("injectResult").innerHTML += "$globalToolName 注入完成<br/>";
            } catch (e) {}
        """.trimIndent()

        injectJs(webView, globalJs, globalToolName)
    }

    /**
     * 动态注入：业务逻辑 JS（页面加载完成时调用）
     * 功能：提供业务相关 JS 方法，如数据处理、UI 操作等
     * 1. 封装为自执行函数 2. 避免变量污染
     */
    fun injectBusinessJs(webView: WebView, businessData: String) {
        val bridgeName = WebConstants.JS_BRIDGE_NAME
        val businessLogicName = WebConstants.INJECT_MODULE_BUSINESS_LOGIC
        val globalToolName = WebConstants.INJECT_MODULE_GLOBAL_TOOL

        val businessJs = """
            // 业务逻辑类（依赖统一桥接：$bridgeName + 工具类：$globalToolName）
            window.$businessLogicName = window.$businessLogicName || {};
            (function(bridgeName, toolName) {
                const bridge = window[bridgeName];
                const tool = window[toolName];

                // 业务数据（原生传递）
                this.businessData = $businessData || {};

                // 处理业务数据（依赖工具类日志）
                this.handleBusinessData = function() {
                    const data = this.businessData;
                    let dataStr = "业务数据：<br/>";
                    for (const key in data) {
                        if (data.hasOwnProperty(key)) {
                            dataStr += key + "：" + data[key] + "<br/>";
                        }
                    }
                    try {
                        const resultDom = document.getElementById("injectResult");
                        if (resultDom) {
                            resultDom.innerHTML += dataStr;
                        }
                        tool?.log("业务数据处理完成");
                    } catch (e) {
                        console.error("$businessLogicName handleBusinessData 错误：" + e.message);
                    }
                };

                // 新增：业务触发原生页面跳转（通过统一桥接）
                this.openOrderDetail = function(orderId) {
                    if (bridge) {
                        try {
                            const method = "${WebConstants.JS_METHOD_OPEN_NATIVE_PAGE}";
                            bridge.${WebConstants.JS_METHOD_CALL_NATIVE}(method, JSON.stringify("OrderDetailPage"));
                            tool?.log("请求打开订单详情页：" + orderId);
                        } catch (e) {
                            console.error("$businessLogicName openOrderDetail 错误：" + e.message);
                        }
                    }
                };
            }).call(window.$businessLogicName, "$bridgeName", "$globalToolName");

            // 注入完成后执行业务逻辑
            try {
                window.$businessLogicName.handleBusinessData();
                console.log("$businessLogicName 注入完成");
                document.getElementById("injectResult").innerHTML += "$businessLogicName 注入完成<br/>";
            } catch (e) {
                console.error("$businessLogicName 执行错误：" + e.message);
                document.getElementById("injectResult").innerHTML += "$businessLogicName 执行错误：" + e.message + "<br/>";
            }
        """.trimIndent()

        injectJs(webView, businessJs, businessLogicName)
    }

    /**
     * 统一 JS 注入方法（兼容 Android 版本）
     */
    private fun injectJs(webView: WebView, jsCode: String, jsName: String) {
        if (webView.url.isNullOrEmpty()) {
            LogUtils.w("JsInjectManager", "$jsName 注入失败：WebView 未加载页面")
            return
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                // Android 4.4+ 推荐使用 evaluateJavascript
                webView.evaluateJavascript(jsCode) { result ->
                    // 注入结果为null是正常现象（JS无返回值）
                    LogUtils.d("JsInjectManager", "$jsName 注入结果：${result ?: "注入成功（无返回值）"}")
                }
            } else {
                // 低版本兼容：使用 loadUrl
                webView.loadUrl("javascript:$jsCode")
                LogUtils.d("JsInjectManager", "$jsName 注入完成（低版本兼容）")
            }
        } catch (e: Exception) {
            LogUtils.e("JsInjectManager", "$jsName 注入异常：${e.message}", e)
            // 注入失败时回显到H5
            webView.evaluateJavascript("javascript:document.getElementById('injectResult').innerHTML += '$jsName 注入异常：${e.message}<br/>'") {}
        }
    }
}