package org.dev.jesen.advancewebview.advance.helper

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * 全局线程工具类（解耦主线程切换逻辑）
 * 核心功能：统一处理主线程切换，支持任意类调用，避免内存泄漏
 */
object AdvanceThreadHelper {
    private lateinit var appContext: Context
    // 主线程Handler（全局唯一）
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * 初始化Application Context（在Application onCreate中调用）
     */
    fun init(context: Application) {
        appContext = context
    }

    /**
     * 重载方法：无需传入Context，直接使用Application Context
     */
    fun runOnMainThread(delayMillis: Long = 0, runnable: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
            return
        }
        mainHandler.postDelayed(runnable, delayMillis)
        AdvanceLogUtils.d(
            "AdvanceThreadHelper",
            "已切换主线程执行任务（延迟${delayMillis}ms），当前线程：${Thread.currentThread().name}"
        )
    }

    /**
     * 在主线程执行任务
     * @param context 上下文（用于日志打印，可选）
     * @param runnable 需执行的任务
     * @param delayMillis 延迟执行时间（默认0ms，即时执行）
     */
    fun runOnMainThread(
        context: Context? = null,
        delayMillis: Long = 0,
        runnable: Runnable
    ) {
        runOnMainThread(delayMillis, runnable)
    }

    /**
     * 判断当前是否为主线程
     */
    fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
}