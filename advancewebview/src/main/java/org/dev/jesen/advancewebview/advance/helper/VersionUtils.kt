package org.dev.jesen.advancewebview.advance.helper

import org.dev.jesen.advancewebview.advance.constant.AdvanceConstants

/**
 * 版本适配工具类（统一处理不同Android版本差异）
 */
object VersionUtils {
    // Android 4.4+
    fun isKitKatOrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_KITKAT
    // Android 5.0+（minSdk 21，基础适配）
    fun isLollipopOrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_LOLLIPOP

    // Android 6.0+（动态权限）
    fun isMarshmallowOrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_MARSHMALLOW

    // Android 7.0+（FileProvider，禁止文件URI）
    fun isNougatOrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_N

    // Android 8.0+（WebView安全浏览，权限变更）
    fun isOreoOrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_O

    fun isOMR1OrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_O_MR1

    // Android 9.0+（禁止明文流量，HTTPS优先）
    fun isPieOrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_P

    // Android 10.0+（分区存储，禁止外部存储写入）
    fun isQOrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_Q

    // Android 11.0+（包可见性，存储权限变更）
    fun isROrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_R

    // Android 12.0+（WebView安全更新，弹窗权限）
    fun isSOrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_S

    // Android 14.0+（targetSdk 36 适配，WebView调试限制）
    fun isUOrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_U

    // Android 16.0+（targetSdk 36，最新特性适配）
    fun isVOrHigher(): Boolean = AdvanceConstants.SDK_INT >= AdvanceConstants.SDK_V

}