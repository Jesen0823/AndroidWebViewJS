package org.dev.jesen.advancewebview.ui

import android.os.Build
import android.os.Bundle
import android.view.View
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
import org.dev.jesen.advancewebview.advance.client.AdvanceWebChromeClient
import org.dev.jesen.advancewebview.advance.client.AdvanceWebViewClient
import org.dev.jesen.advancewebview.advance.constant.AdvanceConstants
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils
import org.dev.jesen.advancewebview.advance.helper.AdvanceThreadHelper
import org.dev.jesen.advancewebview.advance.widget.AdvanceWebView
import org.dev.jesen.advancewebview.databinding.ActivityAdvancePerformanceOptBinding

/**
 * 性能优化测试（AdvancePerformanceOptActivity.kt）
 */
class AdvancePerformanceOptActivity : AppCompatActivity(), AdvanceJsBridge.OnJsCallNativeListener {
    private lateinit var binding: ActivityAdvancePerformanceOptBinding
    private lateinit var mWebView: AdvanceWebView
    private var isImageLoadingEnabled = true // 图片加载状态标记

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvancePerformanceOptBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. 初始化 AdvanceEnterpriseWebView（性能配置强化）
        initAdvanceWebView()

        // 2. 加载性能测试专属 H5 页面
        loadPerformanceHtml()

        initView()
    }

    /**
     * 加载性能测试专属 H5 页面（对应 assets/advance/performance/index.html）
     */
    private fun loadPerformanceHtml() {
        val performanceHtmlUrl = AdvanceConstants.LOCAL_HTML_PERFORMANCE
        mWebView.loadAdvancePage(performanceHtmlUrl)
        AdvanceLogUtils.d("PerformanceOptActivity", "性能测试 H5 页面加载中：$performanceHtmlUrl")
    }

    private fun initView() {
        // 3. 绑定点击事件（场景化测试性能功能）
        binding.btnToggleImage.setOnClickListener {
            isImageLoadingEnabled = !isImageLoadingEnabled
            mWebView.toggleImageLoading(isImageLoadingEnabled)
            val btnText = if (isImageLoadingEnabled) "禁用图片加载（提升首屏速度）" else "启用图片加载（显示完整内容）"
            binding.btnToggleImage.text = btnText
            val tip = if (isImageLoadingEnabled) "图片加载已启用，重新加载页面可查看效果" else "图片加载已禁用，首屏加载速度提升"
            Toast.makeText(this, tip, Toast.LENGTH_SHORT).show()
            // 通知 JS 图片加载状态变更
            mWebView.nativeCallJsManager.notifyCacheState("图片加载状态：${if (isImageLoadingEnabled) "启用" else "禁用"}")
            // 核心修改：同步通知 H5 更新图片加载状态
            pushPerformanceConfigToH5()
        }
        binding.btnTestHardwareAcceleration.setOnClickListener {
            // 切换渲染模式（硬件/软件），测试性能差异
            val currentLayerType = mWebView.layerType
            val newLayerType = if (currentLayerType == android.view.View.LAYER_TYPE_HARDWARE) {
                View.LAYER_TYPE_SOFTWARE
            } else {
                View.LAYER_TYPE_HARDWARE
            }
            mWebView.setLayerType(newLayerType, null)
            val tip = if (newLayerType == android.view.View.LAYER_TYPE_HARDWARE) "已切换为硬件加速（渲染更快）" else "已切换为软件渲染（兼容更好）"
            Toast.makeText(this, tip, Toast.LENGTH_SHORT).show()
            AdvanceLogUtils.d("PerformanceOptActivity", "渲染模式切换：$tip")
        }
        binding.btnTestCacheMode.setOnClickListener {
            val newCacheMode = if (mWebView.settings.cacheMode == WebSettings.LOAD_DEFAULT) {
                WebSettings.LOAD_CACHE_ONLY
            } else {
                WebSettings.LOAD_DEFAULT
            }
            mWebView.settings.cacheMode = newCacheMode
            // 同步通知 H5 更新缓存模式
            pushPerformanceConfigToH5()
        }
    }

    /**
     * 初始化 AdvanceEnterpriseWebView（性能配置强化）
     */
    private fun initAdvanceWebView() {
        mWebView = binding.webView
        mWebView.initJsBridge(this)
        isImageLoadingEnabled = mWebView.settings.loadsImagesAutomatically

        // 页面加载进度监听（性能展示：更新进度条）
        mWebView.webChromeClient = object : AdvanceWebChromeClient() {
            override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                AdvanceLogUtils.d("PerformanceOptActivity", "页面加载进度：$newProgress%")
                // 核心修改：主线程调用 H5 方法，传递实时进度
                AdvanceThreadHelper.runOnMainThread {
                    view?.evaluateJavascript("javascript:updatePageProgress($newProgress)") {
                        AdvanceLogUtils.d("PerformanceOptActivity", "进度已推送给 H5：$newProgress%")
                    }
                }
                binding.pbPageLoad.progress = newProgress
                if (newProgress == 100) {
                    // 页面加载完成，注入性能测试专属 JS
                    pushPerformanceConfigToH5()
                }
            }
        }
        // 性能优化：设置高渲染优先级、禁用不必要功能
        mWebView.settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
        mWebView.settings.setGeolocationEnabled(false)

        mWebView.webViewClient = object : AdvanceWebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.let {
                    // 1. 注入全局工具类 JS
                    mWebView.injectGlobalToolJs()
                    // 2. 注入业务 JS
                    injectPerformanceTestBusinessJs()
                    // 3. 主动推送性能配置（确保 H5 拿到数据）
                    pushPerformanceConfigToH5()
                }
            }
        }

        AdvanceLogUtils.d("PerformanceOptActivity", "AdvanceEnterpriseWebView 初始化完成（性能优化强化）")
    }

    /**
     * 注入性能测试专属业务 JS（传递性能配置信息）
     */
    private fun injectPerformanceTestBusinessJs() {
        val businessData = AdvanceJsBridgeHelper.toJson(
            mapOf(
                "injectTime" to System.currentTimeMillis().toString()
            )
        )
        mWebView.injectBusinessJs(businessData)
    }

    /**
     * 主动推送性能配置给 H5（初始化+状态变更时调用）
     */
    private fun pushPerformanceConfigToH5() {
        val performanceConfig = mapOf(
            "hardwareAcceleration" to (mWebView.layerType == View.LAYER_TYPE_HARDWARE).toString(),
            "imageLoadingEnabled" to isImageLoadingEnabled.toString(),
            "renderPriority" to "HIGH",
            "pageLoadTimeout" to "${AdvanceConstants.WEBVIEW_LOAD_TIMEOUT / 1000}秒",
            "cacheMode" to getCacheModeDesc()
        )
        // 主线程调用 JS 方法，传递配置数据
        AdvanceThreadHelper.runOnMainThread {
            mWebView.nativeCallJsManager.updateUi(performanceConfig)
            AdvanceLogUtils.d("PerformanceOptActivity", "已推送性能配置给 H5：$performanceConfig")
        }
    }


    /**
     * 转换缓存模式为描述文字
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

    //-------------------------------------------OnJsCallNativeListener------------------------
    override fun onShowToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        AdvanceLogUtils.d("PerformanceOptActivity", "JS 调用原生显示 Toast：$message")
    }

    override fun onGetDeviceInfo() {
        // 传递设备性能相关信息
        val performanceDeviceInfo = mapOf(
            "cpuModel" to Build.CPU_ABI,
            "memoryClass" to "100MB",
            "hardwareAcceleration" to (mWebView.layerType == View.LAYER_TYPE_HARDWARE).toString()
        )
        mWebView.nativeCallJsManager.sendDeviceInfo(performanceDeviceInfo)
    }

    override fun onClearCache() {
        // 清理缓存释放内存，提升性能
        mWebView.clearAllAdvanceCache()
        mWebView.nativeCallJsManager.notifyCacheState("缓存清理完成（释放内存，提升性能）")
        AdvanceLogUtils.d("PerformanceOptActivity", "JS 调用原生清理缓存（性能优化）")
    }

    override fun onUnknownMethod(methodName: String, params: String) {
        AdvanceLogUtils.w("PerformanceOptActivity", "未知 JS 方法：$methodName, 参数：$params")
        Toast.makeText(this, "性能场景未知方法：$methodName", Toast.LENGTH_SHORT).show()
    }

    // ---------------------- 生命周期管理（性能资源最优释放）----------------------
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