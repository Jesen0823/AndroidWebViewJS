package org.dev.jesen.androidwebviewjs.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.dev.jesen.androidwebviewjs.R
import org.dev.jesen.androidwebviewjs.core.constants.WebConstants
import org.dev.jesen.androidwebviewjs.core.helpers.WebViewLifecycleHelper
import org.dev.jesen.androidwebviewjs.core.utils.LogUtils
import org.dev.jesen.androidwebviewjs.databinding.ActivityBridgeWebViewBinding
import org.dev.jesen.androidwebviewjs.web.bridge.JsBridge
import org.dev.jesen.androidwebviewjs.web.bridge.NativeCallJsManager
import org.dev.jesen.androidwebviewjs.web.client.CustomWebViewClient
import org.dev.jesen.androidwebviewjs.web.config.WebViewConfig

/**
 * 阶段2：原生-JS 互调演示
 * 功能：实现原生调用 JS、JS 调用原生，桥接方案
 */
class BridgeWebViewActivity : AppCompatActivity(), JsBridge.OnJsCallNativeListener {
    private lateinit var binding: ActivityBridgeWebViewBinding
    private lateinit var webViewLifecycleHelper: WebViewLifecycleHelper
    private lateinit var jsBridge: JsBridge
    private lateinit var nativeCallJsManager: NativeCallJsManager
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBridgeWebViewBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. 初始化WebView配置
        WebViewConfig.initBasicConfig(binding.webView)
        // 2. 设置自定义WebViewClient
        binding.webView.webViewClient = CustomWebViewClient()


        // 步骤3：等待WebView完全初始化，再注入JS接口（关键优化）
        binding.webView.post {
            initJsBridge()
            // 步骤4：延迟500ms加载H5，给接口注入留足时间（低版本Android适配）
            mainHandler.postDelayed({
                loadLocalHtml()
            }, 500)
        }

        // 5.初始化生命周期管理
        webViewLifecycleHelper = WebViewLifecycleHelper(binding.webView)
    }

    /**
     * 初始化 JS 桥接（核心：暴露原生方法给 JS）
     */
    private fun initJsBridge() {
        jsBridge = JsBridge(this)
        LogUtils.d("BridgeWebViewActivity", "开始注入JS桥接：${WebConstants.JS_BRIDGE_NAME}")
        try{
            // 暴露 JS 桥接给 JS，桥接名称为 WebConstants.JS_BRIDGE_NAM
            binding.webView.addJavascriptInterface(jsBridge, WebConstants.JS_BRIDGE_NAME)
            LogUtils.d("BridgeWebViewActivity", "JS桥接注入成功：${WebConstants.JS_BRIDGE_NAME}")
        }catch (e: Exception){
            LogUtils.e("BridgeWebViewActivity", "JS桥接注入失败：${e.message}")
        }
        // 初始化原生调用 JS 管理类
        nativeCallJsManager = NativeCallJsManager(binding.webView, jsBridge)
    }

    private fun loadLocalHtml() {
        LogUtils.d("BridgeWebViewActivity", "加载 H5：${WebConstants.LOCAL_HTML_STAGE2}")
        binding.webView.loadUrl(WebConstants.LOCAL_HTML_STAGE2)
    }


    // -------------JsBridge.OnJsCallNativeListener---回调-------------- :
    override fun onGetUserInfo() {
        LogUtils.d("BridgeWebViewActivity", "JS 请求获取用户信息")
        // 模拟用户信息
        val userInfo = mapOf(
            "userId" to "10001",
            "userName" to "WebView Enterprise",
            "userAge" to "25",
            "userAvatar" to "https://picsum.photos/200/200"
        )
        // 原生调用JS，返回用户信息
        nativeCallJsManager.returnUserInfo(userInfo)
    }

    override fun onOpenNativePage(pageName: String) {
        LogUtils.d("BridgeWebViewActivity", "JS 请求打开原生页面：$pageName")
        // 模拟打开原生页面
        nativeCallJsManager.showToast("已打开原生页面：$pageName")
    }

    override fun onUnknownMethod(methodName: String, params: String) {
        LogUtils.w("BridgeWebViewActivity", "未知 JS 方法：$methodName, 参数：$params")
        nativeCallJsManager.showToast("未知方法：$methodName")
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
        binding.webView.removeJavascriptInterface(WebConstants.JS_BRIDGE_NAME) // 移除 JS 桥接，避免内存泄漏
        binding.webView.removeAllViews()
        binding.webView.destroy()
    }
}