package com.lyf.composescaffold.core.data.di

import com.lyf.composescaffold.core.data.repository.DefaultFeedInteractionStore
import com.lyf.composescaffold.core.data.repository.FeedInteractionStore
import com.lyf.composescaffold.core.data.repository.FeedRepository
import com.lyf.composescaffold.core.data.repository.SampleFeedRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 流媒体数据层 Hilt 依赖注入装配模块（FeedDataModule）。
 *
 * 【作用与职责】：
 * 绑定流媒体数据仓库实现类与接口抽象，解耦业务调用方对具体数据源的实现依赖。
 * 将 [SampleFeedRepository] 绑定为 [FeedRepository]，将 [DefaultFeedInteractionStore] 绑定为 [FeedInteractionStore]。
 *
 * 【核心依赖】：
 * - [SampleFeedRepository]：样本流媒体数据仓库。
 * - [DefaultFeedInteractionStore]：互动标记持久化存储。
 *
 * 【提供功能】：
 * - [bindFeedRepository]：绑定 Feed 仓储单例；
 * - [bindFeedInteractionStore]：绑定 Feed 互动持久化存储。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FeedDataModule {
    /** 绑定流媒体数据仓库契约实现 */
    @Binds
    abstract fun bindFeedRepository(repository: SampleFeedRepository): FeedRepository

    /** 绑定互动状态（点赞、收藏）存储契约实现 */
    @Binds
    abstract fun bindFeedInteractionStore(store: DefaultFeedInteractionStore): FeedInteractionStore
}
