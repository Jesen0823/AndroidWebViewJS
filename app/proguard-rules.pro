# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 保留WebView相关类和注解（避免JS接口混淆）
-keep class android.webkit.JavascriptInterface { *; }
-keep @android.webkit.JavascriptInterface class * { *; }

# 保留JsBridge类及其所有公共方法（核心：避免类和方法被重命名）
-keep class org.dev.jesen.androidwebviewjs.web.bridge.JsBridge {
    public *;
}

# 保留JsBridge的回调接口（避免接口方法被混淆）
-keep interface org.dev.jesen.androidwebviewjs.web.bridge.JsBridge$OnJsCallNativeListener {
    public *;
}

# 保留Gson相关类（避免JSON解析失败，若使用Gson）
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*