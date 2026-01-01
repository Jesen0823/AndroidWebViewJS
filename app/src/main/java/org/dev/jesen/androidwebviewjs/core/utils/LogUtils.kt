package org.dev.jesen.androidwebviewjs.core.utils

import android.util.Log

object LogUtils {
    private const val TAG_PREFIX = "WVSJ_"
    private const val IS_DEBUG = true // 发布版可通过 BuildConfig 控制

    fun d(tag: String, content: String) {
        if (IS_DEBUG) Log.d("$TAG_PREFIX-$tag", "\uD83D\uDCCD===$content")
    }

    fun e(tag: String, content: String, e: Exception? = null) {
        if (IS_DEBUG) {
            if (e != null) {
                Log.e("$TAG_PREFIX-$tag", "\uD83D\uDCE2===$content", e)
            } else {
                Log.e("$TAG_PREFIX-$tag", "\uD83D\uDCE2===$content")
            }
        }
    }

    fun i(tag: String, content: String) {
        if (IS_DEBUG) Log.i("$$TAG_PREFIX-$tag", "\uD83D\uDD0A===$content")
    }

    fun w(tag: String, content: String) {
        if (IS_DEBUG) Log.w("$$TAG_PREFIX-$tag", "\uD83D\uDD14===$content")
    }
}