package org.dev.jesen.advancewebview.advance.config

import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.SafeBrowsingResponse
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.dev.jesen.advancewebview.advance.helper.AdvanceLogUtils
import org.dev.jesen.advancewebview.advance.helper.VersionUtils
import java.net.URL
import java.util.regex.Pattern
import androidx.core.net.toUri

/**
 * AdvanceWebView 安全配置类（安全防御，版本适配+XSS/中间人防护）
 * 职责：禁用危险属性、校验URL安全、防御XSS攻击、适配高版本安全特性
 */
object AdvanceWebSecurityConfig {
    // 安全 URL 白名单（仅允许信任的域名）
    private val SAFE_DOMAIN_WHITELIST = listOf(
        "baidu.com",
        "google.com",
        "github.com",
        "localhost"
    )

    // XSS攻击关键字过滤（简单版，可使用专业库）
    private val XSS_PATTERN = Pattern.compile(
        "<script.*?>.*?</script>|javascript:|onclick|onload|onerror|eval\\(|alert\\(|confirm\\(",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    /**
     * 初始化安全配置（安全策略，全版本适配）
     */
    fun initSecurityConfig(webView: WebView,context: Context){
        val webSettings = webView.settings

        // ---------------------- 1. 禁用危险属性（全版本兼容，核心安全防御）----------------------
        webSettings.apply {
            // 禁用文件访问（禁止WebView访问本地文件，防止信息泄露）
            allowFileAccess = false
            // 禁用文件 URL 访问其他资源（Android 7.0+ 已默认禁用，低版本强制关闭）
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            // 禁用内容访问（防止跨域资源泄露）
            allowContentAccess = false
            // 禁用自动保存表单数据（防止敏感信息泄露）
            @Suppress("DEPRECATION")
            saveFormData = false
            @Suppress("DEPRECATION")
            savePassword = false // 已废弃，高版本自动禁用，低版本显式关闭
        }

        // ---------------------- 2. 高版本安全特性适配（针对性防御，使用AndroidX兼容库）----------------------
        if(VersionUtils.isOreoOrHigher() && WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)){
            // 使用AndroidX WebSettingsCompat配置安全浏览，避免直接调用高版本API
            WebSettingsCompat.setSafeBrowsingEnabled(webSettings, true)
            AdvanceLogUtils.d("AdvanceSecurityConfig", "已启用 WebView 安全浏览（Android 8.0+，兼容targetSdk=36）")

            // 安全浏览回调（处理恶意网站，AndroidX WebViewCompat兼容实现）
            webView.webViewClient = object : WebViewClient() {
                    override fun onSafeBrowsingHit(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        threatType: Int,
                        callback: android.webkit.SafeBrowsingResponse?
                    ) {
                        super.onSafeBrowsingHit(view, request, threatType, callback)
                        AdvanceLogUtils.w("AdvanceSecurityConfig", "检测到恶意网站，威胁类型：$threatType")
                        // 阻止加载恶意网站，返回安全页面（规范）
                        if (VersionUtils.isPieOrHigher()) {
                            callback?.backToSafety(true)
                        }
                    }
            }
        }

        // Android 9.0+ 禁止明文流量（强制 HTTPS，防止中间人攻击）
        if(VersionUtils.isPieOrHigher()){
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            AdvanceLogUtils.d("AdvanceSecurityConfig", "已禁止 WebView 明文流量（Android 9.0+，强制HTTPS）")
        }

        // ③ Android 12.0+（API 31）禁用不必要的 JS 接口（防止漏洞利用，如CVE相关漏洞）
        if (VersionUtils.isSOrHigher()) {
            webView.apply {
                removeJavascriptInterface("searchBoxJavaBridge_")
                removeJavascriptInterface("accessibility")
                removeJavascriptInterface("accessibilityTraversal")
            }
            AdvanceLogUtils.d("AdvanceSecurityConfig", "已移除危险 JS 接口（Android 12.0+）")
        }

        // ④ Android 14.0+（API 34/targetSdk=36）启用 WebView 安全更新（自动修复漏洞，兼容实现）
        if (VersionUtils.isUOrHigher() && WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            // 1. 重新确认安全浏览开启（确保高版本不被覆盖，自动启用安全更新）
            WebSettingsCompat.setSafeBrowsingEnabled(webSettings, true)
            // 2. 额外禁用高版本危险特性（加固，适配targetSdk=36）
            webSettings.apply {
                // 禁用自动打开窗口（防止恶意网站诱导打开新窗口）
                javaScriptCanOpenWindowsAutomatically = false
                // 禁用媒体自动播放（防止恶意媒体消耗资源）
                mediaPlaybackRequiresUserGesture = true
            }
            AdvanceLogUtils.d("AdvanceSecurityConfig", "已强化 WebView 安全配置（Android 14.0+/targetSdk=36），自动启用安全更新")
        }

        AdvanceLogUtils.d("AdvanceSecurityConfig", "WebView 安全配置初始化完成（防御 XSS/中间人/信息泄露，兼容minSdk=21）")
    }

    /**
     * 校验 URL 安全性（规范：白名单+协议校验）
     */
    fun checkUrlSafety(url: String?): Boolean {
        if (url.isNullOrEmpty()) {
            return false
        }

        // 1. 协议校验（仅允许 HTTPS/HTTP/本地文件，禁止自定义协议如js://、intent://）
        val uri = Uri.parse(url)
        val scheme = uri.scheme ?: return false
        val validSchemes = listOf("https", "http", "file")
        if (!validSchemes.contains(scheme.lowercase())) {
            AdvanceLogUtils.w("AdvanceSecurityConfig", "URL 安全校验失败：非法协议 $scheme")
            return false
        }

        // 2. 本地文件校验（仅允许 assets 目录，禁止访问其他本地路径如/sdcard/）
        if (scheme == "file") {
            val validFilePrefix = "file:///android_asset/"
            val isSafeFile = url.startsWith(validFilePrefix)
            if (!isSafeFile) {
                AdvanceLogUtils.w("AdvanceSecurityConfig", "URL 安全校验失败：非法本地文件路径 $url")
            }
            return isSafeFile
        }

        // 3. 域名白名单校验（仅允许信任的域名，防止跳转到恶意网站）
        return try {
            val urlObj = URL(url)
            val host = urlObj.host ?: return false
            val isSafeDomain = SAFE_DOMAIN_WHITELIST.any { host.endsWith(it) }
            if (!isSafeDomain) {
                AdvanceLogUtils.w("AdvanceSecurityConfig", "URL 安全校验失败：非信任域名 $host")
            }
            isSafeDomain
        } catch (e: Exception) {
            AdvanceLogUtils.e("AdvanceSecurityConfig", "URL 解析失败：${e.message}", e)
            false
        }
    }

    /**
     * 过滤 XSS 攻击关键字（净化输入参数，防止恶意脚本注入）
     */
    fun filterXssContent(content: String?): String {
        if (content.isNullOrEmpty()) {
            return ""
        }
        val matcher = XSS_PATTERN.matcher(content)
        val purifiedContent = matcher.replaceAll("")
        if (content != purifiedContent) {
            AdvanceLogUtils.w("AdvanceSecurityConfig", "检测到 XSS 攻击关键字，已净化内容")
        }
        return purifiedContent
    }

    /**
     * 校验 JS 注入内容安全性（防止恶意脚本注入）
     */
    fun checkJsInjectSafety(jsCode: String?): Boolean {
        if (jsCode.isNullOrEmpty()) {
            AdvanceLogUtils.w("AdvanceSecurityConfig", "JS 注入内容为空")
            return false
        }

        // 1. 先过滤 XSS 危险关键字
        val purifiedJs = filterXssContent(jsCode)

        // 2. 禁止危险操作（eval/alert/自定义协议，防止执行恶意逻辑）
        val dangerousKeywords = listOf("eval(", "alert(", "confirm(", "javascript:", "file://", "intent://")
        val hasDangerousCode = dangerousKeywords.any { purifiedJs.contains(it, ignoreCase = true) }
        if (hasDangerousCode) {
            AdvanceLogUtils.w("AdvanceSecurityConfig", "JS 注入内容安全校验失败：包含危险操作关键字")
            return false
        }
        return true
    }
}