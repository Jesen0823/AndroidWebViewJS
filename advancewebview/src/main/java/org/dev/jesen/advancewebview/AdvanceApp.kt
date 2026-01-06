package org.dev.jesen.advancewebview

import android.app.Application
import org.dev.jesen.advancewebview.advance.helper.AdvanceThreadHelper

class AdvanceApp: Application() {
    companion object{
        lateinit var INSTANCE: AdvanceApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        // 初始化线程工具类的Application Context
        AdvanceThreadHelper.init(this)
    }
}