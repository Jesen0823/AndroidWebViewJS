package org.dev.jesen.advancewebview.ui

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.dev.jesen.advancewebview.BuildConfig
import org.dev.jesen.advancewebview.R
import org.dev.jesen.advancewebview.advance.bridge.AdvanceJsBridge
import org.dev.jesen.advancewebview.advance.bridge.AdvanceJsBridgeHelper
import org.dev.jesen.advancewebview.advance.client.AdvanceWebViewClient
import org.dev.jesen.advancewebview.advance.config.AdvanceWebSecurityConfig
import org.dev.jesen.advancewebview.advance.constant.AdvanceConstants
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils
import org.dev.jesen.advancewebview.advance.helper.VersionUtils
import org.dev.jesen.advancewebview.advance.widget.AdvanceWebView
import org.dev.jesen.advancewebview.databinding.ActivityAdvanceSecurityDefenseBinding

/**
 * 安全防御测试 Activity（URL 校验/XSS 过滤/恶意脚本拦截，独立无依赖）
 */
class AdvanceSecurityDefenseActivity : AppCompatActivity() , AdvanceJsBridge.OnJsCallNativeListener{
    private lateinit var binding: ActivityAdvanceSecurityDefenseBinding
    private lateinit var mWebView: AdvanceWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvanceSecurityDefenseBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. 初始化 AdvanceEnterpriseWebView（安全配置强化）
        initAdvanceWebView()

        // 2. 加载安全测试专属 H5 页面
        loadSecurityHtml()

        initView()
    }

    private fun initAdvanceWebView() {
        mWebView = binding.webView
        mWebView.initJsBridge(this)

        // 页面加载完成后，注入安全测试专属 JS
        mWebView.webViewClient = object : AdvanceWebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectSecurityTestBusinessJs()
            }
        }
        // 强化安全配置：禁用 JS 自动开窗、禁止第三方 Cookie
        mWebView.settings.javaScriptCanOpenWindowsAutomatically = false
        CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView,false)
        AdvanceLogUtils.d("SecurityDefenseActivity", "AdvanceEnterpriseWebView 初始化完成（安全防御强化）")
    }

    /**
     * 注入安全测试专属业务 JS（传递安全配置信息）
     */
    private fun injectSecurityTestBusinessJs() {
        val safeBrowsingEnabled =  if(VersionUtils.isOreoOrHigher()) mWebView.settings.safeBrowsingEnabled else false
        val securityConfigData = AdvanceJsBridgeHelper.toJson(
            mapOf(
                "safeDomainWhitelist" to AdvanceConstants.SAFE_DOMAIN_WHITELIST.joinToString(","),
                "xssFilterEnabled" to "true",
                "fileAccessEnabled" to "false",
                "mixedContentEnabled" to "false",
                "safeBrowsingEnabled" to safeBrowsingEnabled.toString()
            )
        )
        mWebView.injectBusinessJs(securityConfigData)
    }

    /**
     * 加载安全测试专属 H5 页面
     */
    private fun loadSecurityHtml() {
        val securityHtmlUrl = AdvanceConstants.LOCAL_HTML_SECURITY
        mWebView.loadAdvancePage(securityHtmlUrl)
        AdvanceLogUtils.d("SecurityDefenseActivity", "安全测试 H5 页面加载中：$securityHtmlUrl")
    }

    private fun initView() {
        // 3. 绑定点击事件（场景化测试安全功能）
        binding.btnCheckUrlSafety.setOnClickListener {
            val testUrl = binding.etTestUrl.text.toString().trim()
            val isSafe = AdvanceWebSecurityConfig.checkUrlSafety(testUrl)
            val tip = if (isSafe) "URL 安全（信任域名/协议）" else "URL 危险（非法域名/协议/路径）"
            Toast.makeText(this, tip, Toast.LENGTH_SHORT).show()
            // 通知 JS URL 校验结果
            mWebView.nativeCallJsManager.notifyCacheState("URL 校验结果：$tip（原生主动触发）")
        }

        binding.btnFilterXssContent.setOnClickListener {
            val testContent = binding.etTestXss.text.toString().trim()
            val filteredContent = AdvanceWebSecurityConfig.filterXssContent(testContent)
            Toast.makeText(this, "XSS 内容已过滤，可查看 H5 结果区域", Toast.LENGTH_SHORT).show()
            // 传递过滤后的内容给 JS
            mWebView.nativeCallJsManager.sendDeviceInfo(
                mapOf(
                    "originalContent" to testContent,
                    "filteredContent" to filteredContent
                )
            )
        }
    }

    //------------------------------------OnJsCallNativeListener---------------------------------
    override fun onShowToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        AdvanceLogUtils.d("SecurityDefenseActivity", "JS 调用原生显示 Toast：$message")
    }

    override fun onGetDeviceInfo() {
        // 传递安全相关设备信息
        val securityDeviceInfo = mapOf(
            "appId" to packageName,
            "debugMode" to BuildConfig.ENABLE_ADVANCE_DEBUG.toString(),
            "targetSdkVersion" to AdvanceConstants.SDK_V.toString()
        )
        mWebView.nativeCallJsManager.sendDeviceInfo(securityDeviceInfo)
    }

    override fun onClearCache() {
        // 安全场景下清理缓存，同时清理敏感信息
        mWebView.clearAllAdvanceCache()
        mWebView.nativeCallJsManager.notifyCacheState("缓存+敏感信息清理完成（JS 触发）")
        AdvanceLogUtils.d("SecurityDefenseActivity", "JS 调用原生清理缓存+敏感信息完成")
    }

    override fun onUnknownMethod(methodName: String, params: String) {
        AdvanceLogUtils.w("SecurityDefenseActivity", "未知 JS 方法（可能为恶意调用）：$methodName, 参数：$params")
        Toast.makeText(this, "安全预警：未知方法调用 $methodName", Toast.LENGTH_SHORT).show()
    }

    // ---------------------- 生命周期管理（安全资源释放）----------------------
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