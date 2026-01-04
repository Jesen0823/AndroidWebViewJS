package org.dev.jesen.advancewebview.advance.constant

import android.os.Build

/**
 * 全局常量
 * 职责：统一管理常量，避免硬编码
 */
object AdvanceConstants {
    // 1. 通信桥配置（全局唯一，避免冲突）
    const val JS_BRIDGE_NAME = "AdvanceAndroidJsBridge"
    const val JS_METHOD_CALL_NATIVE = "callAdvanceAndroid"
    const val JS_METHOD_SHOW_TOAST = "showAdvanceToast"
    const val JS_METHOD_GET_DEVICE_INFO = "getAdvanceDeviceInfo"
    const val JS_METHOD_CLEAR_CACHE = "clearAdvanceCache"

    // 2. 原生调用 JS 配置
    const val NATIVE_METHOD_CALL_JS = "callAdvanceJs"
    const val NATIVE_METHOD_UPDATE_UI = "updateAdvanceUi"
    const val NATIVE_METHOD_NOTIFY_CACHE = "notifyAdvanceCacheState"

    // 3. JS 注入模块配置
    const val INJECT_MODULE_GLOBAL_TOOL = "AdvanceGlobalTool"
    const val INJECT_MODULE_BUSINESS_LOGIC = "AdvanceBusinessLogic"

    // 4. 缓存配置
    const val WEBVIEW_CACHE_DIR = "advance_webview_cache"
    const val WEBVIEW_COOKIE_DIR = "advance_webview_cookie"
    const val WEBVIEW_APP_CACHE_DIR = "advance_webview_app_cache"
    const val WEBVIEW_MAX_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
    const val WEBVIEW_CACHE_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L // 7天

    // 5. 本地 H5 路径（assets/advance 目录）
    const val LOCAL_HTML_BASIC = "file:///android_asset/advance/basic/index.html"

    // 6. 网络配置（HTTPS 优先，适配 targetSdk 36）
    const val HTTPS_TEST_URL = "https://www.baidu.com"
    const val WEBVIEW_LOAD_TIMEOUT = 15 * 1000L // 15秒超时

    // 7. 版本适配常量（简化版本判断）
    val SDK_INT = Build.VERSION.SDK_INT
    const val SDK_LOLLIPOP = 21 // Android 5.0
    const val SDK_MARSHMALLOW = 23 // Android 6.0
    const val SDK_N = 24 // Android 7.0
    const val SDK_O = 26 // Android 8.0

    const val SDK_O_MR1 = 27
    const val SDK_P = 28 // Android 9.0
    const val SDK_Q = 29 // Android 10.0
    const val SDK_R = 30 // Android 11.0
    const val SDK_S = 31 // Android 12.0
    const val SDK_TIRAMISU = 33 // Android 13.0
    const val SDK_U = 34 // Android 14.0
    const val SDK_V = 36 // Android 16.0（targetSdk 36）
}