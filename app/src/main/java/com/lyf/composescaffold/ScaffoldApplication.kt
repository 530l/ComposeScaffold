package com.lyf.composescaffold

import android.app.Application
import com.lyf.composescaffold.core.data.storage.StorageInitializer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ScaffoldApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // MMKV 必须先于一切 KV 读写初始化，早于任何 Hilt 惰性解析。
        StorageInitializer.init(this)
    }
}
