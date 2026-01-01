package org.dev.jesen.androidwebviewjs.core.constants

/**
 * WebView 全局常量
 * 常量统一管理，便于修改和扩展
 */
object WebConstants {
    // JS通信桥名称（原生-JS互调标识）
    const val JS_BRIDGE_NAME = "AndroidJsBridge"

    // JS调用原生方法名
    const val JS_METHOD_CALL_NATIVE = "callAndroid"
    const val JS_METHOD_GET_USER_INFO = "getUserInfo"
    const val JS_METHOD_OPEN_NATIVE_PAGE = "openNativePage"

    // 原生调用JS方法名
    const val NATIVE_METHOD_CALL_JS = "callJs"
    const val NATIVE_METHOD_SHOW_TOAST = "showToast"
    const val NATIVE_METHOD_UPDATE_UI = "updateUi"

    // 本地H5路径
    const val LOCAL_HTML_STAGE1 = "file:///android_asset/stage1/index.html"

    // WebView缓存目录名称
    const val WEBVIEW_CACHE_DIR = "webview_cache"
    const val WEBVIEW_COOKIE_DIR = "webview_cookie"

    // 缓存大小限制50M
    const val WEBVIEW_MAX_CACHE_SIZE = 50 * 1024 * 1024L

    // 页面加载超时时间
    const val WEBVIEW_LOAD_TIMEOUT = 10 * 1000L
}