package org.dev.jesen.advancewebview.advance.helper

import android.util.Log
import org.dev.jesen.advancewebview.BuildConfig

/**
 * 日志工具类（发布版关闭日志）
 */
object AdvanceLogUtils {
    private const val TAG_PREFIX = "AWV_"

    fun d(tag: String, msg: String) {
        if (BuildConfig.ENABLE_ADVANCE_DEBUG) {
            Log.d(TAG_PREFIX + tag, "\uD83D\uDE8B====$msg")
        }
    }

    fun i(tag: String, msg: String) {
        if (BuildConfig.ENABLE_ADVANCE_DEBUG) {
            Log.i(TAG_PREFIX + tag, "\uD83D\uDEB5====$msg")
        }
    }

    fun w(tag: String, msg: String) {
        if (BuildConfig.ENABLE_ADVANCE_DEBUG) {
            Log.w(TAG_PREFIX + tag, "\uD83D\uDEA6====$msg")
        }
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        if (BuildConfig.ENABLE_ADVANCE_DEBUG) {
            if (throwable != null) {
                Log.e(TAG_PREFIX + tag, "\uD83D\uDEAB====$msg", throwable)
            } else {
                Log.e(TAG_PREFIX + tag, "\uD83D\uDEAB====$msg")
            }
        }
    }
}