package org.dev.jesen.advancewebview.advance.helper

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtils {
    // 所需权限列表
    private val STORAGE_PERMISSIONS = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val NETWORK_PERMISSIONS = arrayOf(
        android.Manifest.permission.ACCESS_NETWORK_STATE,
        android.Manifest.permission.INTERNET
    )

    /**
     * 检查是否拥有存储权限（Android 10+ 无需 WRITE_EXTERNAL_STORAGE）
     */
    fun hasStoragePermission(context: Context): Boolean {
        if (VersionUtils.isQOrHigher()) {
            return true // Android 10+ 分区存储，无需传统存储权限
        }
        return STORAGE_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 检查是否拥有网络权限
     */
    fun hasNetworkPermission(context: Context): Boolean {
        return NETWORK_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 获取需要申请的存储权限
     */
    fun getNeedRequestStoragePermissions(context: Context): Array<String> {
        if (VersionUtils.isQOrHigher()) {
            return emptyArray()
        }
        return STORAGE_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }
}