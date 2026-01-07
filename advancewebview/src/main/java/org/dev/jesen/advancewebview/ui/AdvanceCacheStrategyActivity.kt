package org.dev.jesen.advancewebview.ui

import android.os.Bundle
import android.webkit.WebSettings
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
import org.dev.jesen.advancewebview.advance.helper.AdvanceThreadHelper
import org.dev.jesen.advancewebview.advance.helper.FileUtils
import org.dev.jesen.advancewebview.advance.helper.NetWorkUtils
import org.dev.jesen.advancewebview.advance.widget.AdvanceWebView
import org.dev.jesen.advancewebview.databinding.ActivityAdvanceCacheStrategyBinding

/**
 * 缓存策略测试（AdvanceCacheStrategyActivity.kt）
 */
class AdvanceCacheStrategyActivity : AppCompatActivity(), AdvanceJsBridge.OnJsCallNativeListener {
    private lateinit var binding: ActivityAdvanceCacheStrategyBinding
    private lateinit var mWebView: AdvanceWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvanceCacheStrategyBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. 初始化 AdvanceEnterpriseWebView（桥接+缓存配置）
        initAdvanceWebView()

        // 2. 加载缓存测试专属 H5 页面
        loadCacheHtml()

        initView()
    }

    private fun initAdvanceWebView() {
        mWebView = binding.webView
        mWebView.initJsBridge(this)

        // 页面加载完成后，注入缓存测试专属 JS
        mWebView.webViewClient = object : AdvanceWebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                AdvanceLogUtils.d("CacheStrategyActivity", "onPageFinished 准备注入业务测试")
                // 1. 注入全局工具类 JS
                mWebView.injectGlobalToolJs()
                // 2.注入缓存测试业务 JS
                injectCacheTestBusinessJs()
                // 额外推送网络状态（补充缓存配置的辅助信息）
                mWebView.nativeCallJsManager.updateUi(
                    mapOf("networkState" to if (NetWorkUtils.isNetworkAvailable(mWebView.context)) "已联网" else "未联网")
                )
            }


        }
        AdvanceLogUtils.d("CacheStrategyActivity", "AdvanceEnterpriseWebView 初始化完成（缓存策略强化）")
    }

    private fun loadCacheHtml() {
        val cacheHtmlUrl = AdvanceConstants.LOCAL_HTML_CACHE
        mWebView.loadAdvancePage(cacheHtmlUrl)
        AdvanceLogUtils.d("CacheStrategyActivity", "缓存测试 H5 页面加载中：$cacheHtmlUrl")
    }

    /**
     * 注入缓存测试专属业务 JS（传递缓存配置信息）
     */
    private fun injectCacheTestBusinessJs() {
        val cacheConfigData = mapOf(
            "cacheMaxSize" to "100MB",
            "cacheExpireTime" to "7天",
            "cacheDir" to FileUtils.getWebViewCacheDir(this).absolutePath,
            "currentCacheMode" to getCacheModeDesc()
        )
        val cacheConfig = AdvanceJsBridgeHelper.toJson(cacheConfigData)
        mWebView.injectBusinessJs(cacheConfig)

        // 核心优化：注入后主动推送配置给 H5（复用 updateUi 公共方法）
        AdvanceThreadHelper.runOnMainThread {
            mWebView.nativeCallJsManager.updateUi(cacheConfigData)
        }
        AdvanceLogUtils.d("CacheStrategyActivity", "已主动推送缓存配置给 H5")
    }

    private fun initView() {
        // 3. 绑定点击事件（场景化测试缓存功能）
        binding.btnClearCache.setOnClickListener {
            // 原生主动清理缓存
            mWebView.clearAllAdvanceCache()
            // 通知 JS 缓存清理完成
            mWebView.nativeCallJsManager.notifyCacheState("缓存已全部清理（原生主动触发）")
            // 推送更新后的缓存模式（若有变化）
            mWebView.nativeCallJsManager.updateUi(
                mapOf("currentCacheMode" to getCacheModeDesc())
            )
            Toast.makeText(this, "缓存清理完成", Toast.LENGTH_SHORT).show()
        }
        binding.btnTestOfflineCache.setOnClickListener {
            // 切换离线缓存模式（强制加载缓存，无网络时生效）
            mWebView.settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ONLY
            // 重新加载页面验证离线缓存
            mWebView.loadAdvancePage(AdvanceConstants.LOCAL_HTML_CACHE)
            // 复用 notifyCacheState 和 updateUi 公共方法
            mWebView.nativeCallJsManager.notifyCacheState("已切换为离线缓存模式，重新加载页面")
            mWebView.nativeCallJsManager.updateUi(
                mapOf("currentCacheMode" to "仅加载缓存，不访问网络")
            )
            Toast.makeText(this, "已切换为离线缓存模式，重新加载页面", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 复用缓存模式描述转换方法，避免重复
     */
    private fun getCacheModeDesc(): String {
        return when (mWebView.settings.cacheMode) {
            WebSettings.LOAD_DEFAULT -> "有网加载新数据，无网加载缓存"
            WebSettings.LOAD_CACHE_ONLY -> "仅加载缓存，不访问网络"
            WebSettings.LOAD_CACHE_ELSE_NETWORK -> "优先加载缓存，无缓存再访问网络"
            WebSettings.LOAD_NO_CACHE -> "不加载缓存，仅访问网络"
            else -> "未知缓存模式"
        }
    }

    // -------------------------------------OnJsCallNativeListener----------------------------
    override fun onShowToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        AdvanceLogUtils.d("CacheStrategyActivity", "JS 调用原生显示 Toast：$message")
    }

    override fun onGetDeviceInfo() {
        // 传递设备信息，辅助缓存策略判断
        val deviceInfo = mapOf(
            "networkState" to if (NetWorkUtils.isNetworkAvailable(this.applicationContext)) "已联网" else "未联网",
            "deviceModel" to android.os.Build.MODEL,
            "systemVersion" to android.os.Build.VERSION.RELEASE
        )
        mWebView.nativeCallJsManager.sendDeviceInfo(deviceInfo)
    }

    override fun onClearCache() {
        // JS 触发清理缓存
        mWebView.clearAllAdvanceCache()
        mWebView.nativeCallJsManager.notifyCacheState("缓存清理完成（JS 触发）")
        AdvanceLogUtils.d("CacheStrategyActivity", "JS 调用原生清理缓存完成")
    }

    override fun onUnknownMethod(methodName: String, params: String) {
        AdvanceLogUtils.w("CacheStrategyActivity", "未知 JS 方法：$methodName, 参数：$params")
        Toast.makeText(this, "缓存场景未知方法：$methodName", Toast.LENGTH_SHORT).show()
    }

    // ---------------------- 生命周期管理（缓存资源释放）----------------------
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
        mWebView.destroyAdvanceWebView()
    }
}