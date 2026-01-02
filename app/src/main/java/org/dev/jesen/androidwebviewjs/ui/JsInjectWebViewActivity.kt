package org.dev.jesen.androidwebviewjs.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import org.dev.jesen.androidwebviewjs.R
import org.dev.jesen.androidwebviewjs.core.constants.WebConstants
import org.dev.jesen.androidwebviewjs.core.helpers.JsBridgeHelper
import org.dev.jesen.androidwebviewjs.core.helpers.WebViewLifecycleHelper
import org.dev.jesen.androidwebviewjs.core.utils.LogUtils
import org.dev.jesen.androidwebviewjs.databinding.ActivityJsInjectWebViewBinding
import org.dev.jesen.androidwebviewjs.web.JsInjectManager
import org.dev.jesen.androidwebviewjs.web.bridge.JsBridge
import org.dev.jesen.androidwebviewjs.web.bridge.NativeCallJsManager
import org.dev.jesen.androidwebviewjs.web.client.CustomWebViewClient
import org.dev.jesen.androidwebviewjs.web.config.WebViewConfig

/**
 * 阶段3：JS 注入演示
 * 功能：实现提前注入（全局工具类）、动态注入（业务逻辑）
 */
class JsInjectWebViewActivity : AppCompatActivity(), JsBridge.OnJsCallNativeListener {
    private lateinit var binding: ActivityJsInjectWebViewBinding
    private lateinit var webViewLifecycleHelper: WebViewLifecycleHelper
    private lateinit var jsBridge: JsBridge
    private lateinit var nativeCallJsManager: NativeCallJsManager // 原生调用JS管理类
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJsInjectWebViewBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. 初始化WebView基础配置（不变）
        WebViewConfig.initBasicConfig(binding.webView)

        // 1. 初始化 JsBridge（核心修复：注入的JS依赖此桥接调用原生）
        binding.webView.post {
            initJsBridge()
            // 步骤4：延迟500ms加载H5，给接口注入留足时间（低版本Android适配）
            mainHandler.postDelayed({
                loadLocalHtml()
            }, 500)
        }

        // 2. 设置WebView
        initWebView()

        // 3. 初始化生命周期管理
        webViewLifecycleHelper = WebViewLifecycleHelper(binding.webView)
    }

    /**
     * 初始化 JsBridge（与阶段2复用逻辑）
     */
    private fun initJsBridge() {
        jsBridge = JsBridge(this)
        // 暴露桥接给JS，名称与注入的JS保持一致（AndroidJsBridge）
        binding.webView.addJavascriptInterface(jsBridge, WebConstants.JS_BRIDGE_NAME)
        // 初始化原生调用JS管理类
        nativeCallJsManager = NativeCallJsManager(binding.webView, jsBridge)
        LogUtils.d("JsInjectWebViewActivity", "JsBridge 初始化完成")
    }

    /**
     * 加载本地 H5（支持 JS 注入）
     */
    private fun loadLocalHtml() {
        LogUtils.d("JsInjectWebViewActivity", "加载 H5：${WebConstants.LOCAL_HTML_STAGE3}")
        binding.webView.loadUrl(WebConstants.LOCAL_HTML_STAGE3)
    }

    private fun initWebView() {
        // 2. 设置自定义 WebViewClient（重写注入时机方法）
        binding.webView.webViewClient = object : CustomWebViewClient(){
            /**
             * 页面开始加载时：提前注入全局工具类 JS
             */
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.let { JsInjectManager.injectGlobalToolJs(it) }
            }

            /**
             * 页面加载完成时：动态注入业务逻辑 JS
             */
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.let {
                    val businessData = JsBridgeHelper.toJson( // 复用工具类序列化
                        mapOf(
                            "orderId" to "O20240501001",
                            "orderAmount" to "99.00",
                            "orderStatus" to "已支付",
                            "payTime" to "2024-05-01 10:00:00"
                        )
                    )
                    JsInjectManager.injectBusinessJs(it, businessData)
                }
            }
        }
    }

    // ---------------------- 生命周期管理 ----------------------
    override fun onResume() {
        super.onResume()
        webViewLifecycleHelper.onResume()
    }

    override fun onPause() {
        super.onPause()
        webViewLifecycleHelper.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewLifecycleHelper.onDestroy()
        JsBridgeHelper.removeBridge(binding.webView)
        binding.webView.removeAllViews()
        binding.webView.destroy()
    }

    // -----------------------------JsBridge.OnJsCallNativeListener------------------
    override fun onGetUserInfo() {
        // 注入的JS未调用此方法，可留空或实现默认逻辑
        nativeCallJsManager.showToast("JS 调用原生获取用户信息")
    }

    override fun onOpenNativePage(pageName: String) {
        nativeCallJsManager.showToast("JS 调用原生打开页面：$pageName")
    }

    override fun onUnknownMethod(methodName: String, params: String) {
        LogUtils.w("JsInjectWebViewActivity", "未知 JS 方法：$methodName, 参数：$params")
        nativeCallJsManager.showToast("未知方法：$methodName")
        // 回显到H5页面
        runOnUiThread {
            binding.webView.evaluateJavascript("javascript:document.getElementById('injectResult').innerHTML += '未知方法：$methodName<br/>'") {}
        }
    }
}