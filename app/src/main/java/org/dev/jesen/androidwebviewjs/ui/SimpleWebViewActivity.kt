package org.dev.jesen.androidwebviewjs.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.dev.jesen.androidwebviewjs.R
import org.dev.jesen.androidwebviewjs.core.constants.WebConstants
import org.dev.jesen.androidwebviewjs.core.helpers.WebViewLifecycleHelper
import org.dev.jesen.androidwebviewjs.core.utils.LogUtils
import org.dev.jesen.androidwebviewjs.databinding.ActivitySimpleWebViewBinding
import org.dev.jesen.androidwebviewjs.web.client.CustomWebViewClient
import org.dev.jesen.androidwebviewjs.web.config.WebViewConfig

/**
 * 阶段1：简单 WebView 页面
 * 功能：加载本地 H5，演示基础属性配置
 */
class SimpleWebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySimpleWebViewBinding
    private lateinit var webViewLifecycleHelper: WebViewLifecycleHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySimpleWebViewBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 接收 Intent 传递的参数
        val title = intent.getStringExtra("title") ?: "默认标题"
        val content = intent.getStringExtra("content") ?: "默认内容"

        initView()

        // 4. 加载本地 H5
        loadLocalHtml()
    }

    private fun loadLocalHtml() {
        LogUtils.d("SimpleWebViewActivity", "加载本地 H5：${WebConstants.LOCAL_HTML_STAGE1}")
        binding.webView.loadUrl(WebConstants.LOCAL_HTML_STAGE1)
    }

    /**
     * 加载本地 H5（assets 目录）
     */
    private fun initView() {
        // 1. 初始化 WebView 配置
        WebViewConfig.initBasicConfig(binding.webView)
        // 2. 设置自定义 WebViewClient
        binding.webView.webViewClient = CustomWebViewClient()
        // 3. 初始化 WebView 生命周期管理（避免内存泄漏）
        webViewLifecycleHelper = WebViewLifecycleHelper(binding.webView)
    }

    // ---------------------- WebView 生命周期管理----------------------
    override fun onResume() {
        super.onResume()
        webViewLifecycleHelper.onResume() // 恢复 WebView 运行
    }

    override fun onPause() {
        super.onPause()
        webViewLifecycleHelper.onPause() // 暂停 WebView 运行
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewLifecycleHelper.onDestroy() // 销毁 WebView，释放资源（避免内存泄漏）
        binding.webView.removeAllViews()
        binding.webView.destroy()
    }
}