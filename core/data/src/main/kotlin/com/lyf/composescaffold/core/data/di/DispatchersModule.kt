package com.lyf.composescaffold.core.data.di

import com.lyf.composescaffold.core.data.coroutine.DefaultDispatcher
import com.lyf.composescaffold.core.data.coroutine.IoDispatcher
import com.lyf.composescaffold.core.data.coroutine.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 官方标准协程调度器注入模块。
 * 消除业务与数据层对硬编码 Dispatchers 的依赖，便于在测试环境使用 TestDispatcher 进行确定性测试。
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
