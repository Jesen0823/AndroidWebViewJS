package org.dev.jesen.advancewebview.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.dev.jesen.advancewebview.R
import org.dev.jesen.advancewebview.advance.bridge.AdvanceJsBridge
import org.dev.jesen.advancewebview.advance.bridge.AdvanceJsBridgeHelper
import org.dev.jesen.advancewebview.advance.client.AdvanceWebViewClient
import org.dev.jesen.advancewebview.advance.constant.AdvanceConstants
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils
import org.dev.jesen.advancewebview.advance.widget.AdvanceWebView
import org.dev.jesen.advancewebview.databinding.ActivityAdvanceBasicFunctionBinding

/**
 * 基础功能测试（AdvanceBasicFunctionActivity.kt，桥接 + JS 注入）
 */
class AdvanceBasicFunctionActivity : AppCompatActivity(), AdvanceJsBridge.OnJsCallNativeListener {
    private lateinit var binding: ActivityAdvanceBasicFunctionBinding
    private lateinit var mWebView: AdvanceWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvanceBasicFunctionBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // 初始化WebView
        initAdvanceWebView()
        
        // 加载本地H5页面
        loadLocalHtml()
        
        binding.btnInjectBusiness.setOnClickListener { 
            injectBusinessJs()
        }
        binding.btnGetDeviceInfo.setOnClickListener { 
            //调用Js方法获取设备信息
            mWebView.nativeCallJsManager.sendDeviceInfo(getDeviceInfo())
        }
    }

    private fun initAdvanceWebView() {
        mWebView = binding.webView
        // 初始化 JS 桥接（设置回调监听）
        mWebView.initJsBridge(this)
        // 设置 WebViewClient 监听（页面加载时注入全局 JS）
        mWebView.webViewClient = object : AdvanceWebViewClient(){
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                AdvanceLogUtils.d("BasicFunctionActivity", "onPageStarted 开始注入全局工具类JS")
                // 静态注入：全局工具类 JS
                mWebView.injectGlobalToolJs()
            }
        }
        AdvanceLogUtils.d("BasicFunctionActivity", "AdvanceEnterpriseWebView 初始化完成")
    }

    /**
     * 加载本地 H5 页面（基础功能测试）
     */
    private fun loadLocalHtml() {
        mWebView.loadAdvancePage(AdvanceConstants.LOCAL_HTML_BASIC)
        AdvanceLogUtils.d("BasicFunctionActivity", "本地 H5 页面加载中：${AdvanceConstants.LOCAL_HTML_BASIC}")
    }

    /**
     * 动态注入：业务逻辑 JS
     */
    private fun injectBusinessJs() {
        val businessData = AdvanceJsBridgeHelper.toJson(
            mapOf(
                "orderId" to "AdvanceO20240501001",
                "orderAmount" to "199.00",
                "orderStatus" to "已支付",
                "payTime" to "2024-05-01 10:00:00"
            )
        )
        mWebView.injectBusinessJs(businessData)
    }

    private fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "deviceId" to "AdvanceDevice10001",
            "deviceModel" to "Android Phone",
            "systemVersion" to "Android ${android.os.Build.VERSION.RELEASE}",
            "appVersion" to "1.0.0"
        )
    }


    // ---------------------- AdvanceJsBridge 回调实现 ----------------------
    override fun onShowToast(message: String) {
        AdvanceLogUtils.d("BasicFunctionActivity", "JS 调用原生显示 Toast：$message")
        // 显示 Toast（企业级可使用自定义 Toast）
        Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onGetDeviceInfo() {
        AdvanceLogUtils.d("BasicFunctionActivity", "JS 调用原生获取设备信息")
        // 传递设备信息给 JS
        mWebView.nativeCallJsManager.sendDeviceInfo(getDeviceInfo())
    }

    override fun onClearCache() {
        AdvanceLogUtils.d("BasicFunctionActivity", "JS 调用原生清理缓存")
        // 清理所有缓存
        mWebView.clearAllAdvanceCache()
        // 通知 JS 缓存清理完成
        mWebView.nativeCallJsManager.notifyCacheState("缓存清理完成")
    }

    override fun onUnknownMethod(methodName: String, params: String) {
        AdvanceLogUtils.w("BasicFunctionActivity", "未知 JS 方法：$methodName, 参数：$params")
        Toast.makeText(this, "未知方法：$methodName", android.widget.Toast.LENGTH_SHORT).show()
    }

    // ---------------------- 生命周期管理（性能优化，避免内存泄漏）----------------------
    override fun onResume() {
        super.onResume()
        mWebView.lifecycleHelper.onResume()
    }

    override fun onPause() {
        super.onPause()
        mWebView.lifecycleHelper.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 销毁 WebView，释放所有资源
        mWebView.destroyAdvanceWebView()
    }
}