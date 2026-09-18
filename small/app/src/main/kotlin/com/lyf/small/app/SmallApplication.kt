package com.lyf.small.app

import android.app.Application
import android.os.StrictMode
import com.lyf.small.BuildConfig
import com.lyf.small.data.auth.AppLoginExpiredMapper
import com.skydoves.sandwich.SandwichInitializer
import com.skydoves.sandwich.retrofit.exceptions.RetrofitExceptionClassifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SmallApplication : Application() {

    @Inject
    internal lateinit var appLoginExpiredMapper: AppLoginExpiredMapper

    override fun onCreate() {
        super.onCreate()
        // Sandwich 异常分类器启动时注册一次、之后只读，让 Failure.Exception 能区分超时/断网/解析失败。
        SandwichInitializer.sandwichExceptionClassifiers += RetrofitExceptionClassifier
        // 服务端业务码 -1001（登录失效）统一交全局失败 mapper 处理；注册先于任何请求发出。
        SandwichInitializer.sandwichFailureMappers += appLoginExpiredMapper
        if (BuildConfig.DEBUG) {
            // 开发期尽早暴露主线程磁盘/网络访问与资源泄漏，release 不承担检测开销。
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
        }
    }
}
