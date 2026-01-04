package org.dev.jesen.advancewebview.advance.helper

import android.content.Context
import org.dev.jesen.advancewebview.advance.constant.AdvanceConstants
import java.io.File

/**
 * 文件操作工具类（适配不同版本存储策略）
 */
object FileUtils {
    /**
     * 获取 WebView 缓存目录（适配 Android 10+ 分区存储）
     */
    fun getWebViewCacheDir(context: Context): File {
        val cacheDir = if (VersionUtils.isQOrHigher()) {
            // Android 10+：使用应用内部缓存目录（无需存储权限，安全）
            File(context.cacheDir, AdvanceConstants.WEBVIEW_CACHE_DIR)
        } else {
            // Android 10-：优先外部存储，无权限则使用内部存储
            val externalCacheDir: File? = context.externalCacheDir
            if (externalCacheDir != null && PermissionUtils.hasStoragePermission(context)) {
                File(externalCacheDir, AdvanceConstants.WEBVIEW_CACHE_DIR)
            } else {
                File(context.cacheDir, AdvanceConstants.WEBVIEW_CACHE_DIR)
            }
        }
        // 创建目录（不存在则创建）
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        AdvanceLogUtils.d("AdvanceFileUtils", "WebView 缓存目录：${cacheDir.absolutePath}")
        return cacheDir
    }

    /**
     * 获取 WebView 应用缓存目录
     */
    fun getWebViewAppCacheDir(context: Context): File {
        val appCacheDir = File(getWebViewCacheDir(context), AdvanceConstants.WEBVIEW_APP_CACHE_DIR)
        if (!appCacheDir.exists()) {
            appCacheDir.mkdirs()
        }
        return appCacheDir
    }

    /**
     * 获取 WebView Cookie 目录
     */
    fun getWebViewCookieDir(context: Context): File {
        val cookieDir = File(getWebViewCacheDir(context), AdvanceConstants.WEBVIEW_COOKIE_DIR)
        if (!cookieDir.exists()) {
            cookieDir.mkdirs()
        }
        return cookieDir
    }

    /**
     * 删除目录及所有子文件
     */
    fun deleteDir(dir: File?): Boolean {
        if (dir == null || !dir.exists()) {
            return false
        }
        if (dir.isDirectory) {
            val children = dir.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteDir(child)
                }
            }
        }
        return dir.delete()
    }

    /**
     * 获取目录大小
     */
    fun getDirSize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) {
            return 0
        }
        var size = 0L
        val children = dir.listFiles()
        if (children != null) {
            for (child in children) {
                size += if (child.isDirectory) {
                    getDirSize(child)
                } else {
                    child.length()
                }
            }
        }
        return size
    }

    /**
     * 清理过期文件（按时间筛选）
     */
    fun clearExpireFiles(dir: File, expireTime: Long): Boolean {
        if (!dir.exists() || !dir.isDirectory) {
            return false
        }
        val currentTime = System.currentTimeMillis()
        val children = dir.listFiles()
        if (children != null) {
            for (child in children) {
                if (child.isDirectory) {
                    clearExpireFiles(child, expireTime)
                } else {
                    if (currentTime - child.lastModified() > expireTime) {
                        child.delete()
                    }
                }
            }
        }
        return true
    }
}